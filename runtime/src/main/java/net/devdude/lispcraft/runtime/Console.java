package net.devdude.lispcraft.runtime;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Console {
    public final Size size;
    protected final List<Consumer<char[][]>> observers;

    private char[][] buffer;
    private Location cursor;

    public Console(Size size) {
        this.observers = new ArrayList<>();

        this.size = size;
        this.buffer = new char[size.height][size.width];
        this.cursor = new Location(0, 0);

        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++) {
                this.buffer[y][x] = ' ';
            }
        }
    }

    //    TODO: Observe logic is a hold over and probably not the best solution
    public void observe(Consumer<char[][]> observer) {
        this.observers.add(observer);
    }

    public void flush() {
        var chars = this.buffer.clone();
        for (var observer : this.observers) {
            observer.accept(chars);
        }
    }

    public char[][] getScreen() {
        return this.buffer.clone();
    }

    public void moveCursor(int x, int y) {
        setCursor(cursor.x + x, cursor.y + y);
    }

    public void setCursor(int x, int y) {
        assert x >= 0 && x < size.width && y >= 0 && y < size.height;
        this.cursor = new Location(x, y);
    }

    public void scroll(int by) {
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

    public void newLine() {
        if (cursor.y + 1 >= size.height) {
            scroll(1);
            setCursor(0, size.height - 1);
        } else {
            setCursor(0, cursor.y + 1);
        }
    }

    public void print(char c) {
        buffer[cursor.y][cursor.x] = c;

        if (cursor.x + 1 >= size.width) {
            newLine();
        } else {
            moveCursor(1, 0);
        }
    }

    public void print(char[] text) {
        for (char character : text) {
            if (character == '\n') {
                newLine();
                continue;
            }

            print(character);
        }

        this.flush();
    }

    public void print(String text) {
        print(text.toCharArray());
    }

    public record Size(int width, int height) {
    }

    public record Location(int x, int y) {
    }
}
