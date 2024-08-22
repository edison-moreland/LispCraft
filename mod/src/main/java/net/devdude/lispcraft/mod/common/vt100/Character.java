package net.devdude.lispcraft.mod.common.vt100;

import io.wispforest.endec.impl.RecordEndec;

public record Character(char codepoint, Style style) {
    public static final RecordEndec<Character> ENDEC = RecordEndec.createShared(Character.class);
}

