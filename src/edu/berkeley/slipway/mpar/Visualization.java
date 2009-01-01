package edu.berkeley.slipway.mpar;
import com.atmel.fpslic.*;
import java.awt.*;
import java.awt.event.*;
import byucc.edif.tools.merge.*;
import byucc.edif.*;
import java.io.*;
import java.util.*;
import edu.berkeley.slipway.*;
import edu.berkeley.abits.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static edu.berkeley.slipway.mpar.PhysicalFpslic.*;

public class Visualization extends Frame implements MouseMotionListener, MouseListener {
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mousePressed(MouseEvent e){ }
    public void mouseReleased(MouseEvent e)  { }
    public void mouseDragged(MouseEvent e) { }
    public void mouseClicked(MouseEvent e) {
        MPARDemo.temperature += 0.05;
    }
    public void mouseMoved(MouseEvent e) {
        PhysicalFpslic.badx = (e.getX() / CELLSEP)-1;
        PhysicalFpslic.bady = (getHeight()-e.getY()) / CELLSEP;
    }

    public static final Color MOVED = new Color(0x88, 0x88, 0x88);
    public static final Color PLAIN = new Color(0xff, 0x88, 0x88);
    public static final Color EMPTY = new Color(0x88, 0xff, 0x88);

    private final PhysicalFpslic pd;

    public Visualization(PhysicalFpslic pd) {
        this.pd = pd;
        setSize(CELLSEP*(pd.width+2), CELLSEP*(pd.height+2));
        show();
        setSize(CELLSEP*(pd.width+2), CELLSEP*(pd.height+2));
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void draw(Placement placement, Routing routing, boolean drawRouting) {
        Graphics2D g = (Graphics2D)getGraphics();
        g.translate(CELLSEP,CELLSEP);
        g.setColor(Color.white);
        g.fillRect(-CELLSEP, -CELLSEP, getWidth(), getHeight());
        g.setColor(Color.black);
        g.drawString("temperature = " + MPARDemo.temperature,    10, 0);
        g.drawString("congestion  = " + MPARDemo.congestion,     10, 15);
        g.drawString("slack = " + (-1 * MPARDemo.timingpenalty), 10, 30);
        for(int x=0; x<pd.width; x++)
            for(int y=0; y<pd.height; y++) {
                double d = Math.sqrt( (PhysicalFpslic.badx-x)*
                                      (PhysicalFpslic.badx-x)+
                                      (PhysicalFpslic.bady-y)*
                                      (PhysicalFpslic.bady-y) );
                d = (PhysicalFpslic.badr - d);
                if (d<0) d = 0;
                d /= PhysicalFpslic.badr;
                int a = 255 - ((int)(255*d));
                g.setColor(new Color(a, a, a));
                g.fillRect((                         x*CELLSEP - (PLANESEP*3)),
                           ((pd.height * CELLSEP) - (y*CELLSEP + (PLANESEP*3))),
                           CELLSEP,
                           CELLSEP);
            }
        if (drawRouting) {
            routing.draw(g);
        }
        for(int x=0; x<pd.width; x++)
            for(int y=0; y<pd.height; y++) {
                if (placement.cellToNode(pd.getCell(x,y)) != null)
                    g.setColor(PLAIN);
                else
                    g.setColor(EMPTY);
                g.fillRect((                       x*CELLSEP)+2,
                           ((pd.height * CELLSEP) - (y*CELLSEP))+2,
                           CELLSEP-5*PLANESEP-2,
                           CELLSEP-5*PLANESEP-2);
            }
    }

}