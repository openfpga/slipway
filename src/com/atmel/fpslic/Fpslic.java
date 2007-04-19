package com.atmel.fpslic;

import java.util.*;
import java.io.*;
import org.ibex.util.Log;
import static com.atmel.fpslic.FpslicConstants.*;

public abstract class Fpslic {

    public Fpslic(int width, int height) { this.width = width; this.height = height; }

    private int width;
    private int height;
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public abstract void flush();
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

    // Inner Classes ///////////////////////////////////////////////////////////////////////////////

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
        public Sector north() { return row+4>=getHeight() ? null : new Sector(col, row+4); }
        public Sector south() { return row==0 ?             null : new Sector(col, row-4); }
        public Sector east()  { return col+4>=getWidth() ?  null : new Sector(col+4, row); }
        public Sector west()  { return col==0 ?             null : new Sector(col-4, row); }
        public Cell cell()    { return Fpslic.this.cell(col, row); }
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
        public boolean isDriven() {
            // FIXME: bridging connections (horiz-to-vert)
            for(int i=0; i<4; i++)
                if (cell(horizontal ? col+i : col,
                         horizontal ? row   : row+i).out(plane)) return true;
            // FIXME: sector switchbox drivers
            return false;
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
        public SectorWire east()  { return !horizontal ? null : col+4>=getWidth()  ? null : new SectorWire(horizontal, plane, col+4, row); }
        public SectorWire north() { return  horizontal ? null : row+4>=getHeight() ? null : new SectorWire(horizontal, plane, col,   row+4); }
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
            mode4zyx(switchbox(w), enable?0x02:0x00, 0x07);
        }

        public boolean drives(SectorWire w) {
            int connect = (mode4zyx(switchbox(w)) >> (global?3:0)) & 0x7;
            return (connect & 0x2)!=0;
        }
        public SectorWire driverRight() {
            //System.out.println("checking " + Integer.toString(code(true), 16) + " " +
            //Integer.toString(_row(), 16) + " " + Integer.toString(_col(), 16));
            int ret = mode4(z(code(true)), _row(), _col());
            ret = (ret >> (global?3:0)) & 0x7;
            switch(ret) {
                case 0: return null;
                case 1: return null;  /* global wire on same side */
                case 2: return new SectorWire(horizontal, plane, horizontal?(col+4):col, horizontal?row:(row+4));
                case 4: return null;  /* global wire on other side */
                default: throw new Error("multiple drivers on " + this + "!");
            }
        }

        public boolean touches(Cell c) {
            return
                horizontal
                ? (c.row==_row() && (_col()/4 == c.col/4))
                : (c.col==_col() && (_row()/4 == c.row/4));
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
        if (col>=getWidth()) return null;
        if (row>=getHeight()) return null;
        return new Cell(col, row);
    }

    public final class Cell {
        public final int col;
        public final int row;

        public Cell(int col, int row) {
            this.row = row;
            this.col = col;
        }
        
        public Fpslic fpslic() { return Fpslic.this; }

        // Accessors for Neighbors //////////////////////////////////////////////////////////////////////////////

        public SectorWire hwire(int plane)  { return new SectorWire(true, plane, col, row); }
        public SectorWire vwire(int plane)  { return new SectorWire(false, plane, col, row); }
        public Cell east() { return cell(col+1, row); }
        public Cell west() { return cell(col-1, row); }
        public Cell north() { return cell(col,   row+1); }
        public Cell south() { return cell(col,   row-1); }
        public Cell ne() { return cell(col+1, row+1); }
        public Cell nw() { return cell(col-1, row+1); }
        public Cell se() { return cell(col+1, row-1); }
        public Cell sw() { return cell(col-1, row-1); }
        public Sector sector() { return new Sector(this); }

        /* bit positions mean:  [MSB] zxy z_y zx_ z__ _xy __y _x_ ___ [LSB] */
        public void lut(int xlut, int ylut) { xlut(xlut); ylut(ylut); }
        public void xlut(int table)    { mode4(7, row, col, (byte)(table & 0xff)); }
        public byte xlut()             { return (byte)(mode4(7, row, col) & 0xff); }
        public String printXLut()      { return FpslicUtil.printLut(xlut(), "x", "y", "t"); }
        public String printXLutX()     { return FpslicUtil.printLut(xlut(), str(xi(), "x"), str(yi(), "y"), str(ti_source(), "t")); }

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
        public void ylut(int table)    { mode4(6, row, col, (byte)(table & 0xff)); }
        public byte ylut()             { return (byte)(mode4(6, row, col) & 0xff); }
        public String printYLut()      { return FpslicUtil.printLut(ylut(), "y", "x", "t"); }
        public String printYLutX()     { return FpslicUtil.printLut(ylut(), str(yi(), "y"), str(xi(), "x"), str(ti_source(), "t")) + Integer.toString(ylut() & 0xff, 16); }

        public void ff_reset_value(boolean value) {
            //mode4( /* FIXME WRONG!!! */, row, col, 3, !value); return;
        }
        /** FIXME!!! */
        public boolean ff_reset_value() { return false; }
        public boolean columnClocked() {
            return false;
        }

        public void out(int plane, boolean enable) {
            switch(plane) {
                case L0: mode4(0x00, row, col, 2, enable); return;
                case L1: mode4(0x00, row, col, 3, enable); return;
                case L2: mode4(0x00, row, col, 5, enable); return;
                case L3: mode4(0x00, row, col, 4, enable); return;
                case L4: mode4(0x00, row, col, 1, enable); return;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public boolean out(int plane) {
            switch(plane) {
                case L0: return (mode4(0x00, row, col) & (1<<2)) != 0;
                case L1: return (mode4(0x00, row, col) & (1<<3)) != 0;
                case L2: return (mode4(0x00, row, col) & (1<<5)) != 0;
                case L3: return (mode4(0x00, row, col) & (1<<4)) != 0;
                case L4: return (mode4(0x00, row, col) & (1<<1)) != 0;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void h(int plane, boolean enable) {
            switch(plane) {
                case 0: mode4(0x08, row, col, 0, enable); return;
                case 1: mode4(0x08, row, col, 2, enable); return;
                case 2: mode4(0x08, row, col, 5, enable); return;
                case 3: mode4(0x08, row, col, 6, enable); return;
                case 4: mode4(0x00, row, col, 6, enable); return;
                default: throw new RuntimeException("invalid argument");
            }
        }
        
        public boolean hx(int plane) {
            switch(plane) {
                case 0: return (mode4(0x08, row, col) & (1<<0)) != 0;
                case 1: return (mode4(0x08, row, col) & (1<<2)) != 0;
                case 2: return (mode4(0x08, row, col) & (1<<5)) != 0;
                case 3: return (mode4(0x08, row, col) & (1<<6)) != 0;
                case 4: return (mode4(0x00, row, col) & (1<<6)) != 0;
                default: throw new RuntimeException("invalid argument");
            }
        }
        
        public void v(int plane, boolean enable) {
            switch(plane) {
                case 0: mode4(0x08, row, col, 1, enable); return;
                case 1: mode4(0x08, row, col, 3, enable); return;
                case 2: mode4(0x08, row, col, 4, enable); return;
                case 3: mode4(0x08, row, col, 7, enable); return;
                case 4: mode4(0x00, row, col, 7, enable); return;
                default: throw new RuntimeException("invalid argument");
            }
        }
        
        public boolean vx(int plane) {
            switch(plane) {
                case 0: return (mode4(0x08, row, col) & (1<<1)) != 0;
                case 1: return (mode4(0x08, row, col) & (1<<3)) != 0;
                case 2: return (mode4(0x08, row, col) & (1<<4)) != 0;
                case 3: return (mode4(0x08, row, col) & (1<<7)) != 0;
                case 4: return (mode4(0x00, row, col) & (1<<7)) != 0;
                default: throw new RuntimeException("invalid argument");
            }
        }
        

        public int ti_source() {
            switch(mode4(1, row, col) & 0x30) {
                case 0x20: return zi();
                case 0x10: return FB;
                case 0x00: return wi();
                default: throw new Error("ack!");
            }
        }

        public int t() {
            switch(mode4(1, row, col) & 0x30) {
                case 0x00: return TMUX_W;
                case 0x10: return wi()==NONE ? TMUX_FB : TMUX_W_AND_FB;
                case 0x20: return wi()==NONE ? TMUX_Z  : TMUX_W_AND_Z;
                case 0x30: throw new RuntimeException("illegal!");
                default: return TMUX_W; 
            }
        }

        public void t(int code) {
            int result = 0;
            switch(code) {
                case TMUX_W:        result = 0x00; break;
                case TMUX_Z:        result = 0x20; break;
                case TMUX_W_AND_Z:  result = 0x20; break;
                case TMUX_FB:       result = 0x10; break;
                case TMUX_W_AND_FB: result = 0x10; break;
                default: result = 0x00; break;
            }
            mode4(1, row, col, result, 0x30);
        }
        /*
        private void fmux(int source) {
            switch(source) {
                case ZMUX:      
                case FB:        
                case ALWAYS_ON: 
                default: throw new Error("unknown argument to fmux()");
            }
        }

        public boolean win_easable() {
        }
        */

        public int ti() {
            return mode4(1, row, col) & 0x34;
        }

        public void t(boolean ignore_z_and_fb, boolean zm_drives_fb, boolean fb_drives_wm) {
            // still not totally satisfied...
            //     need to find the bit that sets the w-mux off
            //     what does it mean for both bits (0x30) to be set to 1?
            //if (fb && z) throw new RuntimeException("invalid combination");
            int result = 0;
            // ZM->FB = 0x04
            // FB->WM = 0x10
            // WZ->WM = 0x20

            // tff => w&z      [0x20]
            // fff => w        [0x00]
            // ttt => fb&w     [0x34]
            // ftt => fb&w     [0x14]
            // fft => fb&w     [0x10]

            // ttf => w&z      [0x24]
            // ftf => w        [0x04]
            // tft => fb&w     [0x30]
            if (ignore_z_and_fb) result |= 0x20;
            if (zm_drives_fb) result |= 0x04;
            if (fb_drives_wm) result |= 0x10;
            mode4(1, row, col, result, 0x34);
        }


        public void c(int source) {
            switch(source) {
                case XLUT: mode4(1, row, col, 0x00, 0xc0); break;
                case YLUT: mode4(1, row, col, 0x40, 0xc0); break;
                case ZMUX: mode4(1, row, col, 0x80, 0xc0); break;
                default:   throw new RuntimeException("Invalid Argument");
            }
        }
        public int c() {
            int cval = mode4(1, row, col) & 0xc0;
            switch (cval) {
                case 0x00: return XLUT;
                case 0x40: return YLUT;
                case 0x80: return ZMUX;
            }
            throw new Error("c() => " + cval);
        }
        public void b(boolean registered) { mode4(1, row, col, 3, !registered); }
        public void f(boolean registered) { mode4(1, row, col, 2, !registered); }
        public boolean xo()               { return (mode4(1, row, col) & 0x01) != 0; }
        public boolean yo()               { return (mode4(1, row, col) & 0x02) != 0; }
        public void xo(boolean center)    { mode4(1, row, col, 0, center); }
        public void yo(boolean center)    { mode4(1, row, col, 1, center); }

        public void xo(Cell c) {
            if (c.row==row || c.col==col) {   // use the y-input
                xlut(LUT_OTHER);
                yi(c);
            } else {
                xlut(LUT_SELF);
                xi(c);
            }
            xo(false);
        }

        public void yo(Cell c) {
            if (!(c.row==row || c.col==col)) {   // use the x-input
                ylut(LUT_OTHER);
                xi(c);
            } else {
                ylut(LUT_SELF);
                yi(c);
            }
            yo(false);
        }

        public boolean b() { return (mode4(1, row, col) & (1 << 3)) == 0; }
        public boolean f() { return (mode4(1, row, col) & (1 << 2)) == 0; }
        public boolean x() { return (mode4(1, row, col) & (1 << 1)) != 0; }
        public boolean y() { return (mode4(1, row, col) & (1 << 0)) != 0; }

        public int oe() {
            switch (mode4(0x02, row, col) & 0x3) {
                case 0: return NONE;
                case 1: return H4;
                case 2: return V4;
                default: throw new RuntimeException("invalid argument");                    
            }
        }
        public void oe(int source) {
            switch(source) {
                case NONE: mode4(0x02, row, col, 0, 0x3); break;
                case H4:   mode4(0x02, row, col, 1, 0x3); break;
                case V4:   mode4(0x02, row, col, 2, 0x3); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public int xi() {
            // FIXME: can be multiple
            if ((mode4(0x03, row, col) & (1<<4))!=0) return L4;
            switch(mode4(0x05, row, col) & 0xff) {
                case 0x80: return SW;
                case (1<<6): return NE;
                case (1<<5): return SE;
                case (1<<4): return NW;
                case (1<<3): return L0;
                case (1<<2): return L1;
                case (1<<1): return L2;
                case (1<<0): return L3;
                case 0: return NONE;
                default: throw new Error();
            }
        }

        public void xi(int source) {
            switch(source) {
                case SW: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 1<<7); break;
                case NE: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 1<<6); break;
                case SE: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 1<<5); break;
                case NW: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 1<<4); break;

                case L0: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 1<<3); break;
                case L1: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 1<<2); break;
                case L2: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 1<<1); break;
                case L3: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 1<<0); break;
                case L4: mode4(0x03, row, col, 4, true);  mode4(0x05, row, col,    0); break;

                case NONE: mode4(0x03, row, col, 4, false); mode4(0x05, row, col, 0); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void xi(SectorWire sw) {
            if (!sw.touches(this)) throw new RuntimeException("invalid argument");
            xi(sw.plane);
        }

        public void xi(Cell c) {
            if      (c.row==row-1 && c.col==col-1) xi(SW);
            else if (c.row==row+1 && c.col==col-1) xi(NW);
            else if (c.row==row-1 && c.col==col+1) xi(SE);
            else if (c.row==row+1 && c.col==col+1) xi(NE);
            else throw new RuntimeException("invalid argument");
        }

        public int yi() {
            if ((mode4(0x02, row, col) & (1<<6))!=0) return L4;
            switch(mode4(0x04, row, col) & 0xff) {
                case (1<<7): return NORTH;
                case (1<<5): return SOUTH;
                case (1<<6): return WEST;
                case (1<<4): return EAST;
                case (1<<3): return L0;
                case (1<<2): return L1;
                case (1<<1): return L2;
                case (1<<0): return L3;
                case 0: return NONE;
                default: throw new Error();
            }
        }

        public void yi(int source) {
            switch(source) {
                case NORTH: mode4(0x02, row, col, 6, false); mode4(0x04, row, col, 1<<7); break;
                case SOUTH: mode4(0x02, row, col, 6, false); mode4(0x04, row, col, 1<<5); break;
                case WEST:  mode4(0x02, row, col, 6, false); mode4(0x04, row, col, 1<<6); break;
                case EAST:  mode4(0x02, row, col, 6, false); mode4(0x04, row, col, 1<<4); break;
                case L4:    mode4(0x02, row, col, 6, true);  mode4(0x04, row, col,    0); break;
                case L3:    mode4(0x02, row, col, 6, false); mode4(0x04, row, col, 1<<0); break;
                case L2:    mode4(0x02, row, col, 6, false); mode4(0x04, row, col, 1<<1); break;
                case L1:    mode4(0x02, row, col, 6, false); mode4(0x04, row, col, 1<<2); break;
                case L0:    mode4(0x02, row, col, 6, false); mode4(0x04, row, col, 1<<3); break;
                case NONE:  mode4(0x02, row, col, 6, false); mode4(0x04, row, col,    0); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public void yi(SectorWire sw) {
            if (!sw.touches(this)) throw new RuntimeException("invalid argument");
            yi(sw.plane);
        }

        public void yi(Cell c) {
            if      (c.row==row-1 && c.col==col)   yi(SOUTH);
            else if (c.row==row+1 && c.col==col)   yi(NORTH);
            else if (c.row==row   && c.col==col-1) yi(WEST);
            else if (c.row==row   && c.col==col+1) yi(EAST);
            else throw new RuntimeException("invalid argument");
        }

        public void wi(SectorWire sw) {
            if (!sw.touches(this)) throw new RuntimeException("invalid argument");
            wi(sw.plane);
        }

        public void wi(int source) {
            switch(source) {
                case L4:    mode4(0x03, row, col, 1<<5, 0xEC); break;
                case L3:    mode4(0x03, row, col, 1<<6, 0xEC); break;
                case L2:    mode4(0x03, row, col, 1<<7, 0xEC); break;
                case L1:    mode4(0x03, row, col, 1<<3, 0xEC); break;
                case L0:    mode4(0x03, row, col, 1<<2, 0xEC); break;
                case NONE:  mode4(0x03, row, col,    0, 0xEC); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public int wi() {
            int who = mode4(0x03, row, col) & 0xEC;
            switch(who) {
                case (1<<5): return L4;
                case (1<<6): return L3;
                case (1<<7): return L2;
                case (1<<3): return L1;
                case (1<<2): return L0;
                case (1<<0): return NONE;  /* huh? */
                case (0):    return NONE;
                default: throw new RuntimeException("invalid argument: " + who);
            }
        }

       
        public void zi(SectorWire sw) {
            if (!sw.touches(this)) throw new RuntimeException("invalid argument");
            zi(sw.plane);
        }

        public void zi(int source) {
            switch(source) {
                case L4:    mode4(0x02, row, col, 1<<7, 0xDB); break;
                case L3:    mode4(0x02, row, col, 1<<5, 0xDB); break;
                case L2:    mode4(0x02, row, col, 1<<4, 0xDB); break;
                case L1:    mode4(0x02, row, col, 1<<3, 0xDB); break;
                case L0:    mode4(0x02, row, col, 1<<2, 0xDB); break;
                case NONE:  mode4(0x02, row, col,    0, 0xDB); break;
                default: throw new RuntimeException("invalid argument");
            }
        }

        public int zi() {
            switch(mode4(0x02, row, col) & 0x9B) {
                case (1<<7): return L4;
                case (1<<5): return L3;
                case (1<<4): return L2;
                case (1<<3): return L1;
                case (1<<2): return L0;
                case (1<<1): return NONE;  /* huh? */
                case (1<<0): return NONE;  /* huh? */
                case 0:      return NONE;
                default: throw new RuntimeException("invalid argument: zi=="+(mode4(0x02, row, col) & 0x9B));
            }
        }

        public Cell dir(int i) {
            switch(i) {
                case NORTH: return north();
                case SOUTH: return south();
                case EAST: return east();
                case WEST: return west();
                case NW: return nw();
                case SW: return sw();
                case SE: return se();
                case NE: return ne();
            }
            return null;
        }
        public int dir(Cell c) {
            if      (c.row==row-1 && c.col==col-1) return SW;
            else if (c.row==row+1 && c.col==col-1) return NW;
            else if (c.row==row-1 && c.col==col+1) return SE;
            else if (c.row==row+1 && c.col==col+1) return NE;
            else if (c.row==row-1 && c.col==col)   return SOUTH;
            else if (c.row==row+1 && c.col==col)   return NORTH;
            else if (c.row==row   && c.col==col-1) return WEST;
            else if (c.row==row   && c.col==col+1) return EAST;
            return -1;
        }

        public void generalized_c_element() {

            /*
            ylut(LUT_SELF & (~LUT_OTHER));
            xlut((~LUT_SELF) | LUT_OTHER);
            c(ZMUX);
            zi(L2);
            //h(L2, true);
            out(L2, true);
            xo(true);
            yo(true);
            */

            //ylut(0xB2);
            ylut((LUT_SELF & ~LUT_OTHER) | (LUT_Z & ~LUT_OTHER) | (LUT_Z & LUT_SELF & LUT_OTHER));
            xlut(LUT_Z);
            c(YLUT);
            f(false);
            b(false);
            t(false, false, true);
            yo(false);
            xo(false);
        }


        // Relevance //////////////////////////////////////////////////////////////////////////////
        public boolean relevant() {
            return xo_relevant() || yo_relevant() || out_relevant();
        }

        public boolean xo_relevant() { return xo_relevant(NE) || xo_relevant(SE) || xo_relevant(NW) || xo_relevant(SW); }
        public boolean xo_relevant(int direction) {
            switch(direction) {
                case NE: return ne() != null && ne().xi()==SW;
                case NW: return nw() != null && nw().xi()==SE;
                case SE: return se() != null && se().xi()==NW;
                case SW: return sw() != null && sw().xi()==NE;
                default: return false;
            }
        }
        public boolean yo_relevant() { return yo_relevant(NORTH) || yo_relevant(SOUTH) || yo_relevant(EAST) || yo_relevant(WEST); }
        public boolean yo_relevant(int direction) {
            switch(direction) {
                case NORTH: return north() != null && north().yi()==SOUTH  /*&& north().yi_relevant()*/;
                case EAST: return east() != null  && east().yi()==WEST     /*&& east().yi_relevant()*/;
                case SOUTH: return south() != null && south().yi()==NORTH  /*&& south().yi_relevant()*/;
                case WEST: return west() != null  && west().yi()==EAST     /*&& west().yi_relevant()*/;
                default: return false;
            }
        }
        public boolean xi_relevant() { return xi_to_xlut_relevant() || xi_to_ylut_relevant(); }
        public boolean yi_relevant() { return yi_to_xlut_relevant() || yi_to_ylut_relevant(); }
        public boolean xi_to_ylut_relevant() { return (((ylut() & 0xcc) >> 2) != (ylut() & 0x33)); }
        public boolean yi_to_xlut_relevant() { return (((xlut() & 0xcc) >> 2) != (xlut() & 0x33)); }
        public boolean zi_to_xlut_relevant() { return (((xlut() & LUT_Z) >> 4) != (xlut() & LUT_Z)); }
        public boolean zi_to_ylut_relevant() { return (((ylut() & LUT_Z) >> 4) != (ylut() & LUT_Z)); }
        public boolean xi_to_xlut_relevant() { return (((xlut() & LUT_SELF) >> 1) != (xlut() & (LUT_SELF >> 1))); }
        public boolean yi_to_ylut_relevant() { return (((ylut() & LUT_SELF) >> 1) != (ylut() & (LUT_SELF >> 1))); }
        public boolean xlut_relevant() {
            if ((c()==XLUT || c()==ZMUX) && c_relevant()) return true;
            if (xo()) return false;
            return xo_relevant();
        }
        public boolean ylut_relevant() {
            if ((c()==YLUT || c()==ZMUX) && c_relevant()) return true;
            if (yo()) return false;
            return yo_relevant();
        }
        public boolean c_relevant() {
            switch(ti()) {
                case 0x34: return true;
                case 0x14: return true;
                case 0x10: return true;
                case 0x30: return true;
            }
            for(int i=0; i<5; i++)
                if (out(i))
                    return true;
            if (xo() || yo()) return true;
            return false;
        }

        public boolean register_relevant() {
            if (!c_relevant()) return false;
            if (f() && out_relevant()) return true;
            if (f() && fb_relevant()) return true;
            if (b() && xo()) return true;
            if (b() && yo()) return true;
            return false;
        }
        public boolean out_relevant() {
            boolean out = false;
            boolean connect = false;
            for(int i=0; i<4; i++) {

                // FIXME FIXME FIXME
                if (i==3) continue;

                if (out(L0+i)) out = true;
                if (hx(L0+i)) connect = true;
                if (vx(L0+i)) connect = true;
            }
            return out && connect;
        }

        public boolean fb_relevant() {
            /*
            if (!(zi_to_xlut_relevant()) ||
                !(zi_to_ylut_relevant())) return false;
            switch(ti()) {
                case 0x34: return true;
                case 0x14: return true;
                case 0x10: return true;
                case 0x30: return true;
            }
            return false;
            */
            return true;
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

        public void    useoem(boolean use)  { mode4(z(3), row, col, 6, use); }
        public boolean useoem()             { return (mode4(z(3), row, col) & (1<<6))!=0; }
        public void    schmitt(boolean use) { mode4(z(0), row, col, 7, use); }
        public boolean schmitt()            { return (mode4(z(0), row, col) & (1<<7))!=0; }

        public void    slew(int slew) {
            switch(slew) {
                case FAST:   mode4(z(0), row, col, 3<<5, 0x60); return;
                case MEDIUM: mode4(z(0), row, col, 2<<5, 0x60); return;
                case SLOW:   mode4(z(0), row, col, 1<<5, 0x60); return;
                default: throw new Error();
            }
        }

        public void    oem(int source) {
            switch(source) {
                case ALWAYS_ON:  mode4(z(3), row, col, 1<<5, 0x3f); return;
                case ALWAYS_OFF: mode4(z(3), row, col,    0, 0x3f); return;
                default: throw new Error();
            }
        }

        private int z(int code) { return (northsouth ? 0x70 : 0x60) | (primary ? 0x00 : 0x04) | (code & 0x7); }
        public void pullup()   { mode4(z(0), row, col, 0x00<<1, 0x06); }
        public void pulldown() { mode4(z(0), row, col, 0x03<<1, 0x06); }
        public void pullnone() { mode4(z(0), row, col, 0x01<<1, 0x06); }
        public void oe(boolean oe) {
            int old =  mode4(z(1), row, col) & (~(1<<5));
            old     |= oe ? 0 : (1<<5);
            mode4(z(1), row, col, old & 0xff);
        }

        public void output(int which) {
            switch(which) {
                case NONE:
                    mode4(z(1), row, col, 0, 0x1f); return;
                case WEST: case EAST: case NORTH: case SOUTH:
                    mode4(z(1), row, col, 1<<0, 0x1f); return;
                case NW: case SW: case NE: case SE:
                    mode4(z(1), row, col, 1<<1, 0x1f); return;
                default: throw new Error();
            }
        }

    }


}
