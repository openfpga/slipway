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

/** a point, since Java2D's Point2D class sucks rocks */
public class P {

    public final double x;
    public final double y;

    public P(double x, double y) { this.x = x; this.y = y; }
    public P(Point2D p) { this(p.getX(), p.getY()); }
    public double getX() { return x; }
    public double getY() { return y; }

    public P transform(AffineTransform a) {
        Point2D me = new Point2D.Double(x, y);
        return new P(a.transform(me, me));
    }
    public P inverseTransform(AffineTransform a) {
        try {
            Point2D me = new Point2D.Double(x, y);
            return new P(a.inverseTransform(me, me));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
