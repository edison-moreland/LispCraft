package net.devdude.lispcraft.runtime;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Console extends OutputStream {
    public final Size size;
    protected final List<ConsoleStateConsumer> observers;

    private char[][] buffer;
    private Location cursor;

    public Console(Size size) {
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
    }

    //    TODO: onChange logic is a hold over and probably not the best solution
    public void onChange(ConsoleStateConsumer observer) {
        this.observers.add(observer);
    }

    public char[][] getScreen() {
        return this.buffer.clone();
    }

    @Override
    public void write(int c) {
        if (ANSI.isControlCharacter(c)) {
            handleControl(c);
        } else {
            print((char) c);
        }
    }

    @Override
    public void flush() {
        var buffer = this.buffer.clone();
        var cursor = this.cursor;
        for (var observer : this.observers) {
            observer.consoleStateUpdate(buffer, cursor);
        }
    }

    private void print(char c) {
        buffer[cursor.y][cursor.x] = c;

        if (cursor.x + 1 >= size.width) {
            newLine();
        } else {
            moveCursor(1, 0);
        }
    }

    private void handleControl(int c) {
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
                moveCursor(0, cursor.y);
                break;
        }
    }

    private void tab() {
        // Move the cursor to the next multiple of 8
        var x = (8 - cursor.x % 8);
        for (int i = 0; i < x; i++) {
            print(' ');
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
        assert x >= 0 && x < size.width && y >= 0 && y < size.height;
        this.cursor = new Location(x, y);
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
