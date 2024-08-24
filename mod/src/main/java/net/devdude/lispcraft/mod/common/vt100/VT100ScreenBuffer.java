package net.devdude.lispcraft.mod.common.vt100;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

// Holds the current state of the screen and all information needed for presentation
// This class is synced to the client when rendering the ConsoleScreen
public class VT100ScreenBuffer implements Cloneable {
    public static final Endec<Character[][]> SCREEN_ENDEC = Character.ENDEC.listOf().listOf().xmap(
            (list) -> list.stream().map((innerList) -> innerList.toArray(Character[]::new)).toArray(Character[][]::new),
            (array) -> Arrays.stream(array).map((innerArray) -> Arrays.stream(innerArray).toList()).toList()
    );
    public static final Endec<VT100ScreenBuffer> ENDEC = StructEndecBuilder.of(
            Size.ENDEC.fieldOf("size", VT100ScreenBuffer::getSize),
            Location.ENDEC.fieldOf("cursor", VT100ScreenBuffer::getCursor),
            SCREEN_ENDEC.fieldOf("contents", VT100ScreenBuffer::getContents),
            VT100ScreenBuffer::new
    );
    public final Size size;
    private Location cursor;

    private Character[][] contents;

    public VT100ScreenBuffer(Size size) {
        this(size, new Location(0, 0), emptyContents(size));
    }

    private VT100ScreenBuffer(@NotNull Size size, @NotNull Location cursor, @NotNull Character[][] contents) {
        this.size = size;
        this.cursor = cursor;
        this.contents = contents;
    }

    private static Character @NotNull [] @NotNull [] emptyContents(@NotNull Size size) {
        // TODO: We fill the buffer with spaces because a null character is rendered as a box
        //       Since we control text rendering, we should just not render null characters
        var contents = new Character[size.height()][size.width()];
        for (int y = 0; y < size.height(); y++) {
            for (int x = 0; x < size.width(); x++) {
                contents[y][x] = emptyCharacter();
            }
        }
        return contents;
    }

    private static Character emptyCharacter() {
        return new Character(' ', Style.DEFAULT);
    }

    public Character[][] getContents() {
        return this.contents;
    }

    public Size getSize() {
        return this.size;
    }

    public int getWidth() {
        return this.size.width();
    }

    public int getHeight() {
        return this.size.height();
    }

    public Location getCursor() {
        return this.cursor;
    }

    public int getCursorX() {
        return this.cursor.x();
    }

    public int getCursorY() {
        return this.cursor.y();
    }

    public void setCharacter(char codepoint, Style style) {
        contents[cursor.y()][cursor.x()] = new Character(codepoint, style);
    }

    public Character getCharacterAt(int x, int y) {
        return contents[y][x];
    }

    public char getCodepointAt(int x, int y) {
        return contents[y][x].codepoint();
    }

    public void scroll(int by) {
        for (var y = 0; y < size.height(); y++) {
            if (y < size.height() - by) {
                contents[y] = contents[y + by];
            } else {
                contents[y] = emptyLine(size.width());
            }
        }
        setCursorRelative(0, -by);
    }

    private static Character @NotNull [] emptyLine(int width) {
        var contents = new Character[width];
        for (int x = 0; x < width; x++) {
            contents[x] = emptyCharacter();
        }
        return contents;
    }

    public void setCursorRelative(int x, int y) {
        setCursor(cursor.x() + x, cursor.y() + y);
    }

    public void setCursor(int x, int y) {
        if ((x < 0 || x >= size.width()) || (y < 0 || y >= size.height())) {
            throw new RuntimeException("cursor out of bounds");
        }
        cursor = new Location(x, y);
    }

    // Erase lines from start to end-1
    public void eraseLines(int start, int end) {
        if (end < start) {
            var tmp = end;
            end = start;
            start = tmp;
        }

        for (int y = start; y < end; y++) {
            this.contents[y] = emptyLine(size.width());
        }
    }

    // Erase characters from start to end-1 in the current line
    public void eraseInLine(int start, int end) {
        if (end < start) {
            var tmp = end;
            end = start;
            start = tmp;
        }

        for (int x = start; x < end; x++) {
            this.contents[this.cursor.y()][x] = emptyCharacter();
        }
    }

    @Override
    public VT100ScreenBuffer clone() {
        try {
            var screen = (VT100ScreenBuffer) super.clone();
            screen.contents = screen.contents.clone();

            return screen;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
