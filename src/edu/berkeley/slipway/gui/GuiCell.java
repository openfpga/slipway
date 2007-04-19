package edu.berkeley.slipway.gui;

import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;

public class GuiCell {
    
    public static final int BORDER_COLOR      = 0x00BBBBBB;
    public static final int BODY_COLOR        = 0x00555555;
    public static final int GLOBAL_WIRE_COLOR = 0x00008000;
    public static final int LOCAL_WIRE_COLOR  = 0x000FF000;
    public static final int XGATE_COLOR       = 0x00800000;
    public static final int YGATE_COLOR       = 0x00000080;
    double MAIN_AREA = 0.75;

    private final Fpslic.Cell fpslicCell;
    private GuiGate xgate = null;
    private GuiGate ygate = null;

    public GuiCell(Fpslic.Cell fpslicCell) {
        this.fpslicCell = fpslicCell;
    }

    /**
     *  The graphics context argument must already be translated such
     *  that the space allocated to this cell is from (-1,-1) to (1,1)
     */
    public void draw(G g, R r, boolean selected) {
        if (selected) {
            g.color(0x00555555);
            r.fill(g);
        }
        drawGlobalRouting(g, r);
        if (fpslicCell.relevant()) {
            xgate = fpslicCell.xlut_relevant() ? new GuiGate() : null;
            ygate = fpslicCell.ylut_relevant() ? new GuiGate() : null;
            drawBody(g, r);
        }
    }

    private void drawGlobalRouting(G g, R r) {
        g.color(GLOBAL_WIRE_COLOR);
        for(int i=1; i<6; i++) {
            g.line(r.minx() + 2*i, r.miny(),
                   r.minx() + 2*i, r.maxy());
            g.line(r.minx(),       r.miny() + 2*i,
                   r.maxx(),       r.miny() + 2*i);
        }
    }

    private void drawBody(G g, R r) {
        if (xgate == null && ygate == null) return;

        int N = 7;
        int TSIZE   = 10;
        double TSQR = TSIZE / Math.sqrt(2.0);

        R body = r;
        g.color(BODY_COLOR);
        R xgater = null;
        R ygater = null;
        R gateArea = body.plus(12+N, 12+N, -(4+N), -(4+N));
        if      (xgate==null) ygater = gateArea;
        else if (ygate==null) xgater = gateArea;
        else {
            double factor = gateArea.width()/2;
            xgater = gateArea.plus(0, 0, -factor, -factor);
            ygater = gateArea.plus(factor, 0, 0, -factor);
        }

        R xring = gateArea.plus(-4, -4, 4, 4);
        R yring = gateArea.plus(-6, -6, 6, 6);
        if (xgate != null) {
            xgate.rotation(fpslicCell.yi());
            xgate.gateArea = gateArea;
            xgate.r = xgater;
        }
        if (ygate != null) {
            ygate.rotation(fpslicCell.yi());
            ygate.gateArea = gateArea;
            ygate.r = ygater;
        }

        g.color(XGATE_COLOR);
        P p = r.corner(fpslicCell.xi());
        if (p!=null) {
            if (ygate != null) {
                g.route(p, xring, ygate.getInput(1));
                g.line(ygate.getInput(1), ygate.getInputDest(1));
            }
            if (xgate != null) {
                g.route(p, xring, xgate.getInput(0));
                g.line(xgate.getInput(0), xgate.getInputDest(0));
            }
        }

        p = r.corner(fpslicCell.yi());
        g.color(YGATE_COLOR);
        if (p!=null) {
            if (ygate != null) {
                g.route(p, yring, ygate.getInput(0));
                g.line(ygate.getInput(0), ygate.getInputDest(0));
            }
            if (xgate != null) {
                g.route(p, yring, xgate.getInput(1));
                g.line(xgate.getInput(1), xgate.getInputDest(1));
            }
        }

        if (xgater != null) {
            /*
            if (fpslicCell.zi() != NONE) {
                g.color(LOCAL_WIRE_COLOR);
                int layer = fpslicCell.zi() - L0;
                P p2 = new P(r.minx()+2*(layer+1), r.miny()+2*(layer+1));
                R r2 = new R(r.minx()+2*(layer+1), r.miny()+2*(layer+1),
                             r.maxx()-2*(layer+1), r.maxy()-2*(layer+1));
                g.route(p2, r2, xgate.getInput(3));
            }
            */
            if (fpslicCell.wi() != NONE) {
                g.color(LOCAL_WIRE_COLOR);
                int layer = fpslicCell.wi() - L0;
                P p2 = new P(r.minx()+2*(layer+1), r.miny()+2*(layer+1));
                R r2 = new R(r.minx()+2*(layer+1), r.miny()+2*(layer+1),
                             r.maxx()-2*(layer+1), r.maxy()-2*(layer+1));
                g.route(p2, r2, xgate.getInput(2));
                g.line(xgate.getInput(2), xgate.getInputDest(2));
            }
            xgate.draw(g, XGATE_COLOR);
        }
        if (ygater != null) {
            /*
            if (fpslicCell.zi() != NONE) {
                g.color(LOCAL_WIRE_COLOR);
                int layer = fpslicCell.zi() - L0;
                P p2 = new P(r.minx()+2*(layer+1), r.miny()+2*(layer+1));
                R r2 = new R(r.minx()+2*(layer+1), r.miny()+2*(layer+1),
                             r.maxx()-2*(layer+1), r.maxy()-2*(layer+1));
                g.route(p2, r2, ygate.getInput(3));
            }
            */
            if (fpslicCell.wi() != NONE) {
                g.color(LOCAL_WIRE_COLOR);
                int layer = fpslicCell.wi() - L0;
                P p2 = new P(r.minx()+2*(layer+1), r.miny()+2*(layer+1));
                R r2 = new R(r.minx()+2*(layer+1), r.miny()+2*(layer+1),
                             r.maxx()-2*(layer+1), r.maxy()-2*(layer+1));
                g.route(p2, r2, ygate.getInput(2));
                g.line(ygate.getInput(2), ygate.getInputDest(2));
            }
            ygate.draw(g, YGATE_COLOR);
        }
    }
}