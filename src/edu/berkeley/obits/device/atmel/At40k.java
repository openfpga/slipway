package edu.berkeley.obits.device.atmel;
import static edu.berkeley.obits.device.atmel.AtmelDevice.Constants.*;
import edu.berkeley.obits.*;
import org.ibex.util.*;
import java.util.*;
import java.io.*;

public class At40k {

    /*private*/public final AtmelDevice dev;
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

    public final class Sector {
        public final int col;
        public final int row;
        public Sector(Cell c) { this((c.col/4)*4, (c.row/4)*4); }
        private Sector(int col, int row) {
            if (row % 4 != 0) throw new Error("Sector must be created with a multiple-of-4 row");
            if (col % 4 != 0) throw new Error("Sector must be created with a multiple-of-4 col");
            this.row = row;
            this.col = col;
        }
        public Sector north() { return row+4>=height ? null : new Sector(col, row+4); }
        public Sector south() { return row==0 ?        null : new Sector(col, row-4); }
        public Sector east()  { return col+4>=width ?  null : new Sector(col+4, row); }
        public Sector west()  { return col==0 ?        null : new Sector(col-4, row); }
        public Cell cell() { return At40k.this.cell(col, row); }
    }

    public final class SectorWire {
        public final boolean global;
        public final boolean horizontal;
        public final int     plane;
        public final int     row;
        public final int     col;
        public SectorWire(boolean horizontal, int plane, int col, int row) {
            this.horizontal=horizontal;
            this.global = false;
            this.plane=plane;
            this.col= horizontal ? (col & ~0x3) : col;
            this.row=!horizontal ? (row & ~0x3) : row;
        }
        private int z(int z)       { return (horizontal ? 0x30 : 0x20) | z; }
        public int code(boolean topleft) {
            switch(plane) {
                case 0: return z(6)+(topleft?0:1);
                case 1: return z(8)+(topleft?0:1);
                case 2: return z(2*(4-plane))+(topleft?0:1);
                case 3: return z(2*(4-plane))+(topleft?0:1);
                case 4: return z(2*(4-plane))+(topleft?0:1);
            }
            throw new Error();
        }

        private final int fine()   { return horizontal ? row : col; }
        public  final int coarse() { return horizontal ? col : row; }
        private int _row()  { return horizontal ? row          : ((row)>>2); }
        private int _col()  { return horizontal ? ((col)>>2) : col;          }

        public SectorWire west()  { return !horizontal ? null : col-4<0       ? null : new SectorWire(horizontal, plane, col-4, row); }
        public SectorWire east()  { return !horizontal ? null : col+4>=width  ? null : new SectorWire(horizontal, plane, col+4, row); }
        public SectorWire north() { return  horizontal ? null : row+4>=height ? null : new SectorWire(horizontal, plane, col,   row+4); }
        public SectorWire south() { return  horizontal ? null : row-4<0       ? null : new SectorWire(horizontal, plane, col,   row-4); }

        public String toString() {
            return
                (horizontal?(col+":"+(col+3)):(""+col))+","+
                (horizontal?(row+"")         :(row+":"+(row+3)))+
                "x"+plane;
        }

        /** returns the ZYX0 coordinate of the byte controlling the switchbox that allows <tt>w</tt> to drive this wire */
        public int switchbox(SectorWire w) {
            if (w.horizontal==horizontal) {
                if (w.plane!=plane) throw new Error();
                if (Math.abs(w.coarse()-coarse())!=4) throw new Error(w.coarse() + " -- " + coarse());
                boolean topleft = horizontal ? (w.coarse() < coarse()) : (w.coarse() > coarse());
                int col = _col() + (( horizontal && !topleft) ? 1 : 0);
                int row = _row() + ((!horizontal &&  topleft) ? 1 : 0);
                return (code(topleft) << 24) | (row<<16) | (col<<8);
            }
            throw new Error("not implemented");
        }

        public void drives(SectorWire w, boolean enable) {
            dev.mode4zyx(switchbox(w), enable?0x02:0x00, 0x07);
        }

        public boolean drives(SectorWire w) {
            int connect = (dev.mode4zyx(switchbox(w)) >> (global?3:0)) & 0x7;
            return (connect & 0x2)!=0;
        }
        public SectorWire driverRight() {
            System.out.println("checking " + Integer.toString(code(true), 16) + " " + Integer.toString(_row(), 16) + " " + Integer.toString(_col(), 16));
            int ret = dev.mode4(z(code(true)), _row(), _col());
            ret = (ret >> (global?3:0)) & 0x7;
            switch(ret) {
                case 0: return null;
                case 1: return null;  /* global wire on same side */
                case 2: return new SectorWire(horizontal, plane, horizontal?(col+4):col, horizontal?row:(row+4));
                case 4: return null;  /* global wire on other side */
                default: throw new Error("multiple drivers on " + this + "!");
            }
        }
    }
    /*    
    public final class SwitchBox {
        public final boolean h;
        public final int col;
        public final int row;
        public final int plane;
        public SwitchBox(boolean h, int col, int row, int plane) { this.h = h; this.col = col; this.row = row; this.plane = plane; }
        public SectorWire west(boolean global)  { return !h ? null : global ? null : new SectorWire(h, col-4, row,   plane); }
        public SectorWire east(boolean global)  { return !h ? null : global ? null : new SectorWire(h, col+4, row,   plane); }
        public SectorWire north(boolean global) { return !h ? null : global ? null : new SectorWire(h, col,   row-4, plane); }
        public SectorWire south(boolean global) { return !h ? null : global ? null : new SectorWire(h, col,   row+4, plane); }
    }
    */

    public Cell cell(int col, int row) {
        if (col<0) return null;
        if (row<0) return null;
        if (col>=width) return null;
        if (row>=height) return null;
        return new Cell(col, row);
    }

    public final class Cell {
        public final int col;
        public final int row;

        public SectorWire hwire(int plane)  { return new SectorWire(true, plane, col, row); }
        public SectorWire vwire(int plane)  { return new SectorWire(false, plane, col, row); }
        public Cell east() { return cell(col+1, row); }
        public Cell west() { return cell(col-1, row); }
        public Cell north() { return cell(col,   row+1); }
        public Cell south() { return cell(col,   row-1); }

        public Sector sector() { return new Sector(this); }
        public Cell(int col, int row) {
            this.row = row;
            this.col = col;
        }

        /* bit positions mean:  [MSB] zxy z_y zx_ z__ _xy __y _x_ ___ [LSB] */
        public void lut(int xlut, int ylut) { xlut(xlut); ylut(ylut); }
        public void xlut(int table)    { dev.mode4(7, row, col, (byte)(table & 0xff)); }
        public byte xlut()             { return (byte)(dev.mode4(7, row, col) & 0xff); }
        public String printXLut()      { return printLut(xlut(), "x", "y", "t"); }
        public String printXLutX()     { return printLut(xlut(), str(xi(), "x"), str(yi(), "y"), str(ti(), "t")); }

        public String str(int x, String def) {
            switch(x) {
                case NORTH: return "n";
                case SOUTH: return "s";
                case EAST:  return "e";
                case WEST:  return "w";
                case NW:    return "nw";
                case SE:    return "se";
                case NE:    return "ne";
                case SW:    return "sw";
                case FB:    return "fb";
                case L0:    return (hx(0)&&vx(0))?"HV0":hx(0)?"H0":vx(0)?"V0":"L0";
                case L1:    return (hx(1)&&vx(1))?"HV1":hx(1)?"H1":vx(1)?"V1":"L1";
                case L2:    return (hx(2)&&vx(2))?"HV2":hx(2)?"H2":vx(2)?"V2":"L2";
                case L3:    return (hx(3)&&vx(3))?"HV3":hx(3)?"H3":vx(3)?"V3":"L3";
                case L4:    return (hx(4)&&vx(4))?"HV4":hx(4)?"H4":vx(4)?"V4":"L4";
                default: return def;
            }
        }

        /* bit positions mean:  [MSB] zxy zx_ z_y z__ _xy _x_ __y ___ [LSB] */
        public void ylut(int table)    { dev.mode4(6, row, col, (byte)(table & 0xff)); }
        public byte ylut()             { return (byte)(dev.mode4(6, row, col) & 0xff); }
        public String printYLut()      { return printLut(ylut(), "y", "x", "t"); }
        public String printYLutX()     { return printLut(ylut(), str(yi(), "y"), str(xi(), "x"), str(ti(), "t")) + Integer.toString(ylut() & 0xff, 16); }

        public void ff_reset_value(boolean value) {
            //dev.mode4( /* FIXME WRONG!!! */, row, col, 3, !value); return;
        }

        public void out(int plane, boolean enable) {
            switch(plane) {
                case L0: dev.mode4(0x00, row, col, 2, enable); return;
                case L1: dev.mode4(0x00, row, col, 3, enable); return;
                case L2: dev.mode4(0x00, row, col, 5, enable); return;
                case L3: dev.mode4(0x00, row, col, 4, enable); return;
                case L4: dev.mode4(0x00, row, col, 1, enable); return;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public boolean out(int plane) {
            switch(plane) {
                case L0: return (dev.mode4(0x00, row, col) & (1<<2)) != 0;
                case L1: return (dev.mode4(0x00, row, col) & (1<<3)) != 0;
                case L2: return (dev.mode4(0x00, row, col) & (1<<5)) != 0;
                case L3: return (dev.mode4(0x00, row, col) & (1<<4)) != 0;
                case L4: return (dev.mode4(0x00, row, col) & (1<<1)) != 0;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void h(int plane, boolean enable) {
            switch(plane) {
                case 0: dev.mode4(0x08, row, col, 2, enable); return;
                case 1: dev.mode4(0x08, row, col, 0, enable); return;
                case 2: dev.mode4(0x08, row, col, 5, enable); return;
                case 3: dev.mode4(0x08, row, col, 6, enable); return;
                case 4: dev.mode4(0x00, row, col, 6, enable); return;
                default: throw new RuntimeException("invalid argument");
            }
        }
        
        public boolean hx(int plane) {
            switch(plane) {
                case 0: return (dev.mode4(0x08, row, col) & (1<<2)) != 0;
                case 1: return (dev.mode4(0x08, row, col) & (1<<0)) != 0;
                case 2: return (dev.mode4(0x08, row, col) & (1<<5)) != 0;
                case 3: return (dev.mode4(0x08, row, col) & (1<<6)) != 0;
                case 4: return (dev.mode4(0x00, row, col) & (1<<6)) != 0;
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
        
        public boolean vx(int plane) {
            switch(plane) {
                case 0: return (dev.mode4(0x08, row, col) & (1<<1)) != 0;
                case 1: return (dev.mode4(0x08, row, col) & (1<<3)) != 0;
                case 2: return (dev.mode4(0x08, row, col) & (1<<4)) != 0;
                case 3: return (dev.mode4(0x08, row, col) & (1<<7)) != 0;
                case 4: return (dev.mode4(0x00, row, col) & (1<<7)) != 0;
                default: throw new RuntimeException("invalid argument");
            }
        }
        
        public int ti() {
            switch(dev.mode4(1, row, col) & 0x30) {
                case 0x20: return zi();
                case 0x10: return FB;
                case 0x00: return wi();
                default: throw new Error("ack!");
            }
        }
        public void t(int code) {
            int result = 0;
            switch(code) {
                case TMUX_Z: throw new Error("not implemented, but should be possible");
                case TMUX_W_AND_Z: result = 0x20; break;

                case TMUX_FB: result = 0x04; break; /* I think this is actually W_AND_FB, sadly */
                case TMUX_W_AND_FB: result = 0x14; break;
                case TMUX_W: result = 0x00; break;
            }
            dev.mode4(1, row, col, result, 0x34);
        }
        public void t(boolean ignore_z_and_fb, boolean zm_drives_fb, boolean fb_drives_wm) {
            // still not totally satisfied...
            //     how to we distinguish between z&w vs z or w&fb vs fb? ==> you can't!  this is a false connection in my diagram
            //     what does it mean for both bits (0x30) to be set to 1?
            //if (fb && z) throw new RuntimeException("invalid combination");
            int result = 0;
            // ZM->FB = 0x04
            // FB->WM = 0x10
            // WZ->WM = 0x20
 
            // tff => w&z
            // fff => w
            // ttt => fb&w
            // ftt => fb&w
            // fft => fb&w

            // ttf => w&z
            // ftf => w
            // tft => fb&w
            if (ignore_z_and_fb) result |= 0x20;
            if (zm_drives_fb) result |= 0x04;
            if (fb_drives_wm) result |= 0x10;
            dev.mode4(1, row, col, result, 0x34);
        }

        public void c(int source) {
            switch(source) {
                case XLUT: dev.mode4(1, row, col, 0x00, 0xc0); break;
                case YLUT: dev.mode4(1, row, col, 0x40, 0xc0); break;
                case ZMUX: dev.mode4(1, row, col, 0x80, 0xc0); break;
                default:   throw new RuntimeException("Invalid Argument");
            }
        }
        public int c() {
            switch (dev.mode4(1, row, col) >> 6) {
                case 0x00: return XLUT;
                case 0x01: return YLUT;
                case 0x02: return ZMUX;
            }
            throw new Error();
        }
        public void b(boolean registered) { dev.mode4(1, row, col, 3, !registered); }
        public void f(boolean registered) { dev.mode4(1, row, col, 2, !registered); }
        public void xo(boolean center)    { dev.mode4(1, row, col, 1, center); }
        public void yo(boolean center)    { dev.mode4(1, row, col, 0, center); }
        public boolean b() { return (dev.mode4(1, row, col) >> 3)!=1; }
        public boolean f() { return (dev.mode4(1, row, col) >> 2)!=1; }
        public boolean x() { return (dev.mode4(1, row, col) >> 1)==1; }
        public boolean y() { return (dev.mode4(1, row, col) >> 0)==1; }

        public void oe(int source) {
            switch(source) {
                case NONE: dev.mode4(0x02, row, col, 0, 0x3); break;
                case H4:   dev.mode4(0x02, row, col, 1, 0x3); break;
                case V4:   dev.mode4(0x02, row, col, 2, 0x3); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public int xi() {
            // FIXME: can be multiple
            if ((dev.mode4(0x03, row, col) & (1<<4))!=0) return L3;
            switch(dev.mode4(0x05, row, col) & 0xff) {
                case 0x80: return SW;
                case (1<<6): return NE;
                case (1<<5): return SE;
                case (1<<4): return NW;
                case (1<<3): return L3;
                case (1<<2): return L2;
                case (1<<1): return L1;
                case (1<<0): return L0;
                case 0: return NONE;
                default: throw new Error();
            }
        }

        public void xi(int source) {
            switch(source) {
                case SW: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<7); break;
                case NE: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<6); break;
                case SE: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<5); break;
                case NW: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<4); break;
                case L4: dev.mode4(0x03, row, col, 4, true);  dev.mode4(0x05, row, col,    0); break;
                case L3: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<0); break;
                case L2: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<1); break;
                case L1: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<2); break;
                case L0: dev.mode4(0x03, row, col, 4, false); dev.mode4(0x05, row, col, 1<<3); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public int yi() {
            if ((dev.mode4(0x02, row, col) & (1<<6))!=0) return L4;
            switch(dev.mode4(0x04, row, col) & 0xff) {
                case 0x80: return NORTH;
                case (1<<5): return SOUTH;
                case (1<<6): return WEST;
                case (1<<4): return EAST;
                case (1<<3): return L3;
                case (1<<2): return L2;
                case (1<<1): return L1;
                case (1<<0): return L0;
                case 0: return NONE;
                default: throw new Error();
            }
        }

        public void yi(int source) {
            switch(source) {
                case NORTH: dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<7); break;
                case SOUTH: dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<5); break;
                case WEST:  dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<6); break;
                case EAST:  dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<4); break;
                case L4:    dev.mode4(0x02, row, col, 6, true);  dev.mode4(0x04, row, col,    0); break;
                case L3:    dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<0); break;
                case L2:    dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<1); break;
                case L1:    dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<2); break;
                case L0:    dev.mode4(0x02, row, col, 6, false); dev.mode4(0x04, row, col, 1<<3); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void wi(int source) {
            switch(source) {
                case L4:    dev.mode4(0x03, row, col, 1<<5, 0xEC); break;
                case L3:    dev.mode4(0x03, row, col, 1<<6, 0xEC); break;
                case L2:    dev.mode4(0x03, row, col, 1<<7, 0xEC); break;
                case L1:    dev.mode4(0x03, row, col, 1<<3, 0xEC); break;
                case L0:    dev.mode4(0x03, row, col, 1<<2, 0xEC); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public int wi() {
            int who = dev.mode4(0x03, row, col) & 0xff;
            switch(who) {
                case (1<<5): return L4;
                case (1<<6): return L3;
                case (1<<7): return L2;
                case (1<<3): return L1;
                case (1<<2): return L0;
                case (0):    return NONE;
                default: throw new RuntimeException("invalid argument: " + who);
            }
        }

        public void zi(int source) {
            switch(source) {
                case L4:    dev.mode4(0x02, row, col, 1<<7, 0xDB); break;
                case L3:    dev.mode4(0x02, row, col, 1<<5, 0xDB); break;
                case L2:    dev.mode4(0x02, row, col, 1<<4, 0xDB); break;
                case L1:    dev.mode4(0x02, row, col, 1<<3, 0xDB); break;
                case L0:    dev.mode4(0x02, row, col, 1<<2, 0xDB); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public int zi() {
            switch(dev.mode4(0x02, row, col) & 0xff) {
                case (1<<7): return L4;
                case (1<<5): return L3;
                case (1<<4): return L2;
                case (1<<3): return L1;
                case (1<<2): return L0;
                case 0:      return NONE;
                default: throw new RuntimeException("invalid argument");
            }
        }

    }

    public IOB iob_bot(int col, boolean primary)   { return new IOB(col, 0, primary, true); }
    public IOB iob_top(int col, boolean primary)   { return new IOB(col, 1, primary, true); }
    public IOB iob_left(int row, boolean primary)  { return new IOB(0, row, primary, false); }
    public IOB iob_right(int row, boolean primary) { return new IOB(1, row, primary, false); }
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
        public final boolean northsouth;
        public IOB(int col, int row, boolean primary, boolean northsouth) {
            this.col = col;
            this.row = row;
            this.northsouth = northsouth;
            this.primary = primary;
        }
        /*
        public String dump() {
            System.out.println("[ "+
                               (schmitt()?"schmitt ":"")+
                               (slew()==3?"fast ":slew()==2?"med ":slew()==1?"slow ":"slew-unknown ")+
                               (cr()?"cr ":"")+
                               (reg()?"reg ":"")+
                               
        }
        */
        public void enableOutput(int direction) {
            useoem(true);
            output(direction);
            pullnone();
            useoem(true);
            oem(ALWAYS_ON);
            oe(true);
            // note: east-side IOBs should have slew=med, others slew=fast
            slew((!northsouth && col==1) ? MEDIUM : FAST);
        }
        public void enableInput() {
            schmitt(true);
            pullnone();
        }

        public void    useoem(boolean use)  { dev.mode4(z(3), row, col, 6, use); }
        public boolean useoem()             { return (dev.mode4(z(3), row, col) & (1<<6))!=0; }
        public void    schmitt(boolean use) { dev.mode4(z(0), row, col, 7, use); }
        public boolean schmitt()            { return (dev.mode4(z(0), row, col) & (1<<7))!=0; }

        public void    slew(int slew) {
            switch(slew) {
                case FAST:   dev.mode4(z(0), row, col, 3<<5, 0x60); return;
                case MEDIUM: dev.mode4(z(0), row, col, 2<<5, 0x60); return;
                case SLOW:   dev.mode4(z(0), row, col, 1<<5, 0x60); return;
                default: throw new Error();
            }
        }

        public void    oem(int source) {
            switch(source) {
                case ALWAYS_ON:  dev.mode4(z(3), row, col, 1<<5, 0x3f); return;
                case ALWAYS_OFF: dev.mode4(z(3), row, col,    0, 0x3f); return;
                default: throw new Error();
            }
        }

        private int z(int code) { return (northsouth ? 0x70 : 0x60) | (primary ? 0x00 : 0x04) | (code & 0x7); }
        public void pullup()   { dev.mode4(z(0), row, col, 0x00<<1, 0x06); }
        public void pulldown() { dev.mode4(z(0), row, col, 0x03<<1, 0x06); }
        public void pullnone() { dev.mode4(z(0), row, col, 0x01<<1, 0x06); }
        public void oe(boolean oe) {
            int old =  dev.mode4(z(1), row, col) & (~(1<<5));
            old     |= oe ? 0 : (1<<5);
            dev.mode4(z(1), row, col, old & 0xff);
        }

        public void output(int which) {
            switch(which) {
                case NONE:
                    dev.mode4(z(1), row, col, 0, 0x1f); return;
                case WEST: case EAST: case NORTH: case SOUTH:
                    dev.mode4(z(1), row, col, 1<<0, 0x1f); return;
                case NW: case SW: case NE: case SE:
                    dev.mode4(z(1), row, col, 1<<1, 0x1f); return;
                default: throw new Error();
            }
        }

    }

    public static void main(String[] s) throws Exception {
        System.out.println(printLut(0x39, "se", "n", "L0"));
    }
    public static synchronized String printLut(int lut, String xn, String yn, String zn) {
        try {
            File f = File.createTempFile("mvsis", ".mvs");
            f.deleteOnExit();

            FileOutputStream fos = new FileOutputStream(f);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
            pw.println(".model clb");
            pw.println(".inputs "+xn+" "+yn+" "+zn);
            pw.println(".outputs O");
            pw.println(".table "+xn+" "+yn+" "+zn+/*("X_xor_Y X_xor_Z Y_xor_Z")+*/ " -> O");
            for(int i=8; i>=0; i--) {
                int x = ((i & 0x01)!=0 ? 1 : 0);
                int y = ((i & 0x02)!=0 ? 1 : 0);
                int z = ((i & 0x04)!=0 ? 1 : 0);
                pw.print(" "+x+" ");
                pw.print(" "+y+" ");
                pw.print(" "+z+" ");
                //pw.print(" "+(x ^ y)+" ");
                //pw.print(" "+(y ^ z)+" ");
                //pw.print(" "+(z ^ y)+" ");
                pw.print((lut & (1<<i))==0 ? 0 : 1);
                pw.println();
            }
            pw.println(".end");
            pw.flush();
            pw.close();
            Process p = Runtime.getRuntime().exec(new String[] { "mvsis", "-c", "simplify;print_factor", f.getAbsolutePath() });
            new Gobble("mvsis: ", p.getErrorStream()).start();
            //new Gobble("mvsis: ", p.getInputStream()).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String ret = br.readLine();
            //f.delete();
            return ret.trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "*mvsis_error*";
        }
    }

    public static class Gobble extends Thread {
        private final String header;
        private final BufferedReader br;
        public Gobble(String header, BufferedReader br) { this.br = br; this.header = header; }
        public Gobble(String header, Reader r)          { this(header, new BufferedReader(r)); }
        public Gobble(String header, InputStream is)    { this(header, new InputStreamReader(is)); }
        public void run() {
            try {
                for(String s = br.readLine(); s!=null; s=br.readLine())
                    System.err.println(header + s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
