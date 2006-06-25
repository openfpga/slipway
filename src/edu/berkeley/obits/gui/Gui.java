package edu.berkeley.obits.gui;

import static edu.berkeley.obits.device.atmel.AtmelDevice.Constants.*;
import static edu.berkeley.obits.device.atmel.AtmelDevice.Util.*;
import edu.berkeley.obits.*;
import edu.berkeley.obits.device.atmel.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class Gui extends ZoomingPanel implements KeyListener, MouseMotionListener {

    Graphics2D g;
    G gg;

    public static int SIZE = 92;
    public static int RINGS = 3;
    public static int BEVEL = 5;
    public static int CORE_SIZE = 64;
    public static int CORE_OFFSET = 10;
    public static int HOFF = 52;

    public static int RED  = 0xff0000;
    public static int BLUE = 0x0000ff;

    public static final P YLUT_OUTPUT_POINT  = new P(SIZE-CORE_OFFSET-CORE_SIZE+51 - 2, CORE_OFFSET + 41 - 3);
    public static final P XLUT_OUTPUT_POINT  = new P(SIZE-CORE_OFFSET-CORE_SIZE+17 - 2, CORE_OFFSET + 41 - 3);

    At40k at40k;

    private HashSet<Cell> cells = new HashSet<Cell>();
    private Cell[][] ca = new Cell[128][];

    public static final Color nonselectedcell = new Color(0xcc, 0xcc, 0xcc);
    public static final Color selectedcell = new Color(0x44, 0x44, 0xff);

    public Gui(At40k at40k) {
        this.at40k = at40k;
        for(int i=0; i<ca.length; i++)
            ca[i] = new Cell[128];
        for(int x=0; x<7; x++)
            for(int y=0; y<7; y++)
                new Cell(x,y, at40k.cell(x+7, y+7));
    }
    

    public class Cell {
        At40k.Cell cell;
        boolean in = false;
        int _x, _y;
        public Cell(int x, int y, At40k.Cell cell) {
            _x = x;
            _y = y;
            ca[_x][_y] = this;
            this.cell = cell;
            cells.add(this);
        }
        public void draw() {
            gg.color(in ? selectedcell : nonselectedcell);
            g.fillRect(0, 0, SIZE, SIZE);

            drawHwires();
            drawVwires();
            drawLocal();

            AffineTransform t = g.getTransform();
            g.transform(rotateInnerTransform());
            drawMux();
            drawRegister();
            drawInternalRouting();
            drawBuffer();
            g.setTransform(t);

            drawGates();
            drawBorder();
        }
        public void drawBuffer() {
            if (!cell.out_relevant()) return;
            gg.color(Color.black);
            gg.line(21, 64, 28, 60);
            gg.line(21, 64, 28, 68);
            gg.line(28, 60, 28, 68);

            gg.color(Color.magenta);
            if (cell.oe() == V4) {
                gg.line(16, 53, 25, 53);
                gg.line(25, 53, 25, 62);
            } else if (cell.oe() == H4) {
                gg.line(25, 76, 25, 67);
            }
            
        }
        public void drawHwires() {
            gg.color(Color.magenta);
            for(int i=0; i<5; i++)
                if (cell.hwire(i).isDriven())
                    gg.line(0, SIZE-(2*(1+RINGS)+2*i), SIZE, SIZE-(2*(1+RINGS)+2*i));
            int plane = cell.zi();
            if (plane >= L0 && plane <= L4 && cell.hx(plane)) {
                P p1 = new P(38, 18);
                p1 = rotateInner(p1);
                if (cell.zi_to_xlut_relevant() && cell.xlut_relevant())
                    route(new P(84, 84 - 2*(plane-L0)), p1, 3);
                p1 = new P(64, 18);
                p1 = rotateInner(p1);
                if (cell.zi_to_ylut_relevant() && cell.ylut_relevant())
                    route(new P(84, 84 - 2*(plane-L0)), p1, 3);
            }
            plane = cell.wi();
            if (plane >= L0 && plane <= L4 && cell.hx(plane)) {
                P p1 = new P(38, 18);
                p1 = rotateInner(p1);
                if (cell.zi_to_xlut_relevant() && cell.xlut_relevant())
                    route(new P(84, 84 - 2*(plane-L0)), p1, 3);
                p1 = rotateInner(new P(64, 18));
                if (cell.zi_to_ylut_relevant() && cell.ylut_relevant())
                    route(new P(84, 84 - 2*(plane-L0)), p1, 3);
            }
        }
        public void drawVwires() {
            gg.color(Color.magenta);
            for(int i=0; i<5; i++)
                if (cell.vwire(i).isDriven())
                    gg.line(2*(1+RINGS)+2*i, 0, 2*(1+RINGS)+2*i, SIZE);
            int plane = cell.zi();
            if (plane >= L0 && plane <= L4 && cell.vx(plane)) {
                P p1 = new P(38, 18);
                p1 = rotateInner(p1);
                if (cell.zi_to_xlut_relevant() && cell.xlut_relevant())
                    route(new P(17 - 2*(plane-L0), 8), p1, 3);
                p1 = new P(64, 18);
                p1 = rotateInner(p1);
                if (cell.zi_to_ylut_relevant() && cell.ylut_relevant())
                    route(new P(17 - 2*(plane-L0), 8), p1, 3);
            }
            plane = cell.wi();
            if (plane >= L0 && plane <= L4 && cell.vx(plane)) {
                P p1 = new P(38, 18);
                p1 = rotateInner(p1);
                if (cell.zi_to_xlut_relevant() && cell.xlut_relevant())
                    route(new P(17 - 2*(plane-L0), 8), p1, 3);
                p1 = new P(64, 18);
                p1 = rotateInner(p1);
                if (cell.zi_to_ylut_relevant() && cell.ylut_relevant())
                    route(new P(17 - 2*(plane-L0), 8), p1, 3);
            }
        }
            public P corner(int corner, int ring) {
                switch(corner) {
                    case NW: return new P(0    +2*ring,  SIZE -2*xring);
                    case SW: return new P(0    +2*ring,  0    +2*xring);
                    case NE: return new P(SIZE -2*ring,  SIZE -2*xring);
                    case SE: return new P(SIZE -2*ring,  0    +2*xring);
                    default: return null;
                }
            }

        public void drawInternalRouting() {
            gg.color(new Color(0, 107, 51));
            if (cell.fb_relevant()) {
                if (cell.f()) {
                    gg.line(51, 74, 37, 74);
                    gg.line(37, 74, 50, 12);
                } else if (cell.c() == XLUT) {
                    gg.line(32, 52, 50, 52);
                    gg.line(50, 52, 50, 12);
                } else if (cell.c() == YLUT) {
                    gg.line(68, 52, 50, 52);
                    gg.line(50, 52, 50, 12);
                } else {
                    gg.line(50, 56, 41, 56);
                    gg.line(41, 56, 50, 12);
                }
                if (cell.xlut_relevant()) {
                    gg.line(52, 12, XLUT_OUTPUT_POINT.getX(), 12);
                    gg.line(XLUT_OUTPUT_POINT.getX(), 12, XLUT_OUTPUT_POINT.getX(), 32);
                }
                if (cell.ylut_relevant()) {
                    gg.line(52, 12, YLUT_OUTPUT_POINT.getX(), 12);
                    gg.line(YLUT_OUTPUT_POINT.getX(), 12, YLUT_OUTPUT_POINT.getX(), 32);
                }
            }
        }

        public void drawLocal() {
            if (!cell.ylut_relevant() && !cell.xlut_relevant()) return;
            P in   = new P(HOFF, 0);
            P join = new P(HOFF, CORE_OFFSET);
            in = rotateOuter(in);
            join = rotateInner(join);
            int rot = rot();
            switch(rot) {
                case 0: case 2:
                    join = new P(in.getX(), join.getY());
                    break;
                case 1: case 3:
                    join = new P(join.getX(), in.getY());
                    break;
            }

            P xi  = corner(cell.xi(), xring);
            P xi2 = null;
            gg.color(new Color(0xff, 0x00, 0x00));
            int xring = 4;
            if (cell.xi_relevant() && cell.xlut_relevant())
                switch(cell.xi()) {
                    case NW:
                        xi2 = new P(-BEVEL, SIZE+BEVEL);
                        xi = translate(xi, 0, -3);
                        xi2 = translate(xi2, 0, -3);
                        gg.line(xi2, xi);
                        break;
                        
                    case SW:
                        xi2 = new P(-BEVEL, -BEVEL);
                        xi = translate(xi, 0, 3);
                        xi2 = translate(xi2, 0, 3);
                        gg.line(xi2, xi);
                        break;
                        
                    case NE:
                        xi2 = new P(SIZE+BEVEL, SIZE+BEVEL);
                        xi = translate(xi, 0, -3);
                        xi2 = translate(xi2, 0, -3);
                        gg.line(xi2, xi);
                        break;
                        
                    case SE:
                        xi2 = new P(SIZE+BEVEL, -BEVEL);
                        xi = translate(xi, 0, 3);
                        xi2 = translate(xi2, 0, 3);
                        gg.line(xi2, xi);
                        break;
                }

            if (cell.xlut_relevant()) {
                P c;
                gg.color(BLUE);
                if (cell.yi_to_xlut_relevant())
                    route(in, rotateInner(new P(SIZE - CORE_OFFSET - CORE_SIZE/2 - CORE_SIZE / 6, 20)), 5);

                gg.color(RED);
                if (cell.xi_to_xlut_relevant() && xi != null)
                    route(xi, rotateInner(new P(SIZE - CORE_OFFSET - CORE_SIZE/2 - CORE_SIZE / 3, 20)), 4);

                P xo   = XLUT_OUTPUT_POINT;
                P xout = new P(SIZE-CORE_OFFSET-CORE_SIZE+17 - 2, CORE_OFFSET + CORE_SIZE - 3);
                xo = rotateInner(xo);
                xout = rotateInner(xout);
                gg.color(new Color(0xff, 0xcc, 0xcc));
                xring = 6;
                if (cell.ne() != null && cell.ne().xi()==SW && cell.ne().xi_relevant() && cell.ne().xlut_relevant()) {
                    gg.line(xo, xout);
                    P xoo = new P(SIZE-2*xring, SIZE-2*xring);
                    P xoo2 = new P(SIZE, SIZE);
                    xoo = translate(xoo, -3, 0);
                    xoo2 = translate(xoo2, -3, 0);
                    gg.line(xoo2, xoo);
                    route(xout, xoo, xring);
                }
                if (cell.nw() != null && cell.nw().xi()==SE && cell.nw().xi_relevant() && cell.nw().xlut_relevant()) {
                    gg.line(xo, xout);
                    P xoo = new P(0+2*xring, SIZE-2*xring);
                    P xoo2 = new P(0, SIZE);
                    xoo = translate(xoo, 3, 0);
                    xoo2 = translate(xoo2, 3, 0);
                    gg.line(xoo2, xoo);
                    route(xout, xoo, xring);
                }
                if (cell.se() != null && cell.se().xi()==NW && cell.se().xi_relevant() && cell.se().xlut_relevant()) {
                    gg.line(xo, xout);
                    P xoo = new P(SIZE-2*xring, 0+2*xring);
                    P xoo2 = new P(SIZE, 0);
                    xoo = translate(xoo, -3, 0);
                    xoo2 = translate(xoo2, -3, 0);
                    gg.line(xoo2, xoo);
                    route(xout, xoo, xring);
                }
                if (cell.sw() != null && cell.sw().xi()==NE && cell.sw().xi_relevant() && cell.sw().xlut_relevant()) {
                    gg.line(xo, xout);
                    P xoo = new P(0+2*xring, 0+2*xring);
                    P xoo2 = new P(0, 0);
                    xoo = translate(xoo, 3, 0);
                    xoo2 = translate(xoo2, 3, 0);
                    gg.line(xoo2, xoo);
                    route(xout, xoo, xring);
                }
            }

            if (cell.ylut_relevant()) {
                gg.color(new Color(0x00, 0x00, 0xff));
                P c;
                if (cell.yi_to_ylut_relevant()) {
                    c   = new P(SIZE - CORE_OFFSET - CORE_SIZE/2 + CORE_SIZE / 6, 20);
                    c = rotateInner(c);
                    route(in, c, 5);
                }
                gg.color(new Color(0xff, 0x00, 0x00));
                if (cell.xi_to_ylut_relevant()) {
                    c = rotateInner(new P(SIZE - CORE_OFFSET - CORE_SIZE/2 + CORE_SIZE / 3, 20));
                    if (xi != null)
                        route(xi, c, 4);
                }

                P yo   = rotateInner(YLUT_OUTPUT_POINT);
                P yout = rotateInner(new P(SIZE-CORE_OFFSET-CORE_SIZE+51 - 2, CORE_OFFSET + CORE_SIZE - 3));
                gg.color(0xbbbbff);
                if (cell.north() != null && cell.north().yi()==SOUTH && cell.north().yi_relevant() && cell.north().ylut_relevant()) {
                    gg.line(yo, yout);
                    route(yout, new P(SIZE-40, SIZE+ 0), 2);
                }
                if (cell.east() != null  && cell.east().yi()==WEST && cell.east().yi_relevant() && cell.east().ylut_relevant()) {
                    gg.line(yo, yout);
                    route(yout, new P(SIZE+ 0,      40), 2);
                }
                if (cell.south() != null && cell.south().yi()==NORTH && cell.south().yi_relevant() && cell.south().ylut_relevant()) {
                    gg.line(yo, yout);
                    route(yout, new P(     40,       0), 2);
                }
                if (cell.west() != null  && cell.west().yi()==EAST && cell.west().yi_relevant() && cell.west().ylut_relevant()) {
                    gg.line(yo, yout);
                    route(yout, new P(      0, SIZE-40), 2);
                }
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

            double x1 = p1.getX();
            double y1 = p1.getY();
            double x2 = p2.getX();
            double y2 = p2.getY();

            if (x1==x2 || y1==y2) {
                gg.line(p1, p2);
                return;
            }

            if ((x1==SIZE-ring*2 || x1==ring*2) && !(y1==SIZE-ring*2 || y1==ring*2)) {
                P p3 = new P(x1, y2 > SIZE/2 ? SIZE-ring*2 : ring*2);
                gg.line(p1, p3);
                route(p3, p2, ring);
            } else if ((y1==SIZE-ring*2 || y1==ring*2) && !(x1==SIZE-ring*2 || x1==ring*2)) {
                P p3 = new P(x2 > SIZE/2 ? SIZE-ring*2 : ring*2, y1);
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
                g.scale(1, -1);

                GeneralPath p = new GeneralPath();
                p.moveTo(29.141f, 36.301f);
                p.lineTo(29.141f, 36.301f-7.161f);
                p.curveTo(27.71f, 11.24f, 23.413f, 9.45f, 14.82f, 0.5f);
                p.curveTo(6.229f, 9.45f, 1.932f, 11.24f, 0.5f, 29.141f);
                p.lineTo(0.5f, 29.141f+7.161f);
                float x = 0.5f;
                float y = 29.141f+7.161f;
                p.curveTo(5.729f+x, -1.789f+y,
                          6.444f+x, -2.686f+y,
                          14.32f+x, -3.58f+y);
                p.curveTo(22.697f, 33.616f, 23.413f, 34.512f, 29.141f, 36.301f);
                g.translate(0,   -40f);
                if (cell.xlut_relevant()) {
                    gg.color(Color.white);
                    g.fill(p);
                    gg.color(Color.red);
                    g.draw(p);
                }
                g.translate(34f, 0f);
                if (cell.ylut_relevant()) {
                    gg.color(Color.white);
                    g.fill(p);
                    gg.color(Color.blue);
                    g.draw(p);
                }
            } finally {
                g.setTransform(t);
            }
        }
        public void drawMux() {
            if (!cell.c_relevant()) return;
            gg.color(Color.black);
            if (cell.xlut_relevant() && (cell.c() == ZMUX || cell.c() == XLUT)) {
                gg.color(new Color(0xff, 0xbb, 0xbb));
                gg.line(XLUT_OUTPUT_POINT, new P(XLUT_OUTPUT_POINT.getX(), 52));
                gg.line(new P(XLUT_OUTPUT_POINT.getX(), 52), new P(51, 52));
            }
            if (cell.ylut_relevant() && (cell.c() == ZMUX || cell.c() == YLUT)) {
                gg.color(new Color(0xbb, 0xbb, 0xff));
                gg.line(YLUT_OUTPUT_POINT, new P(YLUT_OUTPUT_POINT.getX(), 52));
                gg.line(new P(YLUT_OUTPUT_POINT.getX(), 52), new P(51, 52));
            }
            gg.line(51, 52, 51, 51+25);
            if (cell.c() == ZMUX) {
                gg.color(Color.black);
                gg.line(51, 52, 51, 51+25);
                gg.line(46, 54,     46+2, 54+5);
                gg.line(46+2, 54+5, 60-2, 54+5);
                gg.line(60-2, 54+5, 60,   54);
                gg.line(60,   54,   46, 54);
            }
        }
        public void drawRegister() {
            if (!cell.register_relevant()) return;
            gg.color(Color.white);
            g.fillRect(48, 58, 10, 14);
            gg.color(Color.black);
            g.drawRect(48, 58, 10, 14);
            gg.line(57, 70, 54, 68);
            gg.line(54, 68, 57, 66);
        }
        public void drawBorder() {
            gg.color(Color.gray);
            //gg.line(0, BEVEL, BEVEL,    0);
            gg.line(BEVEL, 0, SIZE-BEVEL, 0);
            //gg.line(SIZE-BEVEL, 0, SIZE, BEVEL);
            gg.line(SIZE, BEVEL, SIZE, SIZE-BEVEL);
            //gg.line(SIZE, SIZE-BEVEL, SIZE-BEVEL, SIZE);
            gg.line(SIZE-BEVEL, SIZE, BEVEL, SIZE);
            //gg.line(BEVEL, SIZE, 0, SIZE-BEVEL);
            gg.line(0, SIZE-BEVEL, 0, BEVEL);
        }
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
            g.translate(     c._x * SIZE/* + (10 * (c._x/4))*/,      c._y * SIZE/* + (10 * (c._y/4))*/);
            c.draw();
        }
        g.setTransform(t);

        g.setTransform(new AffineTransform());

        gg.color(selectedcell);
        g.fillRect(getWidth() - 200, 0, 200, 100);
        gg.color(Color.white);
        g.drawRect(getWidth() - 200, 0, 200, 100);

        Cell newcell = whichCell(mousex, mousey);
        int line = 10;
        if (newcell != null && newcell.cell != null) {
            g.drawString("selected: " + newcell._x + ","+newcell._y,
                         getWidth() - 200 + 10, (line += 15));
            g.drawString("    xlut: " + AtmelSerial.bin8(newcell.cell.xlut()),
                         getWidth() - 200 + 10, (line += 15));
            g.drawString("    ylut: " + AtmelSerial.bin8(newcell.cell.ylut()),
                         getWidth() - 200 + 10, (line += 15));
            String xi = "??";
            switch(newcell.cell.xi()) {
                case NW : xi = "NW"; break;
                case NE : xi = "NE"; break;
                case SW : xi = "SW"; break;
                case SE : xi = "SE"; break;
            }
            g.drawString("      xi: " + xi, getWidth() - 200 + 10, (line += 15));
            String yi = "??";
            switch(newcell.cell.yi()) {
                case NORTH : yi = "NORTH"; break;
                case SOUTH : yi = "SOUTH"; break;
                case EAST  : yi = "EAST"; break;
                case WEST  : yi = "WEST"; break;
            }
            g.drawString("      yi: " + yi, getWidth() - 200 + 10, (line += 15));
        }
        this.g = null;
        this.gg = null;
    }


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
        int col = ((int)p.getX()) / (SIZE + BEVEL);
        int row = ((int)p.getY()) / (SIZE + BEVEL);
        for(Cell c : cells)
            if (c._x == col && c._y == row)
                return c;
        return null;
    }
}
