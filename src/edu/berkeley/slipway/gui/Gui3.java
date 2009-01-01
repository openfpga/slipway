package edu.berkeley.slipway.gui;
// gui: use colors to distinguish planes?  dot-dash lines?


import com.atmel.fpslic.*;
import edu.berkeley.slipway.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static java.awt.event.KeyEvent.*;
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

public class Gui3 extends Canvas implements MouseWheelListener, MouseMotionListener, KeyListener {

    FpslicDevice at40k;
    SlipwayBoard slipway;

    private int width;
    private int height;
    private int magnify = 0;
    public GuiCell[][] ca = new GuiCell[128][];
    private SlipwayBoard ftdiboard;
    public Gui3(FpslicDevice at40k, SlipwayBoard slipway) {
        this(at40k, slipway, 24, 24);
    }
    public Gui3(FpslicDevice at40k, SlipwayBoard slipway, int width, int height) {
        this.at40k = at40k;
        this.slipway = slipway;
        this.width = width;
        this.height = height;
        for(int i=0; i<ca.length; i++)
            ca[i] = new GuiCell[128];
        for(int x=0; x<width; x++)
            for(int y=0; y<height; y++)
                ca[x][y] = new GuiCell(at40k.cell(x, y));
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        magnify -= e.getWheelRotation();
        repaint();
    }

    FpslicDevice.Cell selectedCell = null;
    public void _paint(Graphics2D g_) {
        int SIZE = 100;
        //g_.setStroke(new BasicStroke((float)1.0/SIZE));
        g_.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g_.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        G g = new G(g_);
        g.pushTransform();
        AffineTransform at = new AffineTransform();
        at.translate(getWidth()/2, getHeight()/2);
        at.scale(1, -1);
        double mag = magnify;
        if (magnify > 0) {
            mag -= 1;
            mag /= 5;
            mag += 1;
            at.scale(mag, mag);
        } else if (magnify < 0) {
            mag += 1;
            mag /= 5;
            mag -= 1;
            at.scale(-1.0/mag, -1.0/mag);
        }
        at.translate(-(width*SIZE)/4, -(height*SIZE)/4);
        g.g.transform(at);
        for(int x=0; x<ca.length; x++)
            for(int y=0; y<ca[x].length; y++) {
                R r = new R(SIZE*x,     SIZE*y,
                            SIZE*(x+1), SIZE*(y+1));
                if (ca[x][y] != null) {
                    g.color(GuiCell.BORDER_COLOR);
                    r.draw(g);
                    repaint();
                }
            }
        P mouse = new P(mousex, mousey);
        mouse = mouse.inverseTransform(at);
        selectedCell = null;
        for(int x=0; x<ca.length; x++)
            for(int y=0; y<ca[x].length; y++) {
                R r = new R(SIZE*x,     SIZE*y,
                            SIZE*(x+1), SIZE*(y+1));
                if (ca[x][y] != null) {
                    if (r.contains(mouse)) selectedCell = ca[x][y].fpslicCell;
                    ca[x][y].draw(g, r, r.contains(mouse));
                }
            }
        at = g.getTransform();
        g.popTransform();

        R statusArea = new R(0, getHeight() - 150, getWidth(), getHeight());
        g.color(0x0);
        statusArea.fill(g);

        double keyboardRatio = ((double)keyboard1.getWidth(null)) / ((double)keyboard1.getHeight(null));
        g.g.drawImage(keyboard1,
                      (int)statusArea.minx(),
                      (int)statusArea.miny(),
                      (int)((keyboardRatio * 150)),
                      (int)(150),
                      null);

        statusArea = statusArea.plus(keyboardRatio * 150 + 10, 0, 0, 0);
        Inspector.draw(g, statusArea, selectedCell);

        // map
        R map = new R(getWidth() - 150, getHeight() - 150, getWidth(), getHeight());
        map = map.plus(5, 5, -5, -5);
        double mapw = map.width() / width;
        double maph = map.height() / height;
        P p1 = new P(0, 0).inverseTransform(at);
        P p2 = new P(getWidth(), getHeight()-150).inverseTransform(at);
        p1 = p1.scale(map.width() / (SIZE*width));
        p2 = p2.scale(map.width() / (SIZE*width));
        R rv = new R(map.minx() + p1.x,
                     map.maxy() - p1.y,
                     map.minx() + p2.x,
                     map.maxy() - p2.y);
        for(int x=0; x<width; x++)
            for(int y=0; y<height; y++) {
                R rc = new R(map.minx() + x * mapw,
                             map.miny() + y * maph,
                             map.minx() + (x+1) * mapw - 1,
                             map.miny() + (y+1) * maph - 1);
                if      (selectedCell != null && selectedCell.row==(height-y-1) && selectedCell.col==x) g.color(0xffff00);
                else if (rc.within(rv))  g.color(0x006600);
                else                     g.color(0x444444);
                rc.fill(g);
            }
        g.color(0x00ff00);
        rv.draw(g);
    }

    public void paint(Graphics g) { _paint((Graphics2D)g); }
    public void mouseDragged(MouseEvent e) { mouseMoved(e); }
    public void mouseMoved(MouseEvent e) {
        mousex = e.getX();
        mousey = e.getY();
    }
    int mousex;
    int mousey;

    private static Image keyboard1 =
        Toolkit.getDefaultToolkit().createImage("images/keyboard1.png");
    private static Image keyboard2 =
        Toolkit.getDefaultToolkit().createImage("images/keyboard2.png");
    private static Image keyboard3 =
        Toolkit.getDefaultToolkit().createImage("images/keyboard3.png");

    private boolean[] keys = new boolean[1024];
    public void keyTyped(KeyEvent k) { }
    public void keyReleased(KeyEvent k) {
        keys[k.getKeyCode()] = false;
    }
    public void keyPressed(KeyEvent k) {
        synchronized(this) {
        keys[k.getKeyCode()] = true;
        switch(k.getKeyCode()) {
            case VK_1: {
                if (selectedCell != null) {
                    selectedCell.ylut(0xff);
                }
                break;
            }
            case VK_0: {
                if (selectedCell != null) {
                    selectedCell.ylut(0x00);
                }
                break;
            }
            case VK_C: {
                if (selectedCell != null) {
                    selectedCell.ylut((LUT_SELF & ~LUT_OTHER) |
                                      (LUT_Z    & ~LUT_OTHER) |
                                      (LUT_Z    &   LUT_SELF));
                }
                break;
            }
                /*
            case VK_0: case VK_1: case VK_2: case VK_3: case VK_4: {
                if (selectedCell != null) {
                    int plane = k.getKeyCode() - VK_0;
                    if      (keys[VK_X]) selectedCell.xi(plane+L0);
                    else if (keys[VK_Y]) selectedCell.yi(plane+L0);
                    else if (keys[VK_O]) selectedCell.out(plane+L0, !selectedCell.out(plane+L0));
                }
                break;
            }
                */
        }
        }
    }
}
