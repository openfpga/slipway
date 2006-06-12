package edu.berkeley.obits;

import static edu.berkeley.obits.device.atmel.AtmelDevice.Constants.*;
import static edu.berkeley.obits.device.atmel.AtmelDevice.Util.*;
import edu.berkeley.obits.device.atmel.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class Visual extends JFrame implements KeyListener {

    public static int SIZE = 92;
    public static int RINGS = 3;
    public static int BEVEL = 5;
    public static int CORE_SIZE = 64;
    public static int CORE_OFFSET = 10;

    public static int HOFF = 52;

    /*
    public static void main(String[] s) {
        Visual v = new Visual();
        v.show();
        v.setSize(400, 400);
    }
    */

    At40k at40k;

    public Visual(At40k at40k) {
        this.at40k = at40k;
        for(int x=0; x<7; x++)
            for(int y=0; y<7; y++)
                new Cell(x,y, at40k.cell(x+7, y+7));
        addKeyListener(this);
    }

    private HashSet<Cell> cells = new HashSet<Cell>();

    public class Cell {
        Graphics2D g;
        At40k.Cell cell;
        int _x, _y;
        public Cell(int x, int y, At40k.Cell cell) {
            _x = x;
            _y = y;
            this.cell = cell;
            cells.add(this);
        }
        public void draw() {
            drawHwires();
            drawVwires();
            drawInternalRouting();
            drawLocal();
            drawGates();
            drawMux();
            drawRegister();
            drawBorder();
        }
        public void drawHwires() {
            g.setColor(Color.magenta);
            for(int i=0; i<5; i++)
                if (cell.hwire(i).isDriven())
                    g.drawLine(0, SIZE-(2*(1+RINGS)+2*i), SIZE, SIZE-(2*(1+RINGS)+2*i));
        }
        public void drawVwires() {
            g.setColor(Color.magenta);
            for(int i=0; i<5; i++)
                if (cell.vwire(i).isDriven())
                    g.drawLine(2*(1+RINGS)+2*i, 0, 2*(1+RINGS)+2*i, SIZE);
        }
        public void drawInternalRouting() {
        }

        public void drawLocal() {
            if (!cell.ylut_relevant() && !cell.ylut_relevant()) return;
            Point2D in   = new Point2D.Double(HOFF, 0);
            Point2D join = new Point2D.Double(HOFF, CORE_OFFSET);
            rotateOuter(in);
            rotateInner(join);
            int rot = rot();
            switch(rot) {
                case 0: case 2:
                    join.setLocation(in.getX(), join.getY());
                    break;
                case 1: case 3:
                    join.setLocation(join.getX(), in.getY());
                    break;
            }

            Point2D xi = null;
            g.setColor(new Color(0xff, 0x00, 0x00));
            int xring = 4;
            switch(cell.xi()) {
                case NW:
                    xi = new Point2D.Double(0+2*xring, SIZE-2*xring);
                    g.draw(new Line2D.Double(new Point2D.Double(0, SIZE), xi));
                    break;

                case SW:
                    xi = new Point2D.Double(0+2*xring, 0+2*xring);
                    g.draw(new Line2D.Double(new Point2D.Double(0, 0), xi));
                    break;

                case NE:
                    xi = new Point2D.Double(SIZE-2*xring, SIZE-2*xring);
                    g.draw(new Line2D.Double(new Point2D.Double(SIZE, SIZE), xi));
                    break;

                case SE:
                    xi = new Point2D.Double(SIZE-2*xring, 0+2*xring);
                    g.draw(new Line2D.Double(new Point2D.Double(SIZE, 0), xi));
                    break;

            }

            if (cell.xlut_relevant()) {
                g.setColor(new Color(0x00, 0x00, 0xff));
                Point2D c   = new Point2D.Double(SIZE - CORE_OFFSET - CORE_SIZE/2 - CORE_SIZE / 6, 20);
                rotateInner(c);
                route(in, c, 5);

                g.setColor(new Color(0xff, 0x00, 0x00));
                c   = new Point2D.Double(SIZE - CORE_OFFSET - CORE_SIZE/2 - CORE_SIZE / 3, 20);
                rotateInner(c);
                if (xi != null)
                    route(xi, c, 4);

                Point2D xo   = new Point2D.Double(SIZE-CORE_OFFSET-CORE_SIZE+17 - 2, CORE_OFFSET + 41 - 3);
                Point2D xout = new Point2D.Double(SIZE-CORE_OFFSET-CORE_SIZE+17 - 2, CORE_OFFSET + CORE_SIZE - 3);
                rotateInner(xo);
                rotateInner(xout);
                g.setColor(new Color(0xff, 0xcc, 0xcc));
                g.draw(new Line2D.Double(xo, xout));
                if (cell.ne() != null && cell.ne().xi()==SW) {
                    Point2D xoo = new Point2D.Double(SIZE-2*xring, SIZE-2*xring);
                    g.draw(new Line2D.Double(new Point2D.Double(SIZE, SIZE), xoo));
                    route(xout, xoo, xring);
                }
                if (cell.nw() != null && cell.nw().xi()==SE) {
                    Point2D xoo = new Point2D.Double(0+2*xring, SIZE-2*xring);
                    g.draw(new Line2D.Double(new Point2D.Double(0, SIZE), xoo));
                    route(xout, xoo, xring);
                }
                if (cell.se() != null && cell.se().xi()==NW) {
                    Point2D xoo = new Point2D.Double(SIZE-2*xring, 0+2*xring);
                    g.draw(new Line2D.Double(new Point2D.Double(SIZE, 0), xoo));
                    route(xout, xoo, xring);
                }
                if (cell.sw() != null && cell.sw().xi()==NE) {
                    Point2D xoo = new Point2D.Double(0+2*xring, 0+2*xring);
                    g.draw(new Line2D.Double(new Point2D.Double(0, 0), xoo));
                    route(xout, xoo, xring);
                }
            }

            if (cell.ylut_relevant()) {
                g.setColor(new Color(0x00, 0x00, 0xff));
                Point2D c   = new Point2D.Double(SIZE - CORE_OFFSET - CORE_SIZE/2 + CORE_SIZE / 6, 20);
                rotateInner(c);
                route(in, c, 5);

                g.setColor(new Color(0xff, 0x00, 0x00));
                c   = new Point2D.Double(SIZE - CORE_OFFSET - CORE_SIZE/2 + CORE_SIZE / 3, 20);
                rotateInner(c);
                if (xi != null)
                    route(xi, c, 4);

                Point2D yo   = new Point2D.Double(SIZE-CORE_OFFSET-CORE_SIZE+51 - 2, CORE_OFFSET + 41 - 3);
                Point2D yout = new Point2D.Double(SIZE-CORE_OFFSET-CORE_SIZE+51 - 2, CORE_OFFSET + CORE_SIZE - 3);
                rotateInner(yo);
                rotateInner(yout);
                g.setColor(new Color(0xbb, 0xbb, 0xff));
                //g.setColor(new Color(0x00, 0x00, 0xff));
                g.draw(new Line2D.Double(yo, yout));
                if (cell.north() != null && cell.north().yi()==SOUTH) route(yout, new Point2D.Double(SIZE-40, SIZE+ 0), 2);
                if (cell.east() != null  && cell.east().yi()==WEST)   route(yout, new Point2D.Double(SIZE+ 0,      40), 2);
                if (cell.south() != null && cell.south().yi()==NORTH) route(yout, new Point2D.Double(     40,       0), 2);
                if (cell.west() != null  && cell.west().yi()==EAST)   route(yout, new Point2D.Double(      0, SIZE-40), 2);
            }

        }

        private void rotateOuter(Point2D p) {
            int rot = rot();
            AffineTransform a = new AffineTransform();
            a.rotate((Math.PI/2) * rot);
            switch(rot) {
                case 0: break;
                case 1: a.translate(0,  -SIZE); break;
                case 2: a.translate(-SIZE, -SIZE); break;
                case 3: a.translate(-SIZE, 0); break;
            }
            a.transform(p, p);
        }

        private void rotateInner(Point2D p) {
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
            a.transform(p, p);
        }

        private Point2D project(Point2D p1, int ring) {
            double north = Math.abs( (SIZE-(ring*2)) - p1.getY() );
            double south = Math.abs( (     (ring*2)) - p1.getY() );
            double east  = Math.abs( (SIZE-(ring*2)) - p1.getX() );
            double west  = Math.abs( (     (ring*2)) - p1.getX() );
            if (north < south && north < east && north < west) {
                return new Point2D.Double(p1.getX(), SIZE-ring*2);
            } else if (south < east && south < west) {
                return new Point2D.Double(p1.getX(), ring*2);
            } else if (east < west) {
                return new Point2D.Double(SIZE-ring*2, p1.getY());
            } else {
                return new Point2D.Double(ring*2, p1.getY());
            }
        }

        private void route(Point2D p1, Point2D p2, int ring) {
            int ringpos = ring * 2;
            Point2D projected = project(p1, ring);
            g.draw(new Line2D.Double(p1, projected));
            p1 = projected;

            projected = project(p2, ring);
            g.draw(new Line2D.Double(p2, projected));
            p2 = projected;

            double x1 = p1.getX();
            double y1 = p1.getY();
            double x2 = p2.getX();
            double y2 = p2.getY();

            if (x1==x2 || y1==y2) {
                g.draw(new Line2D.Double(p1, p2));
                return;
            }

            if ((x1==SIZE-ring*2 || x1==ring*2) && !(y1==SIZE-ring*2 || y1==ring*2)) {
                Point2D p3 = new Point2D.Double(x1, y2 > SIZE/2 ? SIZE-ring*2 : ring*2);
                g.draw(new Line2D.Double(p1, p3));
                route(p3, p2, ring);
            }

            if (y1==SIZE-ring*2 || y1==ring*2) {
                Point2D p3 = new Point2D.Double(x2 > SIZE/2 ? SIZE-ring*2 : ring*2, y1);
                g.draw(new Line2D.Double(p1, p3));
                route(p3, p2, ring);
            }

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

                //g.setColor(Color.gray);
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
                    g.setColor(Color.white);
                    g.fill(p);
                    g.setColor(Color.red);
                    g.draw(p);
                }
                g.translate(34f, 0f);
                if (cell.ylut_relevant()) {
                    g.setColor(Color.white);
                    g.fill(p);
                    g.setColor(Color.blue);
                    g.draw(p);
                }
            } finally {
                g.setTransform(t);
            }
        }
        public void drawMux() {
            if (cell.c() != ZMUX) return;
            g.setColor(Color.black);
            g.drawLine(46, 54,     46+2, 54+5);
            g.drawLine(46+2, 54+5, 60-2, 54+5);
            g.drawLine(60-2, 54+5, 60,   54);
            g.drawLine(60,   54,   46, 54);
        }
        public void drawRegister() {
        }
        public void drawBorder() {
            g.setColor(Color.gray);
            //g.drawLine(0, BEVEL, BEVEL,    0);
            g.drawLine(BEVEL, 0, SIZE-BEVEL, 0);
            //g.drawLine(SIZE-BEVEL, 0, SIZE, BEVEL);
            g.drawLine(SIZE, BEVEL, SIZE, SIZE-BEVEL);
            //g.drawLine(SIZE, SIZE-BEVEL, SIZE-BEVEL, SIZE);
            g.drawLine(SIZE-BEVEL, SIZE, BEVEL, SIZE);
            //g.drawLine(BEVEL, SIZE, 0, SIZE-BEVEL);
            g.drawLine(0, SIZE-BEVEL, 0, BEVEL);
        }
    }

    public void paint(Graphics _g) {
        Graphics2D g = (Graphics2D)_g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        g.setStroke(new BasicStroke((float)0.5));
        g.translate(10, 0);
        g.scale(1, -1);
        g.translate(5, -1 * getHeight() + 10);
        g.scale(scale,scale);
        for(Cell c : cells) {
            AffineTransform t = g.getTransform();
            g.translate(     c._x * SIZE/* + (10 * (c._x/4))*/,      c._y * SIZE/* + (10 * (c._y/4))*/);
            c.g = g;
            c.draw();
            c.g = null;
            g.setTransform(t);
        }
    }

    double scale = 1.0;

    public void clear() {
        Graphics2D g = (Graphics2D)getGraphics();
        //g.setColor(Color.black);
        //g.setColor(Color.lightGray);
        g.clearRect(0, 0, getWidth(), getHeight());
    }

    public void keyTyped(KeyEvent k) {
    }
    public void keyReleased(KeyEvent k) {
    }
    public void keyPressed(KeyEvent keyevent) {
        char c = keyevent.getKeyChar();
        switch(c) {
            case '+': scale += 0.1; clear(); paint(getGraphics()); return;
            case '-': scale -= 0.1; clear(); paint(getGraphics()); return;
        }
    }

}
