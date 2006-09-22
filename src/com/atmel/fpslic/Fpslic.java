package com.atmel.fpslic;

import java.util.*;
import java.io.*;
import org.ibex.util.Log;

public abstract class Fpslic {

    public static class Util {
        public static int lutSwap(int x) {
            return
                (x & 0x80)        |
                ((x & 0x20) << 1) |
                ((x & 0x40) >> 1) |
                (x & 0x10) |
                (x & 0x08)        |
                ((x & 0x02) << 1) |
                ((x & 0x04) >> 1) |
                (x & 0x01);
        }
    }
    
    /** issue a command to the device in Mode4 format; see Gosset's documentation for further details */
    public int getWidth() { return 24; }
    public int getHeight() { return 24; }

    private static String hex2(int i) {
        String ret = Integer.toString(i, 16);
        while(ret.length() < 2) ret = "0"+ret;
        return ret.toUpperCase();
    }

    public void readMode4(InputStream in) throws IOException {
        int count = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        for(String str = br.readLine(); str != null; str = br.readLine()) {
            long foo = Long.parseLong(str, 16);
            mode4((int)(foo >> 24), (int)(foo >> 16), (int)(foo >>  8), (int)(foo >>  0));
            count++;
        }
        flush();
        in.close();
    }

    public abstract void flush();

    public void writeMode4(Writer w) throws IOException {
        for(int x=0; x<getWidth(); x++)
            for(int y=0; y<getWidth(); y++)
                for(int z=0; z<255; z++) {
                    if ((z > 0x09 && z < 0x10) ||
                        (z > 0x11 && z < 0x20) ||
                        (z > 0x29 && z < 0x30) ||
                        (z > 0x39 && z < 0x40) ||
                        (z > 0x41 && z < 0x60) ||
                        (z > 0x67 && z < 0x70) ||
                        (z > 0x77 && z < 0xD0) ||
                        (z > 0xD3))
                        continue;
                    w.write(hex2(z));
                    w.write(hex2(y));
                    w.write(hex2(x));
                    w.write(hex2(mode4(z, y, x) & 0xff));
                    w.write('\n');
                }
        w.flush();
    }


    public abstract void mode4(int z, int y, int x, int d);
    public abstract byte mode4(int z, int y, int x);
    public          byte mode4zyx(int zyx) { return mode4(zyx>>24, (zyx>>16)&0xff, (zyx>>8)&0xff); }
    public          void mode4zyx(int zyx, int d, int invmask) { mode4(zyx>>24, (zyx>>16)&0xff, (zyx>>8)&0xff, d, invmask); }
    public          void mode4(int z, int y, int x, int d, int invmask) {
        int old = mode4(z, y, x);
        old &= ~invmask;
        old |= d;
        mode4(z, y, x, old);
    }
    public          void mode4zyx(int zyx, int bit, boolean set) { mode4(zyx>>24, (zyx>>16)&0xff, (zyx>>8)&0xff, bit, set); }
    public          void mode4(int z, int y, int x, int bit, boolean set) {
        int old = mode4(z, y, x);
        old &= ~(1 << bit);
        old |= set ? (1 << bit) : 0;
        mode4(z, y, x, old);
    }
}
