package edu.berkeley.cs.obits;
import java.util.*;
import java.io.*;
import org.ibex.util.*;

public class Atmel {

    public static String pad(int len, String s) { if (s.length() >= len) return s.toUpperCase(); return "0"+pad(len-1,s); }
    public static String hex32(int i) { return pad(8, Long.toString(i & 0xffffffffffffffffL, 16)); }
    public static String hex24(int i) { return pad(6, Integer.toString((i & 0xffffff), 16)); }
    public static String hex8(int i)  { return pad(2, Integer.toString((i & 0xff), 16)); }
    public static String bin8(int i)  { return pad(8, Integer.toString((i & 0xff), 2)); }
    public static String dec2(int i)  { return pad(2, Integer.toString(i & 0xff)); }

    public static void main(String[] s) throws Exception {
        Log.level = Log.DEBUG;
        new Bits().read(System.in);
    }

    private static class Bits {
        private byte[] bits    = new byte[0];
        //public int  get(int whichByte, int whichBit, int len) { }
        //public void set(int whichByte, int whichBit, int len, int val) { }

        private int    control = 0;
        public int  control()            { return control; }
        public void control(int control) { this.control = control; }

        public void read2(InputStream bitstream) throws IOException {
            DataInputStream dis = new DataInputStream(new Encode.Ascii.In(16, bitstream));
            try {
                while(true) {
                    dis.readUnsignedByte();
                    dis.readUnsignedByte();
                    dis.readUnsignedByte();
                    int i = dis.readUnsignedByte();
                    System.out.println(bin8(i));
                }
            } catch (EOFException e) {
            }
        }
        public void read(InputStream bitstream) throws IOException {
            DataInputStream dis = new DataInputStream(new Encode.Ascii.In(2, bitstream));
            int b = dis.readUnsignedByte();
            if (b != 0x00) throw new Error("bitstream did not start with zero-byte; got " + b);
            Log.debug(this, "saw leading zero-byte");
            b = dis.readUnsignedByte();
            if (b != 0xB7) throw new Error("bitstream did not start with preamble byte B7; got " + b);
            Log.debug(this, "saw preamble byte");
            control(dis.readInt());
            Log.debug(this, "set control register to " + hex32(control()));
            int numWindows = dis.readShort();
            Log.debug(this, "this bitstream has " + numWindows + " window(s)");
            int count = 0;
            for(int i=0; i<numWindows; i++) {
                int start      = (dis.readUnsignedByte() << 16) | (int)dis.readShort();
                int end        = (dis.readUnsignedByte() << 16) | (int)dis.readShort();
                count = 0;
                Log.debug(this, "  window " + dec2(i) + " spans addresses " + hex24(start) + " - " + hex24(end) + "[count="+count+"]");
                int _z = (start & 0x0000ff) >>  0;
                int _y = (start & 0x00ff00) >>  8;
                int _x = (start & 0xff0000) >> 16;
                int z_ = (end & 0x0000ff) >>  0;
                int y_ = (end & 0x00ff00) >>  8;
                int x_ = (end & 0xff0000) >> 16;
                int z =  (start & 0x0000ff) >>  0;
                int y =  (start & 0x00ff00) >>  8;
                int x =  (start & 0xff0000) >> 16;

                while(true) {
                    count++;
                    mode4(x, y, z, dis.readUnsignedByte());
                    x++;
                    if (x > 15) { x = 0; y++; }
                    if (y > 15) { y = 0; z++; }
                    do {
                        z++;
                        if (z > 77) {
                            z = _z;
                            x++;
                            if (x > 15) {
                                x = _x;
                                y++;
                                if (y > y_) break;
                            }
                        }
                    } while(!valid(x,y,z));
                }
                //Log.debug(this, "    read " + count + " bytes (" + hex24(count)+")");
            }
            /*
            Log.debug(this, "done " + count);
            b = dis.readUnsignedByte();
            if (b != 0xB7) throw new Error("bitstream did not start with preamble byte B7; got " + bin8(b));
            Log.debug(this, "done2");
            */
        }

        public boolean valid(int x, int y, int z) {
            switch(z) {
                case 0x00: case 0x01: break;
            }
            if (x > 15) return false;
            if (y > 15) return false;
            return true;
        }

        public void mode4(int x, int y, int z, int d) {
            //Log.debug(this, "    ("+dec2(x)+","+dec2(y)+"):"+hex8(z)+" = "+bin8(d));
            //System.out.println(hex8(z)+hex8(y)+hex8(x)+hex8(d));
        }
    }
}
        /*
    public static class At40k extends FPGA {


        public At40k(int width, int height) { this.width = width; this.height = height; }

        public static class At40k10 extends At40k { public At40k10() { super(24, 24); } }

        public Sector sector(int col, int row) { return new Sector(col, row); }
        public final class Sector {
            public final int col;
            public final int row;
            public Sector(int col, int row) {
                if (row % 4 != 0) throw new Error("Sector must be created with a multiple-of-4 row");
                if (col % 4 != 0) throw new Error("Sector must be created with a multiple-of-4 col");
                this.row = row;
                this.col = col;
            }
        }

        public Cell cell(int col, int row) { return new Cell(col, row); }
        public final class Cell {
            public final int col;
            public final int row;
            public Sector getSector() { return sector(col - (col % 4), row - (row % 4)); }
            public Cell(int col, int row) {
                this.row = row;
                this.col = col;
            }
        }

        public class Sector {
            Cell[][] = new Cell[4][4];
            Ram ram  = new Ram();

            Buf[] west  = new Buf[4];
            Buf[] east  = new Buf[4];
            Buf[] north = new Buf[4];
            Buf[] south = new Buf[4];
            Pip   pass  = new Pip[4][4][5][2];
        }


        public class Cell {

            public final Port[] h = new Port[5] { port(), port(), port(), port(), port() };
            public final Port[] v = new Port[5] { port(), port(), port(), port(), port() };
            public final Port[] s = new Port[5] { port(), port(), port(), port(), port() };
            public final Port nw, sw, se, ne;
            public final Port n,  s,  w,  e;
            public final Port xout, yout;

            public final Pip zin     = pip(                s[0], s[1], s[2], s[3], s[4]);
            public final Pip win     = pip(                s[0], s[1], s[2], s[3], s[4]);
            public final Pip xin     = pip(nw, ne, sw, se, s[0], s[1], s[2], s[3], s[4]);
            public final Pip yin     = pip(n,  s,  e,  w,  s[0], s[1], s[2], s[3], s[4]);

            public final Pip wpip    = pip(win, zin, and(win, zin), fb);
            public final Pip zpip    = pip(zin, one, zero);

            public final Lut xlut    = lut(xpip, wmux, ypip);
            public final Lut ylut    = lut(xpip, wmux, ypip);
            public final Mux zmux    = mux(xlut, ylut, zpip);
            public final Reg reg     = reg(zmux);
            public final Pip center  = pip(reg, zmux);

            public final Pip fb      = pip(zmux, reg);

            public final Pip center  = pip(zmux, reg);
            public final Pip xout    = pip(xlut, center);
            public final Pip yout    = pip(ylut, center);

            public final Pip oe      = pip(one, h[4], v[4]);
            public final Buf out     = buf(fb, oe);

            public final Pip[] o     = pip5(out, s);
            public final Pip[] h     = pip5(h, s);
            public final Pip[] v     = pip5(v, s);
        }


    }


    public class At94k extends At40k {
    }
        */

