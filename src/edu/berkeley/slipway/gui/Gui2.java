package edu.berkeley.slipway.gui;

import com.atmel.fpslic.*;
import edu.berkeley.slipway.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static com.atmel.fpslic.FpslicUtil.*;
import edu.berkeley.slipway.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import static edu.berkeley.slipway.gui.GuiConstants.*;

public class Gui2 extends ZoomingPanel2 implements KeyListener, MouseMotionListener {

    Graphics2D g;
    G gg;

    Fpslic at40k;
    FtdiBoard drone;

    private Cell[][] ca = new Cell[128][];

    //public static final Color nonselectedcell = new Color(0x44, 0x44, 0x44);
    public static final Color nonselectedcell = new Color(0xee, 0xee, 0xee);
    public static final Color selectedcell    = new Color(0x00, 0x00, 0x00);

    private FtdiBoard ftdiboard;
    public Gui2(Fpslic at40k, FtdiBoard drone) {
        this(at40k, drone, 24, 24);
    }
    public Gui2(Fpslic at40k, FtdiBoard drone, int width, int height) {
        this.at40k = at40k;
        this.drone = drone;
        for(int i=0; i<ca.length; i++)
            ca[i] = new Cell[128];
        for(int x=0; x<width; x++)
            for(int y=0; y<height; y++)
                new Cell(x,y, at40k.cell(x, y));
    }

    public class Cell {
        Fpslic.Cell cell;
        boolean in = false;
        public boolean scanme = false;
        public boolean xon = false;
        public boolean yon = false;
        public boolean xknown = false;
        public boolean yknown = false;
        int _x, _y;
        public Cell(int x, int y, Fpslic.Cell cell) {
            _x = x;
            _y = y;
            ca[_x][_y] = this;
            this.cell = cell;
            cells.add(this);
        }
        public boolean scanme() { 
            return cell.relevant();
        }
        public void clear() {
            gg.color(0xffffff);
            //gg.color(in ? selectedcell : (scanme() ? new Color(0xbb, 0xbb, 0xbb) : nonselectedcell));
            g.fillRect(0, 0, SIZE, SIZE);
        }
        public void draw() {

            if (cell.relevant() || scanme()) {
                drawWires();
                drawLocal();
                
                AffineTransform t = g.getTransform();
                
                drawBuffer();
                g.transform(rotateInnerTransform());
                drawMux();
                drawRegister();
                drawInternalRouting();
                g.setTransform(t);
                
                drawGates();
            }
            drawBorder();
        }
        public void drawBuffer() {
            /*
            if (!cell.out_relevant()) return;

            GeneralPath p = new GeneralPath();
            p.moveTo(24, 61);
            p.lineTo(22, 68);
            p.lineTo(29, 66);
            p.lineTo(24, 61);
            gg.color(cell.oe() == NONE ? Color.white : Color.green);
            g.fill(p);
            gg.color(Color.green);
            g.draw(p);

            gg.color(Color.magenta);
            if (cell.oe() == V4) {
                gg.line(24, 62, 16, 63);
            } else if (cell.oe() == H4) {
                gg.line(24, 67, 24, 76);
            }
            */
        }
        public void drawWires() {
            gg.color(MAGENTA);
            for(int i=0; i<5; i++)
                if (i!=3)
                if (cell.hwire(i).isDriven()) {
                    gg.color(cell.out(i) ? ORANGE : MAGENTA);
                    gg.line(0, SIZE-(2*(1+RINGS)+2*i), SIZE, SIZE-(2*(1+RINGS)+2*i));
                }
            for(int i=0; i<5; i++)
                if (i!=3)
                if (cell.vwire(i).isDriven()) {
                    gg.color(cell.out(i) ? ORANGE : MAGENTA);
                    gg.line(2*(1+RINGS)+2*i, 0, 2*(1+RINGS)+2*i, SIZE);
                }
            if (cell.zi_to_xlut_relevant()) wire(cell.zi(), true);
            if (cell.zi_to_ylut_relevant()) wire(cell.zi(), false);
            if (cell.zi_to_xlut_relevant()) wire(cell.wi(), true);
            if (cell.zi_to_ylut_relevant()) wire(cell.wi(), false);
        }

        public void wire(int plane, boolean xlut) {
            if (!(plane >= L0 && plane <= L4 && cell.vx(plane))) return;
            if (xlut ? xlut_relevant(cell) : cell.ylut_relevant())
                route(new P(17 - 2*(plane-L0), 8),
                      rotateInner(xlut ? new P(38, 18) : new P(64, 18)),
                      3);
        }

        public P corner(int corner, int size, int ring) {
            switch(corner) {
                case NW: return new P(0    +2*ring,  size -2*ring);
                case SW: return new P(0    +2*ring,  0    +2*ring);
                case NE: return new P(size -2*ring,  size -2*ring);
                case SE: return new P(size -2*ring,  0    +2*ring);
                default: return null;
            }
        }

        public void drawInternalRouting() {
            gg.color(ORANGE);
            /*
            if (cell.fb_relevant()) {
                if (cell.f()) {
                    gg.line(51, 74, 37, 74);
                    gg.line(37, 74, 51, 12);
                } else if (cell.c() == XLUT) {
                    gg.color(LIGHTRED);
                    gg.line(33, 52, 51, 52);
                    gg.line(51, 52, 51, 12);
                } else if (cell.c() == YLUT) {
                    gg.color(LIGHTBLUE);
                    gg.line(67, 52, 51, 52);
                    gg.line(51, 52, 51, 12);
                } else {
                    gg.line(51, 56, 41, 56);
                    gg.line(41, 56, 51, 12);
                }
                if (xlut_relevant(cell)) {
                    gg.line(52, 12, XLUT_OUTPUT_POINT.getX(), 12);
                    gg.line(XLUT_OUTPUT_POINT.getX(), 12, XLUT_OUTPUT_POINT.getX(), 32);
                }
                if (cell.ylut_relevant()) {
                    gg.line(52, 12, YLUT_OUTPUT_POINT.getX(), 12);
                    gg.line(YLUT_OUTPUT_POINT.getX(), 12, YLUT_OUTPUT_POINT.getX(), 32);
                }
            }
            */
        }

        public void drawLocal() {
            if (!cell.ylut_relevant() && !xlut_relevant(cell)) return;

            P in = rotateOuter(new P(HOFF, 0));
            P join = rotateInner(new P(HOFF, CORE_OFFSET));
            int rot = rot();
            switch(rot) {
                case 0: case 2:
                    join = new P(in.getX(), join.getY());
                    break;
                case 1: case 3:
                    join = new P(join.getX(), in.getY());
                    break;
            }

            // x-input to cell
            gg.color(RED);
            P xi  = corner(cell.xi(), SIZE, 4);
            if (((cell.xlut_relevant() && cell.xi_to_xlut_relevant()) ||
                (cell.ylut_relevant() && cell.xi_to_ylut_relevant()))
                && (cell.xi() != NONE) && (cell.xi() < L0 || cell.xi() > L4)
                ) {
                P xi2 = corner(cell.xi(), SIZE + 2*BEVEL, -1).translate(-BEVEL, -BEVEL);
                switch(cell.xi()) {
                    case NW: case NE:
                        xi = translate(xi, 0, -3);
                        xi2 = translate(xi2, 0, -3);
                        break;
                    case SW: case SE:
                        xi = translate(xi, 0, 3);
                        xi2 = translate(xi2, 0, 3);
                        break;
                }
                gg.line(xi2, xi);
            }

            if (xlut_relevant(cell)) {

                if (cell.xi_to_xlut_relevant() && xi != null)
                    route(xi, rotateInner(new P(SIZE - CORE_OFFSET - CORE_SIZE/2 - CORE_SIZE / 3, 20)), 4);

                // xlut y-input
                gg.color(BLUE);
                if (cell.yi_to_xlut_relevant())
                    route(in, rotateInner(new P(SIZE - CORE_OFFSET - CORE_SIZE/2 - CORE_SIZE / 6, 20)), 5);
                /*
                // xlut output
                int xring = 4;
                gg.color(cell.xo() ? ORANGE : LIGHTRED);
                P xout = rotateInner(new P(SIZE-CORE_OFFSET-CORE_SIZE+17 - 2, CORE_OFFSET + CORE_SIZE - 3));
                if (cell.xo()) {
                    xout = rotateInner(new P(51, 74));
                    gg.line(rotateInner(new P(51, 62)), xout);
                } else if (cell.xo_relevant()) {
                    gg.line(rotateInner(XLUT_OUTPUT_POINT), xout);
                }
                if (cell.xo_relevant(NE)) {
                    gg.line(corner(NE, SIZE, xring).translate(0, 3), corner(NE, SIZE, 0).translate(0, 3));
                    route(xout, corner(NE, SIZE, xring).translate(0, 3), xring);
                }
                if (cell.xo_relevant(NW)) {
                    gg.line(corner(NW, SIZE, xring).translate(0, 3),  corner(NW, SIZE, 0).translate(0, 3));
                    route(xout, corner(NW, SIZE, xring).translate(0, 3),  xring);
                }
                if (cell.xo_relevant(SE)) {
                    gg.line(corner(SE, SIZE, xring).translate(0, -3), corner(SE, SIZE, 0).translate(0, -3));
                    route(xout, corner(SE, SIZE, xring).translate(0, -3), xring);
                }
                if (cell.xo_relevant(SW)) {
                    gg.line(corner(SW, SIZE, xring).translate(0, -3),  corner(SW, SIZE, 0).translate(0, -3));
                    route(xout, corner(SW, SIZE, xring).translate(0, -3),  xring);
                }
                */
            }

            if (cell.ylut_relevant()) {

                // ylut y-input
                gg.color(BLUE);

                if (cell.yi_to_ylut_relevant())
                    route(in, rotateInner(new P(SIZE - CORE_OFFSET - CORE_SIZE/2 + CORE_SIZE / 6, 20)), 5);

                // ylut x-input
                gg.color(RED);
                if (xi != null && cell.xi_to_ylut_relevant())
                    route(xi, rotateInner(new P(SIZE - CORE_OFFSET - CORE_SIZE/2 + CORE_SIZE / 3, 20)), 4);

                /*
                // lines directly from the ylut output to the four neighbors
                gg.color(cell.yo() ? ORANGE : LIGHTBLUE);
                P yout = rotateInner(new P(SIZE-CORE_OFFSET-CORE_SIZE+51 - 2, CORE_OFFSET + CORE_SIZE - 3));
                if (cell.yo()) {
                    yout = rotateInner(new P(51, 74));
                    gg.line(rotateInner(new P(51, 62)), yout);
                } else if (cell.yo_relevant()) {
                    gg.line(rotateInner(YLUT_OUTPUT_POINT), yout);
                }
                if (cell.yo_relevant(NORTH)) route(yout, new P(SIZE-40, SIZE+ 0), 2);
                if (cell.yo_relevant(EAST))  route(yout, new P(SIZE+ 0,      40), 2);
                if (cell.yo_relevant(SOUTH)) route(yout, new P(     40,       0), 2);
                if (cell.yo_relevant(WEST))  route(yout, new P(      0, SIZE-40), 2);
                */
            }

        }

        private AffineTransform rotateOuterTransform() {
            int rot = rot();
            AffineTransform a = new AffineTransform();
            a.rotate((Math.PI/2) * rot);
            switch(rot) {
                case 0: break;
                case 1: a.translate(0,  -SIZE); break;
                case 2: a.translate(-SIZE, -SIZE); break;
                case 3: a.translate(-SIZE, 0); break;
            }
            return a;
        }

        private P rotateOuter(P p) { return p.transform(rotateOuterTransform()); }
        private P rotateInner(P p) { return p.transform(rotateInnerTransform()); }
        private P unRotateInner(P p) { return p.inverseTransform(rotateInnerTransform()); }

        private AffineTransform rotateInnerTransform() {
            int rot = rot();            
            AffineTransform a = new AffineTransform();
            a.translate(SIZE-CORE_SIZE-CORE_OFFSET, CORE_OFFSET);
            a.rotate((Math.PI/2) * rot);
            switch(rot) {
                case 0: break;
                case 1: a.translate(0,  -CORE_SIZE); break;
                case 2: a.translate(-CORE_SIZE, -CORE_SIZE); break;
                case 3: a.translate(-CORE_SIZE, 0); break;
            }
            a.translate(-1 * (SIZE-CORE_SIZE-CORE_OFFSET), -CORE_OFFSET);
            return a;
        }


        private P project(P p1, int ring) {
            double north = Math.abs( (SIZE-(ring*2)) - p1.getY() );
            double south = Math.abs( (     (ring*2)) - p1.getY() );
            double east  = Math.abs( (SIZE-(ring*2)) - p1.getX() );
            double west  = Math.abs( (     (ring*2)) - p1.getX() );
            if (north < south && north < east && north < west) return new P(p1.x,        SIZE-ring*2);
            else if (south < east && south < west)             return new P(p1.x,        ring*2);
            else if (east < west)                              return new P(SIZE-ring*2, p1.y);
            else                                               return new P(ring*2,      p1.y);

        }

        private void route(P p1, P p2, int ring) {
            int ringpos = ring * 2;
            P projected = project(p1, ring);
            gg.line(p1, projected);
            p1 = projected;

            projected = project(p2, ring);
            gg.line(p2, projected);
            p2 = projected;

            if (p1.x==p2.x || p1.y==p2.y) {
                gg.line(p1, p2);
                return;
            }

            if ((p1.x==SIZE-ring*2 || p1.x==ring*2) && !(p1.y==SIZE-ring*2 || p1.y==ring*2)) {
                P p3 = new P(p1.x, p2.y > SIZE/2 ? SIZE-ring*2 : ring*2);
                gg.line(p1, p3);
                route(p3, p2, ring);
            } else if ((p1.y==SIZE-ring*2 || p1.y==ring*2) && !(p1.x==SIZE-ring*2 || p1.x==ring*2)) {
                P p3 = new P(p2.x > SIZE/2 ? SIZE-ring*2 : ring*2, p1.y);
                gg.line(p1, p3);
                route(p3, p2, ring);
            } else
                route(p2, p1, ring);
        }
        
        private int rot() {
            int rot = 0;
            switch(cell.yi()) {
                case SOUTH: rot = 0; break;
                case NORTH: rot = 2; break;
                case EAST: rot = 1; break;
                case WEST: rot = 3; break;
                default: {
                    // FIXME: choose based on xin
                    if (cell.north() != null && cell.north().yi()==SOUTH) { rot = 0; break; }
                    if (cell.south() != null && cell.south().yi()==NORTH) { rot = 2; break; }
                    if (cell.east()  != null && cell.east().yi()==WEST) { rot = 3; break; }
                    if (cell.west()  != null && cell.west().yi()==EAST) { rot = 1; break; }
                }
            }
            return rot;
        }
        
        public void drawGates() {
            AffineTransform t = g.getTransform();
            try {
                g.translate(SIZE-CORE_SIZE-CORE_OFFSET, CORE_OFFSET);
                
                int rot = rot();
                g.rotate((Math.PI/2) * rot);
                switch(rot) {
                    case 0: break;
                    case 1: g.translate(0,  -CORE_SIZE); break;
                    case 2: g.translate(-CORE_SIZE, -CORE_SIZE); break;
                    case 3: g.translate(-CORE_SIZE, 0); break;
                }

                //gg.color(Color.gray);
                //g.drawRect(0, 0, CORE_SIZE, CORE_SIZE);
                //g.scale(1, -1);


                g.translate(2,   5f);
                if (xlut_relevant(cell) || scanme()) {
                    Gate gate = getGate(cell.xlut(), true);
                    gate.draw(g,
                              !xknown ? Color.gray : xon ? Color.red : Color.white,
                              (xon && xknown) ? Color.white : Color.red,
                              xon ? Color.white : Color.red
                              );
                }

                g.translate(34f, 0f);
                if (cell.ylut_relevant() || scanme()) {
                    Gate gate = getGate(cell.ylut(), false);
                    gate.draw(g,
                              !yknown ? Color.gray : yon ? Color.blue : Color.white,
                              (yon && yknown) ? Color.white : Color.blue,
                              yon ? Color.white : Color.blue
                              );
                }

            } finally {
                g.setTransform(t);
            }
        }
        public void drawMux() {
            if (!cell.c_relevant()) return;
            gg.color(Color.black);
            if (xlut_relevant(cell) && (cell.c() == ZMUX || cell.c() == XLUT)) {
                gg.color(LIGHTRED);
                gg.line(XLUT_OUTPUT_POINT, new P(XLUT_OUTPUT_POINT.getX(), 52));
                gg.line(new P(XLUT_OUTPUT_POINT.getX(), 52), new P(51, 52));
            }
            if (cell.ylut_relevant() && (cell.c() == ZMUX || cell.c() == YLUT)) {
                gg.color(LIGHTBLUE);
                gg.line(YLUT_OUTPUT_POINT, new P(YLUT_OUTPUT_POINT.getX(), 52));
                gg.line(new P(YLUT_OUTPUT_POINT.getX(), 52), new P(51, 52));
            }
            if (cell.c() == ZMUX)
                gg.color(ORANGE);

            gg.line(51, 52, 51, 51+23);

            if (cell.register_relevant() && cell.f() && !cell.b()) {
                gg.line(51, 56, 60, 56);
                gg.line(60, 56, 60, 51+25);
            } else {
                gg.line(51, 51+23, 51, 51+25);
            }

            if (cell.c() == ZMUX) {

                gg.color(GREEN);
                int plane = cell.zi()+1;
                route(unRotateInner(new P(8 + 2*(plane-L0),  76 + 2*(plane-L0))),
                      new P(51, 20),
                      3);
                gg.line(new P(51, 20), new P(51, 50));

                GeneralPath p = new GeneralPath();
                p.moveTo(45, 50);
                p.lineTo(47, 54);
                p.lineTo(56, 54);
                p.lineTo(58, 50);
                p.lineTo(45, 50);
                gg.color(WHITE);
                gg.g.fill(p);

                gg.color(ORANGE);
                gg.g.draw(p);

            }
        }
        public int ccolor() {
            switch(cell.c()) {
                case XLUT: return LIGHTRED;
                case YLUT: return LIGHTBLUE;
                case ZMUX: return ORANGE;
            }
            return BLACK;
        }
        public void drawRegister() {
            if (!cell.register_relevant()) return;

            int dark = ccolor();
            gg.color(Color.white);
            g.fillRect(47, 58, 9, 14);
            gg.color(dark);
            g.drawRect(47, 58, 9, 14);

            GeneralPath p = new GeneralPath();
            p.moveTo(56, 70);
            p.lineTo(53, 68);
            p.lineTo(56, 66);
            p.lineTo(56, 70);
            gg.color(cell.ff_reset_value() ? WHITE : dark);
            g.fill(p);
            gg.color(dark);
            g.draw(p);
        }
        public void drawBorder() {
            gg.color(Color.gray);
            gg.line(BEVEL, 0, SIZE-BEVEL, 0);
            gg.line(SIZE, BEVEL, SIZE, SIZE-BEVEL);
            gg.line(SIZE-BEVEL, SIZE, BEVEL, SIZE);
            gg.line(0, SIZE-BEVEL, 0, BEVEL);
            /*            
            gg.color(0xdddddd);
            gg.line(0, BEVEL, BEVEL,    0);
            gg.line(SIZE-BEVEL, 0, SIZE, BEVEL);
            gg.line(SIZE, SIZE-BEVEL, SIZE-BEVEL, SIZE);
            gg.line(BEVEL, SIZE, 0, SIZE-BEVEL);
            */
        }
    }

    public void pressed() {
        dragFrom = oldcell;
    }

    public void released() {
        if (dragFrom == null || oldcell == null) return;
        if (Math.abs(dragFrom._y - oldcell._y) > 1) return;
        if (Math.abs(dragFrom._x - oldcell._x) > 1) return;
        if (dragFrom._x == oldcell._x   && dragFrom._y == oldcell._y+1) oldcell.cell.yi(NORTH);
        if (dragFrom._x == oldcell._x   && dragFrom._y == oldcell._y-1) oldcell.cell.yi(SOUTH);
        if (dragFrom._x == oldcell._x+1 && dragFrom._y == oldcell._y)   oldcell.cell.yi(EAST);
        if (dragFrom._x == oldcell._x-1 && dragFrom._y == oldcell._y)   oldcell.cell.yi(WEST);
        if (dragFrom._x == oldcell._x+1 && dragFrom._y == oldcell._y+1) oldcell.cell.xi(NE);
        if (dragFrom._x == oldcell._x+1 && dragFrom._y == oldcell._y-1) oldcell.cell.xi(SE);
        if (dragFrom._x == oldcell._x-1 && dragFrom._y == oldcell._y+1) oldcell.cell.xi(NW);
        if (dragFrom._x == oldcell._x-1 && dragFrom._y == oldcell._y-1) oldcell.cell.xi(SW);
        repaint();
    }

    public void drawKeyboard(Image keyboardImage, Graphics2D g) {
        int width = 300;
        int height = (keyboardImage.getHeight(null) * width) / keyboardImage.getWidth(null);
        g.drawImage(keyboardImage,
                    0, getHeight() - height,
                    width, getHeight(),
                    0, 0,
                    keyboardImage.getWidth(null), keyboardImage.getHeight(null),
                    null);
    }

    public void _paint(Graphics2D g) {

        this.g = g;
        this.gg = new G(g);
        g.setStroke(new BasicStroke((float)0.5));

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);

        AffineTransform t  = g.getTransform();
        for(Cell c : cells) {
            g.setTransform(t);
            g.translate(     c._x * SIZE /*+ (10 * (c._x/4))*/,      c._y * SIZE /*+ (10 * (c._y/4))*/);
            c.clear();
        }
        for(Cell c : cells) {
            g.setTransform(t);
            g.translate(     c._x * SIZE /*+ (10 * (c._x/4))*/,      c._y * SIZE /*+ (10 * (c._y/4))*/);
            c.draw();
        }
        g.setTransform(t);

        g.setTransform(new AffineTransform());

        gg.color(selectedcell);
        g.fillRect(getWidth() - 200, 0, 200, 600);
        gg.color(Color.white);
        g.drawRect(getWidth() - 200, 0, 200, 600);

        Cell newcell = whichCell(mousex, mousey);
        int line = 10;
        g.setFont(new Font("monospaced", 0, 14));
        if (newcell != null && newcell.cell != null) {
            g.drawString("selected: " + newcell._x + "," + newcell._y, getWidth() - 200 + 10, (line += 15));
            g.drawString("    xlut: " + XLUT_EQUATIONS[newcell.cell.xlut() & 0xff], getWidth() - 200 + 10, (line += 15));
            g.drawString("    ylut: " + YLUT_EQUATIONS[newcell.cell.ylut() & 0xff], getWidth() - 200 + 10, (line += 15));
            String xi = "??";
            switch(newcell.cell.xi()) {
                case NW : xi = "NW"; break;
                case NE : xi = "NE"; break;
                case SW : xi = "SW"; break;
                case SE : xi = "SE"; break;
                case NONE  : xi = "."; break;
                default:  xi = "L"+(newcell.cell.xi()-L0); break;
            }
            g.drawString("x-in mux: " + xi, getWidth() - 200 + 10, (line += 15));

            String yi = "??";
            switch(newcell.cell.yi()) {
                case NORTH : yi = "NORTH"; break;
                case SOUTH : yi = "SOUTH"; break;
                case EAST  : yi = "EAST"; break;
                case WEST  : yi = "WEST"; break;
                case NONE  : yi = "."; break;
                default:     yi = "L"+(newcell.cell.yi()-L0); break;
            }
            g.drawString("y-in mux: " + yi, getWidth() - 200 + 10, (line += 15));

            g.drawString("w-in mux: " + (newcell.cell.wi()==NONE ? "." : ("L"+(newcell.cell.wi()-L0))),
                         getWidth() - 200 + 10, (line += 15));
            g.drawString("z-in mux: " + (newcell.cell.zi()==NONE ? "." : ("L"+(newcell.cell.zi()-L0))),
                         getWidth() - 200 + 10, (line += 15));

            String tm = "??";
            switch(newcell.cell.t()) {
                case TMUX_FB:       tm = "fb"; break;
                case TMUX_W_AND_FB: tm = "w&fb"; break;
                case TMUX_Z:        tm = "z"; break;
                case TMUX_W_AND_Z:  tm = "w&z"; break;
                case TMUX_W:        tm = "w"; break;
            }
            g.drawString("t-in mux: " + tm, getWidth() - 200 + 10, (line += 15));

            g.drawString(" set/rst: " + (newcell.cell.ff_reset_value() ? "reset=SET" : "."),
                         getWidth() - 200 + 10, (line += 15));

            String outs = "";
            for(int i=0; i<5; i++) outs += (newcell.cell.out(L0+i) ? (i+" ") : ". ");
            g.drawString("     out: " + outs,
                         getWidth() - 200 + 10, (line += 15));
            String hs = "";
            for(int i=0; i<5; i++) hs += (newcell.cell.hx(L0+i) ? (i+" ") : ". ");
            g.drawString("  h conn: " + hs,
                         getWidth() - 200 + 10, (line += 15));
            String vs = "";
            for(int i=0; i<5; i++) vs += (newcell.cell.vx(L0+i) ? (i+" ") : ". ");
            g.drawString("  v conn: " + vs,
                         getWidth() - 200 + 10, (line += 15));
            g.drawString("out enab: " + (newcell.cell.oe()==H4 ? "H4" : newcell.cell.oe()==V4 ? "V4" : "."),
                         getWidth() - 200 + 10, (line += 15));
            g.drawString("   c-mux: " + (newcell.cell.c()==ZMUX ? "zmux" : newcell.cell.c()==XLUT ? "x-lut" : "y-lut"),
                         getWidth() - 200 + 10, (line += 15));
            g.drawString(" fb src: " + (newcell.cell.f() ? "clocked" : "."),
                         getWidth() - 200 + 10, (line += 15));
            g.drawString("  bypass: " + (newcell.cell.b() ? "clocked" : "."),
                         getWidth() - 200 + 10, (line += 15));
            g.drawString("   x out: " + (newcell.cell.xo() ? (newcell.cell.b() ? "register" : "center") : "."),
                         getWidth() - 200 + 10, (line += 15));
            g.drawString("   y out: " + (newcell.cell.yo() ? (newcell.cell.b() ? "register" : "center") : "."),
                         getWidth() - 200 + 10, (line += 15));

        }
        if (shiftkey) {
            drawKeyboard(keyboard2, g);
        } else switch(lastChar) {
            case 'x':
            case 'y':
            case 'z':
            case 'w':
            case 'o':
            case 'h':
            case 'v':
                drawKeyboard(keyboard3, g);
                break;
            default:
                drawKeyboard(keyboard1, g);
                break;
        }

        while (mousebutton) {

            if (dragFrom == null || oldcell == null) break;
            if (Math.abs(dragFrom._y - oldcell._y) > 1) break;
            if (Math.abs(dragFrom._x - oldcell._x) > 1) break;
            g.setTransform(t);
            if (dragFrom._x == oldcell._x || dragFrom._y == oldcell._y)
                gg.color(BLUE);
            else
                gg.color(RED);

            gg.line( oldcell._x * SIZE + SIZE/2,
                     oldcell._y * SIZE + SIZE/2,
                     dragFrom._x * SIZE + SIZE/2,
                     dragFrom._y * SIZE + SIZE/2, 5);
            break;
        }

        this.g = null;
        this.gg = null;
    }
    Cell dragFrom = null;

    public void clear() {
        Graphics2D g = (Graphics2D)getGraphics();
        //gg.color(Color.black);
        //gg.color(Color.lightGray);
        g.clearRect(0, 0, getWidth(), getHeight());
    }

    public static final P translate(P p, int dx, int dy) {
        return new P(p.getX()+dx, p.getY()+dy);
    }

    public Cell whichCell(int x, int y) {
        P p = new P(x,y);
        try {
            p = p.inverseTransform(transform);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int col = ((int)p.getX()+0) / SIZE;
        int row = ((int)p.getY()+0) / SIZE;
        for(Cell c : cells)
            if (c._x == col && c._y == row)
                return c;
        return null;
    }

    public static boolean xlut_relevant(Fpslic.Cell c) {
        return c.xlut_relevant();
    }

    public void mouseClicked(MouseEvent e) {
        final Cell c = whichCell(e.getX(), e.getY());
        if (c==null) return;
    }


    int made = 0;
    private class BCB extends FtdiBoard.ByteCallback {
        Gui2.Cell c;
        int who;
        public BCB(Gui2.Cell c, int who) {
            this.who = who; this.c = c;
            made++;
        }
        public void call(byte b) throws Exception {
            boolean on = (b & 0x80) != 0;
            switch(who) {
                case YLUT:
                    c.yknown = true;
                    c.yon = on;
                    repaint();
                    break;
                case XLUT:
                    c.xknown = true;
                    c.xon = on;
                    repaint();
                    break;
            }
            made--;
        }
    }

    public Gate getGate(byte lut, boolean xlut) {
        for(Gate g : knownGates)
            if (g.setLut(lut, xlut))
                return g;
        return unknownGate;
    }

    public Gate unknownGate = new Circle("?");
    public Gate[] knownGates =
        new Gate[] {
            new And(),
            new Or(),
            new Circle("0") { public boolean result(boolean x, boolean y, boolean z) { return false; } },
            new Circle("1") { public boolean result(boolean x, boolean y, boolean z) { return true; } },
            new Circle("x") { public boolean result(boolean x, boolean y, boolean z) { return x; } },
            new Circle("y") { public boolean result(boolean x, boolean y, boolean z) { return y; } },
            new Circle("z") { public boolean result(boolean x, boolean y, boolean z) { return z; } },
            new Circle("~x") { public boolean result(boolean x, boolean y, boolean z) { return !x; } },
            new Circle("~y") { public boolean result(boolean x, boolean y, boolean z) { return !y; } },
            new Circle("~z") { public boolean result(boolean x, boolean y, boolean z) { return !z; } }
        };

    // FIXME: 2-input gates?
    public abstract class Gate {
        public boolean invert_x;
        public boolean invert_y;
        public boolean invert_z;
        public boolean invert_out;
        public abstract boolean result(boolean x, boolean y, boolean z);
        public void draw(Graphics2D g, Color fill, Color stroke, Color text) {
            GeneralPath p = new GeneralPath();
            makePath(p);
            g.setColor(fill);
            g.fill(p);
            g.setColor(stroke);
            g.draw(p);

            AffineTransform a = g.getTransform();
            g.scale(1, -1);
            if (label() != null) {
                g.setColor(text);
                g.drawString(label(), 7, -14);
            }
            g.setTransform(a);
        }
        public String label() { return null; }
        public boolean setLut(int lut, boolean xlut) {
            /*
            for(int inverts = 0; inverts < 16; inverts++) {
                invert_x   = (inverts & 0x1) != 0;
                invert_y   = (inverts & 0x2) != 0;
                invert_z   = (inverts & 0x4) != 0;
                invert_out = (inverts & 0x8) != 0;
            */
                boolean good = true;
                for(int bit=0; bit<8; bit++) {
                    boolean x = xlut ? ((bit & 0x1) != 0) : ((bit & 0x2) != 0);
                    boolean y = xlut ? ((bit & 0x2) != 0) : ((bit & 0x1) != 0);
                    boolean z = (bit & 0x4) != 0;
                    boolean expect = (lut & (1<<bit)) != 0;

                    // FIXME symmetry issues here....
                    boolean result = result(x ^ invert_x, y ^ invert_y, z ^ invert_z) ^ invert_out;
                    if (result != expect) { good = false; break; }
                }
                if (good) return true;
                /*
            }
                */
            return false;
        }
        public abstract void makePath(GeneralPath gp);
    }

    public class Or extends Gate {
        public boolean result(boolean x, boolean y, boolean z) { return x || y || z; }
        public String label() { return "+"; }
        public void draw(Graphics2D g, Color fill, Color stroke, Color text) {
            AffineTransform at = g.getTransform();
            g.scale(1, -1);
            g.translate(0, -40);
            super.draw(g, fill, stroke, text);
            g.setTransform(at);
        }
        public void makePath(GeneralPath gp) {
            gp.moveTo(29.141f, 36.301f);
            gp.lineTo(29.141f, 36.301f-7.161f);
            gp.curveTo(27.71f, 11.24f, 23.413f, 9.45f, 14.82f, 0.5f);
            gp.curveTo(6.229f, 9.45f, 1.932f, 11.24f, 0.5f, 29.141f);
            gp.lineTo(0.5f, 29.141f+7.161f);
            float x = 0.5f;
            float y = 29.141f+7.161f;
            gp.curveTo(5.729f+x, -1.789f+y,
                       6.444f+x, -2.686f+y,
                       14.32f+x, -3.58f+y);
            gp.curveTo(22.697f, 33.616f, 23.413f, 34.512f, 29.141f, 36.301f);
        }
    }

    public class Circle extends Gate {
        String label;
        public Circle(String label) { this.label = label; }
        public boolean result(boolean x, boolean y, boolean z) { return false; }
        public String label() { return label; }
        public void makePath(GeneralPath gp) {
            int S = 30;
            gp.moveTo(0, S/2);
            gp.lineTo(S/2, S);
            gp.lineTo(S, S/2);
            gp.lineTo(S/2, 0);
            gp.lineTo(0, S/2);
            gp.closePath();
        }
    }

    public class And extends Gate {
        public boolean result(boolean x, boolean y, boolean z) { return x && y && z; }
        public String label() { return "&"; }
        public void makePath(GeneralPath gp) {
            gp.moveTo(0, 2);
            gp.lineTo(0, 19);
            gp.curveTo(0, 27, 3, 35, 13, 35);
            gp.curveTo(20, 35, 23, 27, 23, 19);
            gp.lineTo(23, 2);
            gp.closePath();
        }
    }

    public class Muller extends And {
        public String label() { return "C"; }
        public void draw(Graphics2D g, Color fill, Color stroke, Color text) {
            super.draw(g, fill, stroke, text);
            g.setColor(stroke);
            g.drawLine(0, 0, 23, 0);
        }
    }

    public class Xor extends Or {
        public boolean result(boolean x, boolean y, boolean z) { return x ^ y ^ z; }
        public String label() { return "^"; }
        public void draw(Graphics2D g, Color fill, Color stroke, Color text) {
            super.draw(g, fill, stroke, text);
            g.setColor(stroke);
            AffineTransform at = g.getTransform();
            g.scale(1, -1);
            g.translate(0, -40);

            g.translate(0, 4);
            GeneralPath gp = new GeneralPath();
            float x = 0.5f;
            float y = 29.141f+7.161f;
            gp.moveTo(x,y);
            gp.curveTo(5.729f+x, -1.789f+y,
                       6.444f+x, -2.686f+y,
                       14.32f+x, -3.58f+y);
            gp.curveTo(22.697f, 33.616f, 23.413f, 34.512f, 29.141f, 36.301f);
            g.draw(gp);

            g.setTransform(at);
        }
    }

    public abstract class Mux extends Gate {
        public String label() { return "?"; }
        public void makePath(GeneralPath gp) {
            gp.moveTo(0, 15);
            gp.lineTo(2, 23);
            gp.lineTo(23, 23);
            gp.lineTo(25, 15);
            gp.lineTo(0, 15);
        }
    }

    public class X_Mux extends Mux {
        public boolean result(boolean x, boolean y, boolean z) { return x ? y : z; }
    }
    public class Y_Mux extends Mux {
        public boolean result(boolean x, boolean y, boolean z) { return y ? x : z; }
    }
    public class Z_Mux extends Mux {
        public boolean result(boolean x, boolean y, boolean z) { return z ? x : y; }
    }

    public abstract class Buf extends Gate {
        public void makePath(GeneralPath gp) {
            gp.moveTo(0, 15);
            gp.lineTo(13, 35);
            gp.lineTo(25, 15);
            gp.lineTo(0, 15);
        }
    }

    public class X extends Buf {
        public boolean result(boolean x, boolean y, boolean z) { return x; }
    }
    public class Y extends Buf {
        public boolean result(boolean x, boolean y, boolean z) { return y; }
    }
    public class Z extends Buf {
        public boolean result(boolean x, boolean y, boolean z) { return z; }
    }

    private static Image keyboard1 =
        Toolkit.getDefaultToolkit().createImage("images/keyboard1.png");
    private static Image keyboard2 =
        Toolkit.getDefaultToolkit().createImage("images/keyboard2.png");
    private static Image keyboard3 =
        Toolkit.getDefaultToolkit().createImage("images/keyboard3.png");

}
