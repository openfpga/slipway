package edu.berkeley.slipway.gui;

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

/** Graphics */
public class G {

    public final Graphics2D g;

    public G(Graphics2D g) { this.g = g; }
    public G(Graphics g) { this((Graphics2D)g); }

    public void line(Point2D p1, Point2D p2) { line(new P(p1), new P(p2)); }

    public void line(P p1, P p2) { line(p1.x, p1.y, p2.x, p2.y); }
    public void line(double x, double y, P p2) { line(x, y, p2.x, p2.y); }
    public void line(P p1, double x, double y) { line(p1.x, p1.y, x, y); }
    public void line(double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }
    public void line(double x1, double y1, double x2, double y2, int stroke) {
        g.setStroke(new BasicStroke(stroke));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        g.setStroke(new BasicStroke(1));
    }
    public void setFont(Font f) { g.setFont(f); }
    public void drawString(String s, P p) { drawString(s, p.x, p.y); }
    public void drawString(String s, double x, double y) {
        g.drawString(s, (int)x, (int)y);
    }
    public void color(Color c) { g.setColor(c); }
    public void color(int color) {
        g.setColor(new Color((color >> 16) & 0xff,
                             (color >>  8) & 0xff,
                             (color >>  0) & 0xff
                             ));
    }

    private ArrayList<AffineTransform> transformStack =
        new ArrayList<AffineTransform>();

    public AffineTransform getTransform() { return g.getTransform(); }
    public void pushTransform() {
        transformStack.add(new AffineTransform(g.getTransform()));
    }
    public void popTransform() {
        AffineTransform t = transformStack.remove(transformStack.size()-1);
        g.setTransform(t);
    }

    public void fillTriangle(double x1, double y1, 
                             double x2, double y2, 
                             double x3, double y3) {
        GeneralPath gp = new GeneralPath();
        gp.moveTo((float)x1, (float)y1);
        gp.lineTo((float)x2, (float)y2);
        gp.lineTo((float)x3, (float)y3);
        gp.closePath();
        g.fill(gp);
    }

    public void route(P p1, R r, P p2) {
        if      (p1.x < r.minx() && p1.y < r.miny()) { line(p1, r.minx(), r.miny()); route(new P(r.minx(), r.miny()), r, p2); }
        else if (p1.x > r.maxx() && p1.y < r.miny()) { line(p1, r.maxx(), r.miny()); route(new P(r.maxx(), r.miny()), r, p2); }
        else if (p1.x < r.minx() && p1.y > r.maxy()) { line(p1, r.minx(), r.maxy()); route(new P(r.minx(), r.maxy()), r, p2); }
        else if (p1.x > r.maxx() && p1.y > r.maxy()) { line(p1, r.maxx(), r.maxy()); route(new P(r.maxx(), r.maxy()), r, p2); }
        else if (p1.x < r.minx()) { line(p1, r.minx(), p1.y); route(new P(r.minx(), p1.y), r, p2); }
        else if (p1.x > r.maxx()) { line(p1, r.maxx(), p1.y); route(new P(r.maxx(), p1.y), r, p2); }
        else if (p1.y < r.miny()) { line(p1, p1.x, r.miny()); route(new P(p1.x, r.miny()), r, p2); }
        else if (p1.y > r.maxy()) { line(p1, p1.x, r.maxy()); route(new P(p1.x, r.maxy()), r, p2); }
        else {
            double updist    = Math.abs(r.maxy()-p2.y);
            double downdist  = Math.abs(r.miny()-p2.y);
            double leftdist  = Math.abs(r.minx()-p2.x);
            double rightdist = Math.abs(r.maxx()-p2.x);
            if (updist != 0 && updist <= downdist && updist <= leftdist && updist <= rightdist)
                { line(p2, p2.x, r.maxy()); route(p1, r, new P(p2.x, r.maxy())); }
            else if (downdist != 0 && downdist <= updist && downdist <= leftdist && downdist <= rightdist)
                { line(p2, p2.x, r.miny()); route(p1, r, new P(p2.x, r.miny())); }
            else if (leftdist != 0 && leftdist <= downdist && leftdist <= updist && leftdist <= rightdist)
                { line(p2, r.minx(), p2.y); route(p1, r, new P(r.minx(), p2.y)); }
            else if (rightdist != 0 && rightdist <= updist && rightdist <= downdist && rightdist <= leftdist)
                { line(p2, r.maxx(), p2.y); route(p1, r, new P(r.maxx(), p2.y)); }
            else {
                if (p2.x == p1.x && (p2.x==r.maxx() || p2.x==r.minx())) line(p1, p2);
                else if (p2.y == p1.y && (p2.y==r.maxy() || p2.y==r.miny())) line(p1, p2);

                // these cases are where p1 and p2 are each on opposite sides
                else if (p2.y==r.maxy() && p1.y==r.miny() && p1.x!=r.minx() && p1.x!=r.maxx() && p2.x!=r.minx() && p2.x!=r.maxx()) { }
                else if (p1.y==r.maxy() && p2.y==r.miny() && p1.x!=r.minx() && p1.x!=r.maxx() && p2.x!=r.minx() && p2.x!=r.maxx()) { }
                else if (p1.x==r.maxx() && p2.x==r.minx() && p1.y!=r.miny() && p1.y!=r.maxy() && p2.y!=r.miny() && p2.y!=r.maxy()) { }
                else if (p2.x==r.maxx() && p1.x==r.minx() && p1.y!=r.miny() && p1.y!=r.maxy() && p2.y!=r.miny() && p2.y!=r.maxy()) { }

                else {
                    if (Math.abs(p1.x-p2.x) < Math.abs(p1.y-p2.y)) {
                        if (p1.x == r.minx() || p1.x == r.maxx()) {
                            line(p2, p1.x, p2.y);
                            route(p1, r, new P(p1.x, p2.y));
                        } else {
                            line(p1, p2.x, p1.y);
                            route(p2, r, new P(p2.x, p1.y));
                        }
                    } else {
                        if (p1.y == r.miny() || p1.y == r.maxy()) {
                            line(p2, p2.x, p1.y);
                            route(p1, r, new P(p2.x, p1.y));
                        } else {
                            line(p1, p1.x, p2.y);
                            route(p2, r, new P(p1.x, p2.y));
                        }
                    }
                }
            }
        }
    }

}
