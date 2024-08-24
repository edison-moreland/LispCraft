package net.devdude.lispcraft.mod.common.vt100;

import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.common.vt100.ansi.ANSI;
import net.devdude.lispcraft.mod.common.vt100.ansi.C1;
import net.devdude.lispcraft.mod.common.vt100.ansi.CSI;
import net.devdude.lispcraft.mod.common.vt100.ansi.SGR;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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
    private boolean doLocalEcho = false;

    private Style style = Style.DEFAULT;

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
                Mod.LOGGER.warn("VT100 IO exception", e);
                this.stop();
            }
        });
    }

    private void run() throws IOException {
        while (this.running) {
            var c = readNext();

            if (ANSI.isControlCharacter(c)) {
                handleAnsiControl(c);
            } else {
                write((char) c);
            }

            this.flush();
        }
    }

    public void stop() {
        assert this.running;
        this.running = false;
    }

    private int readNext() throws IOException {
        var next = this.input.read();
        if (ANSI.isControlCharacter(next)) {
            Mod.LOGGER.info("read {} ╳ {}", String.format("0x%02X", next), next);
        } else {
            Mod.LOGGER.info("read {} {} {}", String.format("0x%02X", next), (char) next, next);
        }
        return next;
    }

    private void handleAnsiControl(int c) throws IOException {
        switch (c) {
            case ANSI.LF -> newLine();
            case ANSI.BS -> backspace();
            case ANSI.TAB -> tab();
            case ANSI.CR -> {
                //                newLine();
                screen.setCursor(0, screen.getCursorY());
            }
            case ANSI.ESC -> handleC1(readNext());
            default -> Mod.LOGGER.warn("Unknown control character: {}", c);
        }
    }

    private void write(char c) {
        screen.setCharacter(c, this.style);

        if (screen.getCursorX() + 1 >= screen.getWidth()) {
            newLine();
            screen.setCursor(0, screen.getCursorY());
        } else {
            screen.setCursorRelative(1, 0);
        }
    }

    public void flush() {
        var screen = getScreen();
        for (var observer : this.observers) {
            observer.screenBufferUpdates(screen);
        }
    }

    private void newLine() {
        if (screen.getCursorY() + 1 >= screen.getHeight()) {
            this.screen.scroll(1);
        }
        screen.setCursorRelative(0, 1);
    }

    private void backspace() {
        if (screen.getCursorX() == 0) {
            if (screen.getCursorY() == 0) {
                return; // At the top of the screen, nothing to do
            }

            // The cursor is at the start of a line, we need to put it behind any text on the line above
            setCursorBehindText(screen.getCursorY() - 1);
        } else {
            // The cursor is somewhere in the middle of a line
            screen.setCursorRelative(-1, 0);
        }

        screen.setCharacter(' ', Style.DEFAULT);
    }

    private void tab() {
        // Move the cursor to the next multiple of 8
        var x = (8 - screen.getCursorX() % 8);
        for (int i = 0; i < x; i++) {
            write(' ');
        }
    }

    private void handleC1(int c1) throws IOException {
        switch (c1) {
            case ANSI.ESC -> {
            }
            case C1.CSI -> handleCSI();
            case C1.NEL -> newLine();
            case '(', ')', '*', '+' -> readNext(); // Designate character set, ignored
            //            case C1.CCH -> {
            //
            //            }
            default -> Mod.LOGGER.warn("Unknown control sequence: C1 {} [{}]", (char) c1, c1);
        }
    }

    public VT100ScreenBuffer getScreen() {
        return this.screen.clone();
    }

    private void setCursorBehindText(int y) {
        // Going backwards, find the first column without text and put the cursor to it's right
        // - If there is text in the last column, put the cursor on the last column
        // - If the line is empty, put the cursor on the first column
        for (var x = screen.getWidth() - 1; x >= 0; x--) {
            if (screen.getCodepointAt(x, y) != ' ') {
                // This is the last column with text in it
                screen.setCursor(Integer.min(x + 1, screen.getWidth() - 1), y);
                return;
            }
        }

        // This line is empty
        screen.setCursor(0, y);
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
            var next = readNext();
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
            case CSI.SGR -> {
                if (parameterBytes != null && (parameterBytes[0] == '>' || parameterBytes[0] == '?')) {
                    return; // No clue what this is. Full sequence seen: ">4;2" & "?4"
                }
                selectGraphicRendition(parseCSIParams(parameterBytes, 0));
            }
            case CSI.ED -> eraseInPage(parseCSIParam(parameterBytes, 0));
            case CSI.EL -> eraseInLine(parseCSIParam(parameterBytes, 0));
            case CSI.CUP -> cursorPosition(parseCSIParams(parameterBytes, 1, 1));
            case CSI.CUB -> cursorLeft(parseCSIParam(parameterBytes, 1));
            case CSI.CUF -> cursorRight(parseCSIParam(parameterBytes, 1));
            case CSI.CUD -> cursorDown(parseCSIParam(parameterBytes, 1));
            case CSI.CUU -> cursorUp(parseCSIParam(parameterBytes, 1));
            case CSI.DSR -> deviceStatusReport(parseCSIParam(parameterBytes, 5));
            case CSI.SM -> {
                // DEC Private mode
                var dec = (parameterBytes != null && parameterBytes[0] == '?');
                if (dec) {
                    parameterBytes = Arrays.copyOfRange(parameterBytes, 1, parameterBytes.length);
                }
                setMode(parseCSIParams(parameterBytes, 0, 0), dec);
            }
            case CSI.RM -> {
                // DEC Private mode
                var dec = (parameterBytes != null && parameterBytes[0] == '?');
                if (dec) {
                    parameterBytes = Arrays.copyOfRange(parameterBytes, 1, parameterBytes.length);
                }
                resetMode(parseCSIParams(parameterBytes, 0, 0), dec);
            }
            default -> {
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
    }

    private void selectGraphicRendition(int[] sgr) {
        if (sgr.length > 1) {
            Mod.LOGGER.info("Ignoring SGR sequence {}", sgr);
            return;
        }

        switch (sgr[0]) {
            case SGR.RESET -> this.style = Style.DEFAULT;
            case SGR.BOLD -> this.style = this.style.withIntensity(Style.Intensity.BOLD);
            case SGR.FAINT -> this.style = this.style.withIntensity(Style.Intensity.FAINT);
            case SGR.ITALIC -> this.style = this.style.withItalicized(true);
            case SGR.UNDERLINED -> this.style = this.style.withUnderlined(true);
            case SGR.INVERTED -> this.style = this.style.withNegative(true);
            case SGR.CONCEALED -> this.style = this.style.withConcealed(true);
            case SGR.CROSSED_OUT -> this.style = this.style.withCrossedOut(true);
            case SGR.NORMAL_INTENSITY -> this.style = this.style.withIntensity(Style.Intensity.NORMAL);
            case SGR.NOT_ITALIC -> this.style = this.style.withItalicized(false);
            case SGR.NOT_UNDERLINED -> this.style = this.style.withUnderlined(false);
            case SGR.NOT_INVERTED -> this.style = this.style.withNegative(false);
            case SGR.NOT_CONCEALED -> this.style = this.style.withConcealed(false);
            case SGR.NOT_CROSSED_OUT -> this.style = this.style.withCrossedOut(false);
            case SGR.FOREGROUND_BLACK -> this.style = this.style.withForeground(Style.BLACK);
            case SGR.FOREGROUND_RED -> this.style = this.style.withForeground(Style.RED);
            case SGR.FOREGROUND_GREEN -> this.style = this.style.withForeground(Style.GREEN);
            case SGR.FOREGROUND_YELLOW -> this.style = this.style.withForeground(Style.YELLOW);
            case SGR.FOREGROUND_BLUE -> this.style = this.style.withForeground(Style.BLUE);
            case SGR.FOREGROUND_MAGENTA -> this.style = this.style.withForeground(Style.MAGENTA);
            case SGR.FOREGROUND_CYAN -> this.style = this.style.withForeground(Style.CYAN);
            case SGR.FOREGROUND_WHITE -> this.style = this.style.withForeground(Style.WHITE);
            case SGR.FOREGROUND_DEFAULT -> this.style = this.style.withForeground(Style.DEFAULT.foreground());
            case SGR.BACKGROUND_BLACK -> this.style = this.style.withBackground(Style.BLACK);
            case SGR.BACKGROUND_RED -> this.style = this.style.withBackground(Style.RED);
            case SGR.BACKGROUND_GREEN -> this.style = this.style.withBackground(Style.GREEN);
            case SGR.BACKGROUND_YELLOW -> this.style = this.style.withBackground(Style.YELLOW);
            case SGR.BACKGROUND_BLUE -> this.style = this.style.withBackground(Style.BLUE);
            case SGR.BACKGROUND_MAGENTA -> this.style = this.style.withBackground(Style.MAGENTA);
            case SGR.BACKGROUND_CYAN -> this.style = this.style.withBackground(Style.CYAN);
            case SGR.BACKGROUND_WHITE -> this.style = this.style.withBackground(Style.WHITE);
            case SGR.BACKGROUND_DEFAULT -> this.style = this.style.withBackground(Style.DEFAULT.background());
            default -> Mod.LOGGER.info("Unhandled SGR {}", sgr);
        }
    }

    private int[] parseCSIParams(byte @Nullable [] parameter, int... defaults) {
        if (parameter == null) {
            return defaults;
        }

        var stringParams = new String(parameter).split(";");

        var params = defaults;
        if (stringParams.length > params.length) {
            params = Arrays.copyOf(params, stringParams.length);
        }

        for (int i = 0; i < stringParams.length; i++) {
            params[i] = Integer.parseInt(stringParams[i]);
        }

        return params;
    }

    private void eraseInPage(int p) {
        switch (p) {
            // Erase from cursor to end of screen
            case 0 -> {
                if (screen.getCursorY() < (screen.getHeight() - 1)) {
                    screen.eraseLines(screen.getCursorY() + 1, screen.getHeight());
                }
                eraseInLine(p);
            }
            // Erase from cursor to beginning of screen
            case 1 -> {
                if (screen.getCursorY() > 0) {
                    screen.eraseLines(0, screen.getCursorY());
                }
                eraseInLine(p);
            }
            // Erase the whole page
            case 2 -> screen.eraseLines(0, screen.getHeight());
        }
    }

    private int parseCSIParam(byte @Nullable [] parameter, int def) {
        if (parameter == null) {
            return def;
        } else {
            return Integer.parseInt(new String(parameter));
        }
    }

    private void eraseInLine(int p) {
        switch (p) {
            // Erase from cursor to end of line
            case 0 -> screen.eraseInLine(screen.getCursorX(), screen.getWidth());
            // Erase from cursor to beginning of line
            case 1 -> screen.eraseInLine(0, screen.getCursorX() + 1);
            // Erase the whole line
            case 2 -> screen.eraseInLine(0, screen.getWidth());
        }
    }

    private void cursorPosition(int[] params) {
        var x = Integer.max(Integer.min(params[0] - 1, this.screen.getWidth() - 1), 0);
        var y = Integer.max(Integer.min(params[1] - 1, this.screen.getHeight() - 1), 0);

        this.screen.setCursor(x, y);
    }

    private void cursorLeft(int n) {
        this.screen.setCursorRelative(-Integer.min(n, this.screen.getCursorX()), 0);
    }

    private void cursorRight(int n) {
        this.screen.setCursorRelative(Integer.min(n, (this.screen.getWidth() - 1) - this.screen.getCursorX()), 0);
    }

    private void cursorDown(int n) {
        this.screen.setCursorRelative(0, Integer.min(n, (this.screen.getHeight() - 1) - this.screen.getCursorY()));
    }

    private void cursorUp(int n) {
        this.screen.setCursorRelative(0, -Integer.min(n, this.screen.getCursorY()));
    }

    private void deviceStatusReport(int dsr) throws IOException {
        var report = new ByteArrayOutputStream();
        switch (dsr) {
            case 5 -> report.write(new byte[]{
                    ANSI.ESC, C1.CSI, '0', 'n'
            });
            case 6 -> {
                report.write(ANSI.ESC);
                report.write(C1.CSI);
                report.write(String.valueOf(this.screen.getCursorY()).getBytes());
                report.write(';');
                report.write(String.valueOf(this.screen.getCursorX()).getBytes());
                report.write('R');
            }
            default -> throw new IllegalStateException("Unexpected value: " + dsr);
        }

        this.output.write(report.toByteArray());
    }

    private void setMode(int[] params, boolean dec) {
        if (dec) {
            Mod.LOGGER.warn("CSI ? {} h", params);
        } else {
            Mod.LOGGER.warn("CSI {} h", params);
        }
    }

    private void resetMode(int[] params, boolean dec) {
        if (dec) {
            switch (params[0]) {
                case 2004 -> {
                } // Reset bracketed paste mode
                default -> Mod.LOGGER.warn("CSI ? {} l", params);
            }
        } else {
            Mod.LOGGER.warn("CSI {} l", params);
        }
    }

    // TODO: onChange logic is a hold over and probably not the best solution
    public void onChange(ScreenBufferConsumer observer) {
        this.observers.add(observer);
    }

    // accepts keyboard input after it's been translated to ANSI
    public void writeKeyboardInput(byte[] input) throws IOException {
        this.output.write(input);
        this.output.flush();

        if (doLocalEcho) {
            this.hostOutput.write(input);
            this.hostOutput.flush();
        }
    }

    @FunctionalInterface
    public interface ScreenBufferConsumer {
        void screenBufferUpdates(VT100ScreenBuffer buffer);
    }


}

