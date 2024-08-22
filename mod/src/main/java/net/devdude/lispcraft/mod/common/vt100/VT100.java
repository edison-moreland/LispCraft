package net.devdude.lispcraft.mod.common.vt100;

import net.devdude.lispcraft.mod.Mod;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

// Emulate a VT100 console
// Writing to the output stream will update the screen buffer
public class VT100 {
    protected final List<ScreenBufferConsumer> observers;

    private final PipedInputStream input;
    private final PipedOutputStream hostOutput;

    private final PipedOutputStream output;
    private final PipedInputStream hostInput;

    private final VT100ScreenBuffer screen;
    private final ByteBuffer parseBuf = MappedByteBuffer.allocate(255);
    private boolean running = false;

    public VT100(Size size) {
        this.observers = new ArrayList<>();

        this.screen = new VT100ScreenBuffer(size);

        try {
            this.input = new PipedInputStream();
            this.hostOutput = new PipedOutputStream(this.input);

            this.output = new PipedOutputStream();
            this.hostInput = new PipedInputStream(this.output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isInRange(int v, int low, int high) {
        return low <= v && v <= high;
    }

    public InputStream getInputStream() throws IOException {
        return this.hostInput;
    }

    public OutputStream getOutputStream() throws IOException {
        return this.hostOutput;
    }

    public void start() {
        assert !this.running;
        this.running = true;
        Thread.startVirtualThread(() -> {
            try {
                this.run();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

    private void run() throws IOException {

        while (this.running) {
            var c = this.input.read();

            if (ANSI.isControlCharacter(c)) {
                handleAnsiControl(c);
            } else {
                write((char) c);
            }

            this.flush();
        }
    }

    private void handleAnsiControl(int c) throws IOException {
        switch (c) {
            case ANSI.LF:
                newLine();
                break;
            case ANSI.BS:
                backspace();
                break;
            case ANSI.TAB:
                tab();
                break;
            case ANSI.CR:
                screen.setCursor(0, screen.getCursor().y());
                break;
            case ANSI.ESC:
                // C1 escape sequence, next byte should be 0x40 to 0x5F
                handleC1();
                break;
            default:
                Mod.LOGGER.warn("Unknown control character: {}", c);
        }
    }

    private void handleC1() throws IOException {
        var next = this.input.read();
        switch (next) {
            case C1.CSI:
                // CSI control sequence introducer
                handleCSI();
                break;
            case C1.NEL:
                newLine();
                break;
            case C1.CCH:
                backspace();
                break;
            default:
                write((char) next);
                Mod.LOGGER.warn("Unknown control sequence: C1 {}", next);
        }
    }

    private void handleCSI() throws IOException {
        // Read control sequence
        // parameter byte range = 0x30–0x3F
        // intermediate byte range = 0x20–0x2F
        // final byte range = 0x40–0x7E

        var phase = 0; // 0 = looking for parameter, 1 = intermediate
        var i = 0;
        var finalParameterByteI = 0;
        var finalIntermediateByteI = 0;
        var finalByteI = 0;
        this.parseBuf.clear();
        while (this.running) {
            var next = this.input.read();
            this.parseBuf.put((byte) next);

            if (phase == 0 && !isInRange(next, 0x30, 0x3F)) {
                finalParameterByteI = i - 1;
                phase = 1;
            }

            if (phase == 1 && !isInRange(next, 0x20, 0x2F)) {
                finalIntermediateByteI = i - 1;
                phase = 2;
            }

            if (next >= 0x40 && next <= 0x7E) {
                if (phase != 2) {
                    throw new RuntimeException("This shouldn't happen, you messed up");
                }

                finalByteI = i;
                break;
            }

            i++;
        }

        byte[] parameterBytes = null;
        if (finalParameterByteI >= 0) {
            parameterBytes = new byte[finalParameterByteI + 1];
            this.parseBuf.get(0, parameterBytes);
        }

        byte[] intermediateBytes = null;
        if (finalIntermediateByteI != finalParameterByteI) {
            intermediateBytes = new byte[finalIntermediateByteI - finalParameterByteI];
            this.parseBuf.get(finalParameterByteI + 1, intermediateBytes);
        }

        byte finalByte = this.parseBuf.get(finalByteI);

        switch (finalByte) {
            case 109: // SGR
                var sgr = 0;
                if (parameterBytes != null) {
                    sgr = Integer.parseInt(new String(parameterBytes));
                }

                handleSGR(sgr);
                break;
            default:
                var seq = new byte[parseBuf.position()];
                parseBuf.get(0, seq);

                var parsed = "";
                if (parameterBytes != null) {
                    parsed += new String(parameterBytes);
                }
                if (intermediateBytes != null) {
                    parsed += new String(intermediateBytes);
                }
                parsed += new String(new byte[]{finalByte});

                Mod.LOGGER.warn("Unknown control sequence: CSI {} {}", parsed, seq);
        }
    }

    private void handleSGR(int sgr) {
        Mod.LOGGER.info("CSI SGR {}", sgr);
        switch (sgr) {
            case 0:
                // reset
                break;
            case 1:
                // bold
                break;
            case 7:
                // negative
                break;
        }
    }

    public void stop() {
        assert this.running;
        this.running = false;
    }

    // TODO: onChange logic is a hold over and probably not the best solution
    public void onChange(ScreenBufferConsumer observer) {
        this.observers.add(observer);
    }

    public VT100ScreenBuffer getScreen() {
        return this.screen.clone();
    }

    // accepts keyboard input after it's been translated to ANSI
    public void writeKeyboardInput(byte[] input) throws IOException {
        this.output.write(input);
        this.output.flush();

        // Do local echo
        this.hostOutput.write(input);
        this.hostOutput.flush();
    }

    public void flush() {
        var screen = getScreen();
        for (var observer : this.observers) {
            observer.screenBufferUpdates(screen);
        }
    }

    private void write(char c) {
        screen.setCharacter(c, Style.DEFAULT);

        if (screen.getCursor().x() + 1 >= screen.getSize().width()) {
            newLine();
        } else {
            screen.setCursorRelative(1, 0);
        }
    }

    private void tab() {
        // Move the cursor to the next multiple of 8
        var x = (8 - screen.getCursor().x() % 8);
        for (int i = 0; i < x; i++) {
            write(' ');
        }
    }

    private void newLine() {
        if (screen.getCursor().y() + 1 >= screen.getSize().height()) {
            this.screen.scroll(1);
            screen.setCursor(0, screen.getSize().height() - 1);
        } else {
            screen.setCursor(0, screen.getCursor().y() + 1);
        }
    }

    private void backspace() {
        if (screen.getCursor().x() == 0) {
            if (screen.getCursor().y() == 0) {
                return; // At the top of the screen, nothing to do
            }

            // The cursor is at the start of a line, we need to put it behind any text on the line above
            setCursorBehindText(screen.getCursor().y() - 1);
        } else {
            // The cursor is somewhere in the middle of a line
            screen.setCursorRelative(-1, 0);
        }

        screen.setCharacter(' ', Style.DEFAULT);
    }

    private void setCursorBehindText(int y) {
        // Going backwards, find the first column without text and put the cursor to it's right
        // - If there is text in the last column, put the cursor on the last column
        // - If the line is empty, put the cursor on the first column
        for (var x = screen.getSize().width() - 1; x >= 0; x--) {
            if (screen.getCodepointAt(x, y) != ' ') {
                // This is the last column with text in it
                screen.setCursor(Integer.min(x + 1, screen.getSize().width() - 1), y);
                return;
            }
        }

        // This line is empty
        screen.setCursor(0, y);
    }

    @FunctionalInterface
    public interface ScreenBufferConsumer {
        void screenBufferUpdates(VT100ScreenBuffer buffer);
    }


}
