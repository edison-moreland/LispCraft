package net.devdude.lispcraft.mod.common.vt100;

import io.wispforest.endec.impl.RecordEndec;

public record Size(int width, int height) {
    public static final RecordEndec<Size> ENDEC = RecordEndec.createShared(Size.class);
}
