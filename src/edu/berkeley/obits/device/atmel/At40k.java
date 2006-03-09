package edu.berkeley.obits.device.atmel;
import static edu.berkeley.obits.device.atmel.AtmelDevice.Constants.*;
import edu.berkeley.obits.*;
import org.ibex.util.*;

public class At40k {

    private final AtmelDevice dev;
    private final int width;
    private final int height;

    public At40k(AtmelDevice dev, int width, int height) {
        this.width = width;
        this.height = height;
        this.dev = dev;
    }

    public static class At40k10 extends At40k {
        public At40k10(AtmelDevice dev) { super(dev, 24, 24); }
    }

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

        /* bit positions mean:  [MSB] ___ __y _x_ _xy z__ z_y zx_ zxy [LSB] */
        public void ylut(byte table)   { dev.mode4(6, row, col, ~table); }

        /* bit positions mean:  [MSB] ___ __y _x_ _xy z__ z_y zx_ zxy [LSB] */
        public void xlut(byte table)   { dev.mode4(7, row, col, ~table); }

        public void ff_reset_value(boolean value) {
            //dev.mode4( /* FIXME WRONG!!! */, row, col, 3, !value); return;
        }

        public void out(int plane, boolean enable) {
            switch(plane) {
                case 0: dev.mode4(0x00, row, col, 2, enable); return;
                case 1: dev.mode4(0x00, row, col, 3, enable); return;
                case 2: dev.mode4(0x00, row, col, 5, enable); return;
                case 3: dev.mode4(0x00, row, col, 4, enable); return;
                case 4: dev.mode4(0x00, row, col, 1, enable); return;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void h(int plane, boolean enable) {
            switch(plane) {
                case 0: dev.mode4(0x08, row, col, 0, enable); return;
                case 1: dev.mode4(0x08, row, col, 2, enable); return;
                case 2: dev.mode4(0x08, row, col, 5, enable); return;
                case 3: dev.mode4(0x08, row, col, 6, enable); return;
                case 4: dev.mode4(0x00, row, col, 6, enable); return;
                default: throw new RuntimeException("invalid argument");
            }
        }
        
        public void v(int plane, boolean enable) {
            switch(plane) {
                case 0: dev.mode4(0x08, row, col, 1, enable); return;
                case 1: dev.mode4(0x08, row, col, 3, enable); return;
                case 2: dev.mode4(0x08, row, col, 4, enable); return;
                case 3: dev.mode4(0x08, row, col, 7, enable); return;
                case 4: dev.mode4(0x00, row, col, 7, enable); return;
                default: throw new RuntimeException("invalid argument");
            }
        }
        
        public void t(boolean z, boolean w, boolean fb) {
            // still not totally satisfied...
            //     how to we distinguish between z&w vs z or w&fb vs fb?
            //     what does it mean for both bits (0x30) to be set to 1?
            if (fb && z) throw new RuntimeException("invalid combination");
            int result = 0;
            if (z)  result |= 0x20;
            if (fb) result |= 0x10;
            dev.mode4(1, row, col, result, 0x30);
        }

        public void c(int source) {
            switch(source) {
                case XLUT: dev.mode4(1, row, col, 0x00, 0xc0); break;
                case YLUT: dev.mode4(1, row, col, 0x40, 0xc0); break;
                case ZMUX: dev.mode4(1, row, col, 0x80, 0xc0); break;
                default:   throw new RuntimeException("Invalid Argument");
            }
        }

        public void b(boolean registered) { dev.mode4(1, row, col, 3, !registered); }
        public void f(boolean registered) { dev.mode4(1, row, col, 2, !registered); }

        public void xo(boolean center)    { dev.mode4(1, row, col, 1, center); }
        public void yo(boolean center)    { dev.mode4(1, row, col, 0, center); }

        public void oe(int source) {
            switch(source) {
                case NONE: dev.mode4(0x02, row, col, 0, 0x3); break;
                case H4:   dev.mode4(0x02, row, col, 1, 0x3); break;
                case V4:   dev.mode4(0x02, row, col, 2, 0x3); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void xin(int source) {
            switch(source) {
                case SW: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<7); break;
                case NE: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<6); break;
                case SE: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<5); break;
                case NW: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<4); break;
                case L4: dev.mode4(0x03, row, col, 4, true);  dev.mode4(0x05, row, col,    0); break;
                case L3: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<3); break;
                case L2: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<2); break;
                case L1: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<1); break;
                case L0: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<0); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        // FIXME: cancel out the others
        public void yin(int source) {
            switch(source) {
                case NORTH: dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<7); break;
                case SOUTH: dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<6); break;
                case WEST:  dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<5); break;
                case EAST:  dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<4); break;
                case L4:    dev.mode4(0x02, row, col, 6, true);  dev.mode4(0x04, row, col,    0); break;
                case L3:    dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<3); break;
                case L2:    dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<2); break;
                case L1:    dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<1); break;
                case L0:    dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<0); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void win(int source) {
            switch(source) {
                case L4:    dev.mode4(0x03, row, col, 1<<5, 0xEC); break;
                case L3:    dev.mode4(0x03, row, col, 1<<6, 0xEC); break;
                case L2:    dev.mode4(0x03, row, col, 1<<7, 0xEC); break;
                case L1:    dev.mode4(0x03, row, col, 1<<3, 0xEC); break;
                case L0:    dev.mode4(0x03, row, col, 1<<2, 0xEC); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void zin(int source) {
            switch(source) {
                case L4:    dev.mode4(0x02, row, col, 1<<7, 0xDB); break;
                case L3:    dev.mode4(0x02, row, col, 1<<5, 0xDB); break;
                case L2:    dev.mode4(0x02, row, col, 1<<4, 0xDB); break;
                case L1:    dev.mode4(0x02, row, col, 1<<3, 0xDB); break;
                case L0:    dev.mode4(0x02, row, col, 1<<2, 0xDB); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

    }

    public IOB iob_top(int col, boolean primary)   { return new IOB(col, 0, primary); }
    public IOB iob_bot(int col, boolean primary)   { return new IOB(col, 1, primary); }
    public IOB iob_left(int row, boolean primary)  { return new IOB(1, row, primary); }
    public IOB iob_right(int row, boolean primary) { return new IOB(2, row, primary); }
    /*
    public IOB fromPin(int pin) {
        if (pin >=  4 && pin <= 11) return io(pin-3);
        if (pin >= 15 && pin <= 18) return io(pin-2);
        if (pin >= 19 && pin <= 24) return io(pin);
        if (pin >= 27 && pin <= 30) return io(pin-2);
        if (pin >= 33 && pin <= 36) return io(pin);
        if (pin >= 38 && pin <= 47) return io(pin+1);


        if (pin >= 33 && pin <= 36) return io(pin+36);
        if (pin >= 38 && pin <= 41) return io(pin+43);
        if (pin >= 42 && pin <= 43) return io(pin+47);
        if (pin >= 44 && pin <= 47) return io(pin+49);
        if (pin >= 57 && pin <= 62) return io(pin+40);
        if (pin >= 63 && pin <= 66) return io(pin+46);
        if (pin >= 68 && pin <= 71) return io(pin+53);
        if (pin >= 72 && pin <= 73) return io(pin+53);
        if (pin >= 74 && pin <= 75) return io(pin+63);
        if (pin >= 76 && pin <= 77) return io(143+(pin-76));
        if (pin >= 80 && pin <= 81) return io(145+(pin-80));
        if (pin >= 82 && pin <= 85) return io(151+(pin-82));
        if (pin >= 86 && pin <= 89) return io(165+(pin-86));
        if (pin >= 91 && pin <= 94) return io(177+(pin-91));
        if (pin >= 95 && pin <= 96) return io(183+(pin-95));
        if (pin >= 97 && pin <= 100) return io(189+(pin-97));
        if (pin >= 161 && pin <= 164) return io(289+(pin-161));
        if (pin >= 165 && pin <= 166) return io(297+(pin-165));
        if (pin >= 167 && pin <= 168) return io(303+(pin-167));
        if (pin >= 169 && pin <= 170) return io(309+(pin-169));
        if (pin >= 172 && pin <= 173) return io(313+(pin-172));
        if (pin >= 174 && pin <= 175) return io(325+(pin-174));
        if (pin >= 176 && pin <= 179) return io(327+(pin-176));
        if (pin >= 180 && pin <= 181) return io(335+(pin-180));
        if (pin >= 184 && pin <= 185) return io(337+(pin-184));
        if (pin >= 186 && pin <= 191) return io(343+(pin-186));
        if (pin >= 192 && pin <= 193) return io(359+(pin-192));
        if (pin >= 195 && pin <= 196) return io(363+(pin-195));
        if (pin >= 197 && pin <= 200) return io(369+(pin-197));
        if (pin >= 201 && pin <= 204) return io(381+(pin-201));
    }
    public io(int ionum) {
        if (ionum <= 94) {
            int cell = (94 - pin) / 2;
            boolean primary = cell * 2 == (94-pin);
        }
    }
    */
    public final class IOB {
        public final int col;
        public final int row;
        public final boolean primary;
        public IOB(int col, int row, boolean primary) { this.col = col; this.row = row; this.primary = primary; }

        private int z(int code) { return 0x60 | (primary ? 0x08 : 0x10) | code; }
        public void pullup()   { dev.mode4(z(0), row, col, (dev.mode4(z(0), row, col) & (~0x6)) | 0x00); }
        public void pulldown() { dev.mode4(z(0), row, col, (dev.mode4(z(0), row, col) & (~0x6)) | 0x03); }
        public void pullnone() { dev.mode4(z(0), row, col, (dev.mode4(z(0), row, col) & (~0x6)) | 0x01); }
        public void oe(boolean oe) {
            int old =  dev.mode4(z(1), row, col) & (~(1<<5));
            old     |= oe ? 0 : (1<<5);
            dev.mode4(z(1), row, col, old & 0xff);
        }
        public void output(int which) {
            if (which < 0 || which > 6) throw new RuntimeException("oem(x) only valid for 0<=x<=5");
            int d = dev.mode4(z(1), row, col) & 0x80;
            if (which>0) { d |= 1 << (which==6 ? 6 : which==0 ? 5 : (which-1)); }
            Log.warn("z", Integer.toString(z(1),16));
            Log.warn("y", Integer.toString(row,16));
            Log.warn("x", Integer.toString(col,16));
            Log.warn("d", Integer.toString(d,16));
            dev.mode4(z(1), row, col, d);
        }
        
    }

}
