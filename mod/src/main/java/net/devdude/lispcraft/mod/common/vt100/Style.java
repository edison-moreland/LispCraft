package net.devdude.lispcraft.mod.common.vt100;

import io.wispforest.endec.impl.RecordEndec;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Style(
        int foreground,
        int background,
        Intensity intensity,
        boolean italicized,
        boolean underlined,
        boolean negative,
        boolean concealed,
        boolean crossedOut
) {
    public static final RecordEndec<Style> ENDEC = RecordEndec.createShared(Style.class);
    public static int BLACK = 0xFF000000;
    public static int RED = 0xFFCD0000;
    public static int GREEN = 0xFF00CD00;
    public static int YELLOW = 0xFFCDCD00;
    public static int BLUE = 0xFF0000EE;
    public static int MAGENTA = 0xFFCD00CD;
    public static int CYAN = 0xFF00CDCD;
    public static int WHITE = 0xFFE5E5E5;
    public static final Style DEFAULT = new Style(WHITE, BLACK, Intensity.NORMAL, false, false, false, false, false);
    public static int BRIGHT_BLACK = 0xFF7F7F7F;
    public static int BRIGHT_RED = 0xFFFF0000;
    public static int BRIGHT_GREEN = 0xFF00FF00;
    public static int BRIGHT_YELLOW = 0xFFFFFF00;
    public static int BRIGHT_BLUE = 0xFF5C5CFF;
    public static int BRIGHT_MAGENTA = 0xFFFF00FF;
    public static int BRIGHT_CYAN = 0xFF00FFFF;
    public static int BRIGHT_WHITE = 0xFFFFFFFF;
    public static Intensity NORMAL = Intensity.NORMAL;
    public static Intensity BOLD = Intensity.BOLD;
    public static Intensity FAINT = Intensity.FAINT;

    @Contract("_ -> new")
    public @NotNull Style withForeground(int color) {
        return new Style(
                color,
                this.background,
                this.intensity,
                this.italicized,
                this.underlined,
                this.negative,
                this.concealed,
                this.crossedOut
        );
    }

    @Contract("_ -> new")
    public @NotNull Style withBackground(int color) {
        return new Style(
                this.foreground,
                color,
                this.intensity,
                this.italicized,
                this.underlined,
                this.negative,
                this.concealed,
                this.crossedOut
        );
    }

    @Contract("_ -> new")
    public @NotNull Style withIntensity(Intensity intensity) {
        return new Style(
                this.foreground,
                this.background,
                intensity,
                this.italicized,
                this.underlined,
                this.negative,
                this.concealed,
                this.crossedOut
        );
    }

    @Contract("_ -> new")
    public @NotNull Style withItalicized(boolean italicized) {
        return new Style(
                this.foreground,
                this.background,
                this.intensity,
                italicized,
                this.underlined,
                this.negative,
                this.concealed,
                this.crossedOut
        );
    }

    @Contract("_ -> new")
    public @NotNull Style withUnderlined(boolean underlined) {
        return new Style(
                this.foreground,
                this.background,
                this.intensity,
                this.italicized,
                underlined,
                this.negative,
                this.concealed,
                this.crossedOut
        );
    }

    @Contract("_ -> new")
    public @NotNull Style withNegative(boolean negative) {
        return new Style(
                this.foreground,
                this.background,
                this.intensity,
                this.italicized,
                this.underlined,
                negative,
                this.concealed,
                this.crossedOut
        );
    }

    @Contract("_ -> new")
    public @NotNull Style withConcealed(boolean concealed) {
        return new Style(
                this.foreground,
                this.background,
                this.intensity,
                this.italicized,
                this.underlined,
                this.negative,
                concealed,
                this.crossedOut
        );
    }

    @Contract("_ -> new")
    public @NotNull Style withCrossedOut(boolean crossedOut) {
        return new Style(
                this.foreground,
                this.background,
                this.intensity,
                this.italicized,
                this.underlined,
                this.negative,
                this.concealed,
                crossedOut
        );
    }

    public enum Intensity {
        NORMAL, BOLD, FAINT
    }
}
