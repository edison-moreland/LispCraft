package net.devdude.lispcraft.mod.common.vt100;

import io.wispforest.endec.impl.RecordEndec;

public record Location(int x, int y) {
    public static final RecordEndec<Location> ENDEC = RecordEndec.createShared(Location.class);
}
