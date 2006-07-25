package edu.berkeley.obits.gui;

import static edu.berkeley.obits.device.atmel.AtmelDevice.Constants.*;
import static edu.berkeley.obits.device.atmel.AtmelDevice.Util.*;
import edu.berkeley.obits.*;
import edu.berkeley.obits.device.atmel.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import static java.awt.event.KeyEvent.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public abstract class ZoomingPanel extends JComponent implements KeyListener, MouseMotionListener, MouseListener {

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
        addMouseListener(this);
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
        switch(k.getKeyCode()) {
            case VK_X: xkey = false; return; 
            case VK_Y: ykey = false; return; 
        }
    }
    public char lastChar;
    public void keyPressed(KeyEvent k) {
        keyPressed0(k);
        char c = k.getKeyChar();
        if (c=='q') c = 'y';
        lastChar = c;
    }
    public void keyPressed0(KeyEvent k) {
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
        Gui.Cell cell = whichCell(mousex, mousey);
        At40k.Cell c = cell == null ? null : cell.cell;
        switch(k.getKeyCode()) {
            case VK_PLUS:
                if (xkey) {
                    c.xlut(LUT_SELF | LUT_OTHER | LUT_Z);
                    repaint();
                    return;
                } else if (ykey) {
                    c.ylut(LUT_SELF | LUT_OTHER | LUT_Z);
                    repaint();
                    return;
                }
                scale += 0.1; repaint(); return;
            case VK_MULTIPLY:
                if (xkey) {
                    c.xlut(LUT_SELF & LUT_OTHER & LUT_Z);
                    repaint();
                    return;
                } else if (ykey) {
                    c.ylut(LUT_SELF & LUT_OTHER & LUT_Z);
                    repaint();
                    return;
                }
                scale += 0.1; repaint(); return;
            case VK_MINUS: scale -= 0.1; repaint(); return;
            case VK_ESCAPE: scale = 1.0; recenter = null; repaint(); return;
            case VK_NUMPAD7: whichCell(mousex, mousey).cell.xi(NW); repaint(); return;
            case VK_NUMPAD8: whichCell(mousex, mousey).cell.yi(NORTH); repaint(); return;
            case VK_NUMPAD9: whichCell(mousex, mousey).cell.xi(NE); repaint(); return;
            case VK_NUMPAD4: whichCell(mousex, mousey).cell.yi(WEST); repaint(); return;
            case VK_NUMPAD6: whichCell(mousex, mousey).cell.yi(EAST); repaint(); return;
            case VK_NUMPAD1: whichCell(mousex, mousey).cell.xi(SW); repaint(); return;
            case VK_NUMPAD2: whichCell(mousex, mousey).cell.yi(SOUTH); repaint(); return;
            case VK_NUMPAD3: whichCell(mousex, mousey).cell.xi(SE); repaint(); return;

            case VK_0:
            case VK_1:
            case VK_2:
            case VK_3:
            case VK_4: {
                int i = L0 + (k.getKeyChar() - '0');
                switch(lastChar) {
                    case 'x': c.xi(i); break;
                    case 'y': c.yi(i); break;
                    case 'w': c.wi(i); break;
                    case 'z': c.zi(i); break;
                    case ' ': c.out(i, !c.out(i)); break;
                }
                repaint();
                return;
                }

                //case VK_F: c.t(TMUX_W_AND_FB); repaint(); return;
            case VK_W: c.t(TMUX_W); repaint(); return;
                //case VK_Z: c.t(TMUX_W_AND_Z); repaint(); return;

            case VK_C:
                if (lastChar == 'x') { c.xo(true); repaint(); return; }
                if (lastChar == 'y') { c.yo(true); repaint(); return; }
                switch(c.c()) {
                    case XLUT: c.c(YLUT); break;
                    case YLUT: c.c(ZMUX); break;
                    case ZMUX: c.c(XLUT); break;
                    default:   c.c(ZMUX); break;
                }
                repaint();
                return;

            case VK_F: c.f(!c.f()); repaint(); return;
            case VK_B: c.b(!c.b()); repaint(); return;

            case VK_X:
                if (lastChar == 'x') { c.xo(false); repaint(); return; }
                xkey = true;
                return; 

            case VK_Q:
            case VK_Y:
                if (lastChar == 'y') { c.yo(false); repaint(); return; }
                ykey = true;
                return; 

            case VK_R:
                boolean reg = c.f();
                c.f(!reg);
                c.b(!reg);
                repaint();
                return;

            case VK_SPACE:
                scan();
                break;

            case VK_S:
                writeMode4();
                break;
            case VK_L:
                readMode4();
                break;

            case VK_O:
                switch(c.oe()) {
                    case H4:   c.oe(V4); break;
                    case V4:   c.oe(NONE); break;
                    case NONE: c.oe(H4); break;
                }
                repaint();
                return;

                // xlut table
                // ylut table
                // ff reset polarity
                // h0..h5, v0..v5
                // xi=0..5
                // yi=0..5
                // zi=0..5
                // wi=0..5
                // t-mux
        }
    }

    Gui.Cell oldcell = null;
    public abstract Gui.Cell whichCell(int x, int y);
    public void mouseMoved(MouseEvent m) {

        Gui.Cell newcell = whichCell(m.getX(), m.getY());
        /*
        System.out.println((oldcell==null ? "old=null" : ("old=" + oldcell._x+","+oldcell._y+"  "))+
                           (newcell==null ? "new=null" : ("new=" + newcell._x+","+newcell._y+"  ")));
        */
        if (oldcell != newcell) {
            if (oldcell != null) oldcell.in = false;
            if (newcell != null) newcell.in = true;
            repaint();
        }
        oldcell = newcell;

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
        /*
        recenter = new Point2D.Float(m.getX(), m.getY());
        try {
            transform.transform(recenter, recenter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        repaint();
        */
    }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseClicked(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) {
        
    }

    public boolean xkey = false;
    public boolean ykey = false;

    HashSet<Gui.Cell> cells = new HashSet<Gui.Cell>();
    public abstract void scan(final Gui.Cell c);
    public abstract void scan();
    public abstract void writeMode4();
    public abstract void readMode4();
}
