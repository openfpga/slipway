package com.atmel.fpslic;

public class FpslicConstants {
    public static final int NONE  = -1;
    public static final int L0    = 0;
    public static final int L1    = 1;
    public static final int L2    = 2;
    public static final int L3    = 3;
    public static final int L4    = 4;

    public static final int NORTH = 8;
    public static final int WEST  = 9;
    public static final int SOUTH = 10;
    public static final int EAST  = 11;

    public static final int XLUT  = 12;
    public static final int YLUT  = 13;
    public static final int ZMUX  = 14;

    public static final int H4    = 15;
    public static final int V4    = 16;

    public static final int NW    = 20;
    public static final int SW    = 21;
    public static final int NE    = 22;
    public static final int SE    = 23;

    public static final int SLOW   = 24;
    public static final int MEDIUM = 25;
    public static final int FAST   = 26;

    public static final int ALWAYS_ON  = 27;
    public static final int ALWAYS_OFF = 28;

    public static final int FB    = 29;

    public static final int LUT_SELF  = 0xAA;  // 1010 1010
    public static final int LUT_Z     = 0xF0;  // 1111 0000
    public static final int LUT_OTHER = 0xCC;  // 1100 1100

    public static final int TMUX_W_AND_Z   = 0x00001001;
    public static final int TMUX_W         = 0x00001002;
    public static final int TMUX_Z         = 0x00001004;
    public static final int TMUX_W_AND_FB  = 0x00001008;
    public static final int TMUX_FB        = 0x00001010;


}
