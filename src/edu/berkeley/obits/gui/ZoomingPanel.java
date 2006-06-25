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

public abstract class ZoomingPanel extends JComponent implements KeyListener, MouseMotionListener {

    double scale = 1.0;
    double oscale = 1.0;
    public    int dragx = 0;
    public    int dragy = 0;
    public    boolean drag = false;
    protected int mousex;
    protected int mousey;
    protected AffineTransform transform = new AffineTransform();
    private   Point2D recenter;
    private   Point2D recenter2;

    public ZoomingPanel() {
        setDoubleBuffered(true);
        addKeyListener(this);
        addMouseMotionListener(this);
    }

    public abstract void _paint(Graphics2D g);

    public final void paint(Graphics _g) {
        Graphics2D g = (Graphics2D)_g;
        g.scale(scale,scale);
        g.translate(10, 0);
        g.scale(1, -1);
        g.translate(5, -1 * getHeight() + 10);

        transform = g.getTransform();

        Point2D p = new Point2D.Float();
        if (recenter != null) {
            transform.transform(recenter, p);
            transform.preConcatenate(AffineTransform.getTranslateInstance(dragx - p.getX(),
                                                                          dragy - p.getY()));
        }
        g.setTransform(transform);

        if (drag) {
            g.setColor(Color.blue);
            g.drawLine((int)(recenter.getX() - 10),
                       (int)(recenter.getY() - 10),
                       (int)(recenter.getX() + 10),
                       (int)(recenter.getY() + 10));
            g.drawLine((int)(recenter.getX() + 10),
                       (int)(recenter.getY() - 10),
                       (int)(recenter.getX() - 10),
                       (int)(recenter.getY() + 10));
        }
        _paint(g);
    }


    public void keyTyped(KeyEvent k) {
    }
    public void keyReleased(KeyEvent k) {
        if (k.getKeyCode() == k.VK_ALT) {
            if (drag) {
                drag = false;
                oscale = scale;
                repaint();
            }
        }
    }
    public void keyPressed(KeyEvent k) {
        if (k.getKeyCode() == k.VK_ALT) {
            drag = true;
            dragx = mousex;
            dragy = mousey;
            recenter = new Point2D.Float(dragx, dragy);
            try {
                transform.inverseTransform(recenter, recenter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        char c = k.getKeyChar();
        switch(c) {
            case '+': scale += 0.1; repaint(); return;
            case '-': scale -= 0.1; repaint(); return;
            case 'z': scale = 1.0; recenter = null; repaint(); return;
        }
    }
    public void mouseMoved(MouseEvent m) {
        /*
        Cell oldcell = whichCell(mousex, mousey);
        Cell newcell = whichCell(m.getX(), m.getY());
        System.out.println((oldcell==null ? "old=null" : ("old=" + oldcell._x+","+oldcell._y+"  "))+
                           (newcell==null ? "new=null" : ("new=" + newcell._x+","+newcell._y+"  ")));
        if (oldcell != newcell) {
            if (oldcell != null) oldcell.in = false;
            if (newcell != null) newcell.in = true;
            repaint();
        }
        */
        mousex = m.getX();
        mousey = m.getY();
        if (drag) {
            if (mousey > dragy + 5) {
                scale = 1 / ((1 / oscale) + ((mousey - dragy) / 50.0));
            } else if (mousey < dragy - 5) {
                scale = oscale + ((dragy - mousey) / 50.0);
            } else {
                scale = oscale;
            }
            repaint();
        }
    }

   
    public boolean isFocusable() { return true; }

    public void mouseDragged(MouseEvent m) {
        mouseMoved(m);
        dragx = m.getX();
        dragy = m.getY();
    }
    public void mousePressed(MouseEvent m) {
        recenter = new Point2D.Float(m.getX(), m.getY());
        try {
            transform.transform(recenter, recenter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        repaint();
    }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseClicked(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
}
