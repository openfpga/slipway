package edu.berkeley.slipway.gui;

import com.atmel.fpslic.*;
import java.awt.geom.*;

public class R {

    private double x1, x2, y1, y2;
    public R(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
    }

    public double getWidth() { return Math.abs(x2-x1); }
    public double getHeight() { return Math.abs(y2-y1); }
    public double minx() { return Math.min(x1, x2); }
    public double miny() { return Math.min(y1, y2); }
    public double maxx() { return Math.max(x1, x2); }
    public double maxy() { return Math.max(y1, y2); }
    public double cx() { return (x1+x2)/2; }
    public double cy() { return (y1+y2)/2; }
    public double width() { return Math.abs(x2-x1); }
    public double height() { return Math.abs(y2-y1); }

    public void fill(G g) {
        g.g.fill(new Rectangle2D.Double(minx(), miny(), width(), height()));
    }
    public void draw(G g) {
        g.line(x1, y1, x1, y2);
        g.line(x1, y2, x2, y2);
        g.line(x2, y2, x2, y1);
        g.line(x2, y1, x1, y1);
    }

    public boolean contains(P p) {
        return p.x >= minx() && p.x < maxx() && p.y >= miny() && p.y < maxy();
    }

    public R plus(double minxplus, double minyplus, double maxxplus, double maxyplus) {
        return new R(minx()+minxplus,
                     miny()+minyplus,
                     maxx()+maxxplus,
                     maxy()+maxyplus);
    }
}