package net.devdude.lispcraft.mod.common.vt100;

import io.wispforest.endec.impl.RecordEndec;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Style(int foreground, int background, boolean bold) {
    public static final RecordEndec<Style> ENDEC = RecordEndec.createShared(Style.class);

    public static final Style DEFAULT = new Style(0xFFFFFFFF, 0xFF000000, false);

    @Contract("_ -> new")
    public @NotNull Style withForeground(int color) {
        return new Style(color, this.background, this.bold);
    }

    @Contract("_ -> new")
    public @NotNull Style withBackground(int color) {
        return new Style(this.foreground, color, this.bold);
    }

    @Contract("_ -> new")
    public @NotNull Style withBold(boolean bold) {
        return new Style(this.foreground, this.background, bold);
    }
}
