package edu.berkeley.slipway.gui;

import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static com.atmel.fpslic.FpslicUtil.*;
import edu.berkeley.slipway.*;
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

    private FtdiBoard ftdiboard;

    public ZoomingPanel(FtdiBoard ftdiboard) {
        this.ftdiboard = ftdiboard;
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
        shiftkey = (k.getModifiers() & k.SHIFT_MASK) != 0;
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
    public int keyMode;
    public void keyPressed(KeyEvent k) {
        shiftkey = (k.getModifiers() & k.SHIFT_MASK) != 0;
        keyPressed0(k);
        char c = k.getKeyChar();
        if (c=='q') c = 'y';
        lastChar = c;
    }
    public boolean shiftkey = false;
    public void keyPressed0(KeyEvent k) {
        repaint();
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
        Fpslic.Cell c = cell == null ? null : cell.cell;
        if ((k.getModifiers() & k.ALT_MASK) != 0 || (k.getModifiers() & k.META_MASK) != 0)
            switch(k.getKeyCode()) {
                case VK_S:
                    writeMode4();
                    break;
                    /*
                case VK_O:
                    readMode4();
                    break;
                    */
                case VK_0: {
                    c.xlut(0x00);
                    c.ylut(0x00);
                    repaint();
                    return;
                }
                case VK_1: {
                    c.xlut(0xff);
                    c.ylut(0xff);
                    repaint();
                    return;
                }
                case VK_F: {
                    int save1y = c.fpslic().cell(19,22).ylut();
                    int save1x = c.fpslic().cell(19,22).xlut();
                    int save2y = c.fpslic().cell(20,22).ylut();
                    int save2x = c.fpslic().cell(20,22).xlut();
                    c.fpslic().cell(19,22).ylut(0xff);
                    c.fpslic().cell(19,22).xlut(0xff);
                    for(int i=0; i<800; i++) {
                        c.fpslic().cell(20,22).ylut(0xff);
                        c.fpslic().cell(20,22).xlut(0xff);
                        c.fpslic().flush();
                        c.fpslic().cell(20,22).ylut(0x00);
                        c.fpslic().cell(20,22).xlut(0x00);
                        c.fpslic().flush();
                    }
                    c.fpslic().cell(19,22).ylut(save1y);
                    c.fpslic().cell(19,22).xlut(save1x);
                    c.fpslic().cell(20,22).ylut(save2y);
                    c.fpslic().cell(20,22).xlut(save2x);
                    System.out.println("done");
                    repaint();
                    return;
                }
                case VK_BACK_QUOTE: {
                    c.xlut(0xff);
                    c.ylut(0xff);
                    repaint();
                    return;
                }
                case VK_A: {
                    c.xlut(LUT_SELF & LUT_OTHER);
                    c.ylut(LUT_SELF & LUT_OTHER);
                    repaint();
                    return;
                }
                case VK_2:
                case VK_G:
                    {
                    c.generalized_c_element();
                    repaint();
                    return;
                }
                case VK_3: {
                    c.generalized_c_element();
                    c.xlut((LUT_SELF & ~LUT_OTHER) | (LUT_Z & ~LUT_OTHER) | (LUT_Z & LUT_SELF & LUT_OTHER));
                    c.ylut(LUT_Z);
                    c.c(XLUT);
                    repaint();
                    return;
                }
                case VK_Z: {
                    c.xlut(LUT_Z);
                    c.ylut(LUT_Z);
                    repaint();
                    return;
                }
                case VK_W: {
                    c.xlut(LUT_Z);
                    c.ylut(LUT_SELF);
                    c.t(TMUX_W);
                    repaint();
                    return;
                }
                case VK_T: {
                    //c.t(;
                    repaint();
                    return;
                }
                case VK_O: {
                    c.xlut(LUT_SELF | LUT_OTHER);
                    c.ylut(LUT_SELF | LUT_OTHER);
                    repaint();
                    return;
                }
                case VK_X: {
                    c.xlut(LUT_SELF);
                    c.ylut(LUT_OTHER);
                    repaint();
                    return;
                }
                case VK_Y: {
                    c.xlut(LUT_OTHER);
                    c.ylut(LUT_SELF);
                    repaint();
                    return;
                }
                case VK_I: {
                    System.out.println("interrupt count => " + ftdiboard.readCount());
                    repaint();
                    return;
                }
            }

        else switch(k.getKeyCode()) {
            case VK_ESCAPE: scale = 1.0; recenter = null; repaint(); return;

            case VK_BACK_QUOTE: case VK_0: case VK_1: case VK_2: case VK_3: case VK_4: {
                int i = k.getKeyCode()==VK_BACK_QUOTE ? NONE : (L0 + (k.getKeyChar() - '0'));
                switch(lastChar) {
                    case 'x': c.xi(i); break;
                    case 'y': case 'q': c.yi(i); break;
                    case 'w': c.wi(i); break;
                    case 'z': c.zi(i); break;
                    case 'o': c.out(i, !c.out(i)); break;
                    case 'h': c.h(i, !c.hx(i)); break;
                    case 'v': c.v(i, !c.vx(i)); break;
                }
                repaint();
                return;
                }

            case VK_W: if (lastChar == 'w') c.wi(NONE); repaint(); return;
            case VK_X: if (lastChar == 'x') c.xi(NONE); repaint(); return;
            case VK_Y: if (lastChar == 'y') c.yi(NONE); repaint(); return;
            case VK_Z: if (lastChar == 'z') c.zi(NONE); repaint(); return;
            case VK_Q: if (lastChar == 'q') c.yi(NONE); repaint(); return;

            case VK_C:
                if (lastChar == 'x') { c.xo(true); repaint(); return; }
                if (lastChar == 'y' || lastChar == 'q') { c.yo(true); repaint(); return; }
                switch(c.c()) {
                    case XLUT: c.c(YLUT); break;
                    case YLUT: c.c(ZMUX); break;
                    case ZMUX: c.c(XLUT); break;
                    default:   c.c(ZMUX); break;
                }
                repaint();
                return;

            case VK_F: c.f(!c.f()); repaint(); return;
            case VK_S:
                Gui.Cell gc = whichCell(mousex,mousey);
                gc.scanme = !gc.scanme;
                //case VK_S: c.ff_reset_value(!c.ff_reset_value()); repaint(); return;
            case VK_R: c.b(!c.b()); repaint(); return;

            case VK_T:
                switch(c.t()) {
                    case TMUX_FB:
                    case TMUX_W_AND_FB:
                        c.t(TMUX_W_AND_Z);
                        break;

                    case TMUX_Z:
                    case TMUX_W_AND_Z:
                        c.t(TMUX_W);
                        break;

                    case TMUX_W:
                        c.t(TMUX_W_AND_FB);
                        break;
                }
                repaint();
                return;

            case VK_E:
                switch(c.oe()) {
                    case H4:   c.oe(V4); break;
                    case V4:   c.oe(NONE); break;
                    case NONE: c.oe(H4); break;
                }
                repaint();
                return;

                // ff reset polarity
                // h0..h5, v0..v5
                // xi=0..5
                // yi=0..5
                // zi=0..5
                // wi=0..5
                // t-mux
                
        }
    }

    public Gui.Cell oldcell = null;
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
        //dragx = m.getX();
        //dragy = m.getY();

    }
    public abstract void pressed();
    public abstract void released();
    public void mousePressed(MouseEvent m) {
        mousebutton = true;

        /*
        recenter = new Point2D.Float(m.getX(), m.getY());
        try {
            transform.transform(recenter, recenter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        repaint();
        */
        pressed();
    }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseClicked(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) {
        mousebutton = false;
        released();
    }
    public boolean mousebutton = false;
    public boolean xkey = false;
    public boolean ykey = false;

    HashSet<Gui.Cell> cells = new HashSet<Gui.Cell>();
    public abstract void scan(final Gui.Cell c);
    public abstract void scan();
    public abstract void writeMode4();
    public abstract void readMode4();
}
