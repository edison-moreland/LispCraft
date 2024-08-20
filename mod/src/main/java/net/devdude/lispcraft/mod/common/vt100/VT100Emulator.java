package net.devdude.lispcraft.mod.common.vt100;

import net.devdude.lispcraft.mod.Mod;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Emulate the screen buffer of a VT100 console
// Writing to the output stream will update the screen buffer
public class VT100Emulator {
    public final Size size;
    protected final List<ConsoleStateConsumer> observers;

    private final PipedInputStream input;
    private final PipedOutputStream hostOutput;

    private final PipedOutputStream output;
    private final PipedInputStream hostInput;

    private char[][] buffer;
    private Location cursor;

    private boolean running = false;

    public VT100Emulator(Size size) {
        this.observers = new ArrayList<>();

        this.size = size;
        this.buffer = new char[size.height][size.width];
        this.cursor = new Location(0, 0);

        // TODO: We fill the buffer with spaces because a null character is rendered as a box
        //       Since we control text rendering, we should just not render null characters
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++) {
                this.buffer[y][x] = ' ';
            }
        }

        try {
            this.input = new PipedInputStream();
            this.hostOutput = new PipedOutputStream(this.input);

            this.output = new PipedOutputStream();
            this.hostInput = new PipedInputStream(this.output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

            Mod.LOGGER.info("Writing {}", c);
            if (ANSI.isControlCharacter(c)) {
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
                        setCursor(0, cursor.y);
                        break;
                    default:
                        Mod.LOGGER.warn("Unknown control character: {}", c);
                }
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

    //    TODO: onChange logic is a hold over and probably not the best solution
    public void onChange(ConsoleStateConsumer observer) {
        this.observers.add(observer);
    }

    public char[][] getScreen() {
        return this.buffer.clone();
    }

    // accepts keyboard input after it's been translated to ANSI
    public void writeKeyboardInput(byte[] input) throws IOException {
        this.output.write(input);
        this.output.flush();
        // TODO: Local echo handled here
    }

    public void flush() {
        var buffer = this.buffer.clone();
        var cursor = this.cursor;
        for (var observer : this.observers) {
            observer.consoleStateUpdate(buffer, cursor);
        }
    }

    private void write(char c) {
        buffer[cursor.y][cursor.x] = c;

        if (cursor.x + 1 >= size.width) {
            newLine();
        } else {
            moveCursor(1, 0);
        }
    }

    private void tab() {
        // Move the cursor to the next multiple of 8
        var x = (8 - cursor.x % 8);
        for (int i = 0; i < x; i++) {
            write(' ');
        }
    }

    private void newLine() {
        if (cursor.y + 1 >= size.height) {
            scroll(1);
            setCursor(0, size.height - 1);
        } else {
            setCursor(0, cursor.y + 1);
        }
    }


    private void backspace() {
        if (cursor.x == 0) {
            if (cursor.y == 0) {
                return; // At the top of the screen, nothing to do
            }

            // The cursor is at the start of a line, we need to put it behind any text on the line above
            setCursorBehindText(cursor.y - 1);
        } else {
            // The cursor is somewhere in the middle of a line
            moveCursor(-1, 0);
        }

        buffer[cursor.y][cursor.x] = ' ';
    }

    private void scroll(int by) {
        for (var y = 0; y < size.height; y++) {
            if (y < size.height - by) {
                buffer[y] = buffer[y + by];
            } else {
                buffer[y] = new char[size.width];
                for (var x = 0; x < size.width; x++) {
                    buffer[y][x] = ' ';
                }
            }
        }
        moveCursor(0, -by);
    }

    private void setCursorBehindText(int y) {
        // Going backwards, find the first column without text and put the cursor to it's right
        // - If there is text in the last column, put the cursor on the last column
        // - If the line is empty, put the cursor on the first column
        for (var x = size.width - 1; x >= 0; x--) {
            if (buffer[y][x] != ' ') {
                // This is the last column with text in it
                setCursor(Integer.min(x + 1, size.width - 1), y);
                return;
            }
        }

        // This line is empty
        setCursor(0, y);
    }

    private void moveCursor(int x, int y) {
        setCursor(cursor.x + x, cursor.y + y);
    }

    private void setCursor(int x, int y) {
        if ((x < 0 || x >= size.width) || (y < 0 || y >= size.height)) {
            throw new RuntimeException("cursor out of bounds");
        }
        this.cursor = new Location(x, y);
    }

    public Location getCursor() {
        return this.cursor;
    }

    @FunctionalInterface
    public interface ConsoleStateConsumer {
        void consoleStateUpdate(char[][] buffer, Location cursor);
    }

    public record Size(int width, int height) {
    }

    public record Location(int x, int y) {
    }
}
