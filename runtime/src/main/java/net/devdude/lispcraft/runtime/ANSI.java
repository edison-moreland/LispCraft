package net.devdude.lispcraft.runtime;

public class ANSI {

    public static final int NUL = 0x0;
    public static final int SOH = 0x1;
    public static final int STX = 0x2;
    public static final int ETX = 0x3;
    public static final int EOT = 0x4;
    public static final int ENQ = 0x5;
    public static final int ACK = 0x6;
    public static final int BEL = 0x7;
    public static final int BS = 0x8;
    public static final int TAB = 0x9;
    public static final int LF = 0xA;
    public static final int VT = 0xB;
    public static final int FF = 0xC;
    public static final int CR = 0xD;
    public static final int SO = 0xE;
    public static final int SI = 0xF;
    public static final int DLE = 0x10;
    public static final int DC1 = 0x11;
    public static final int DC2 = 0x12;
    public static final int DC3 = 0x13;
    public static final int DC4 = 0x14;
    public static final int NAK = 0x15;
    public static final int SYN = 0x16;
    public static final int ETB = 0x17;
    public static final int CAN = 0x18;
    public static final int EM = 0x19;
    public static final int SUB = 0x1A;
    public static final int ESC = 0x1B;
    public static final int FS = 0x1C;
    public static final int GS = 0x1D;
    public static final int RS = 0x1E;
    public static final int US = 0x1F;

    public static boolean isControlCharacter(int c) {
        return c <= ANSI.US;
    }
}
