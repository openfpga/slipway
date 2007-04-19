package edu.berkeley.slipway.gui;

import com.atmel.fpslic.*;
import edu.berkeley.slipway.*;
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
import static edu.berkeley.slipway.gui.GuiConstants.*;

public class Gui3 extends Canvas implements MouseWheelListener, MouseMotionListener {

    Fpslic at40k;
    FtdiBoard drone;

    private int width;
    private int height;
    private int magnify = 0;
    private GuiCell[][] ca = new GuiCell[128][];
    private FtdiBoard ftdiboard;
    public Gui3(Fpslic at40k, FtdiBoard drone) {
        this(at40k, drone, 24, 24);
    }
    public Gui3(Fpslic at40k, FtdiBoard drone, int width, int height) {
        this.at40k = at40k;
        this.drone = drone;
        this.width = width;
        this.height = height;
        for(int i=0; i<ca.length; i++)
            ca[i] = new GuiCell[128];
        for(int x=0; x<width; x++)
            for(int y=0; y<height; y++)
                ca[x][y] = new GuiCell(at40k.cell(x, y));
        addMouseWheelListener(this);
        addMouseMotionListener(this);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        magnify -= e.getWheelRotation();
        repaint();
    }

    public void _paint(Graphics2D g_) {
        int SIZE = 100;
        //g_.setStroke(new BasicStroke((float)1.0/SIZE));
        g_.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g_.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        G g = new G(g_);
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
        for(int x=0; x<ca.length; x++)
            for(int y=0; y<ca[x].length; y++) {
                R r = new R(SIZE*x,     SIZE*y,
                            SIZE*(x+1), SIZE*(y+1));
                if (ca[x][y] != null)
                    ca[x][y].draw(g, r, r.contains(mouse));
            }
    }

    public void paint(Graphics g) { _paint((Graphics2D)g); }
    public void mouseDragged(MouseEvent e) { mouseMoved(e); }
    public void mouseMoved(MouseEvent e) {
        mousex = e.getX();
        mousey = e.getY();
    }
    int mousex;
    int mousey;
}
