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

    public void color(Color c) { g.setColor(c); }
    public void color(int color) {
        g.setColor(new Color((color >> 16) & 0xff,
                             (color >>  8) & 0xff,
                             (color >>  0) & 0xff
                             ));
    }

}
