package net.devdude.lispcraft.runtime;

public class ANSI {

    public static final char NUL = 0x0;
    public static final char SOH = 0x1;
    public static final char STX = 0x2;
    public static final char ETX = 0x3;
    public static final char EOT = 0x4;
    public static final char ENQ = 0x5;
    public static final char ACK = 0x6;
    public static final char BEL = 0x7;
    public static final char BS = 0x8;
    public static final char TAB = 0x9;
    public static final char LF = 0xA;
    public static final char VT = 0xB;
    public static final char FF = 0xC;
    public static final char CR = 0xD;
    public static final char SO = 0xE;
    public static final char SI = 0xF;
    public static final char DLE = 0x10;
    public static final char DC1 = 0x11;
    public static final char DC2 = 0x12;
    public static final char DC3 = 0x13;
    public static final char DC4 = 0x14;
    public static final char NAK = 0x15;
    public static final char SYN = 0x16;
    public static final char ETB = 0x17;
    public static final char CAN = 0x18;
    public static final char EM = 0x19;
    public static final char SUB = 0x1A;
    public static final char ESC = 0x1B;
    public static final char FS = 0x1C;
    public static final char GS = 0x1D;
    public static final char RS = 0x1E;
    public static final char US = 0x1F;

    public static boolean isControlCharacter(char c) {
        return c <= ANSI.US;
    }
}
