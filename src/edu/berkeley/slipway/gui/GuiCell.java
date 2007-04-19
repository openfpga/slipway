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
        R body = r;
        g.color(BODY_COLOR);
        R xgater = null;
        R ygater = null;
        int N = 7;
        R gateArea = body.plus(12+N, 12+N, -(4+N), -(4+N));
        if      (xgate==null) ygater = gateArea;
        else if (ygate==null) xgater = gateArea;
        else {
            double factor = gateArea.width()/2;
            xgater = gateArea.plus(0, 0, -factor, -factor);
            ygater = gateArea.plus(factor, 0, 0, -factor);
        }

        R xring = gateArea.plus(4, 4, -4, -4);
        R yring = gateArea.plus(6, 6, -6, -6);

        int rot = 0;
        switch (fpslicCell.yi()) {
            case NORTH: rot = 2; break;
            case SOUTH: rot = 0; break;
            case WEST:  rot = 3; break;
            case EAST:  rot = 1; break;
            default:    rot = 0;
        }
        if (xgate != null) {
            xgate.rotation = rot;
            xgate.gateArea = gateArea;
        }
        if (ygate != null) {
            ygate.rotation = rot;
            ygate.gateArea = gateArea;
        }

        int TSIZE   = 10;
        double TSQR = TSIZE / Math.sqrt(2.0);
        g.color(XGATE_COLOR);
        P p = null;
        switch (fpslicCell.xi()) {
            case SW: p = new P(r.minx(), r.miny()); break;
            case SE: p = new P(r.maxx(), r.miny()); break;
            case NW: p = new P(r.minx(), r.maxy()); break;
            case NE: p = new P(r.maxx(), r.maxy()); break;
        }
        if (p!=null) {
            if (ygate != null) {
                g.route(p, xring, ygate.getInput(1, ygater));
                g.line(ygate.getInput(1, ygater), ygate.getInputDest(1, ygater));
            }
            if (xgate != null) {
                g.route(p, xring, xgate.getInput(0, xgater));
                g.line(xgate.getInput(0, xgater), xgate.getInputDest(0, xgater));
            }
        }

        p = null;
        g.color(YGATE_COLOR);
        switch (fpslicCell.yi()) {
            case NORTH: p = new P(r.cx(), r.maxy()); break;
            case SOUTH: p = new P(r.cx(), r.miny()); break;
            case WEST:  p = new P(r.minx(), r.cy()); break;
            case EAST:  p = new P(r.maxx(), r.cy()); break;
        }
        if (p!=null) {
            if (ygate != null) {
                g.route(p, yring, ygate.getInput(0, ygater));
                g.line(ygate.getInput(0, ygater), ygate.getInputDest(0, ygater));
            }
            if (xgate != null) {
                g.route(p, yring, xgate.getInput(1, xgater));
                g.line(xgate.getInput(1, xgater), xgate.getInputDest(1, xgater));
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
                g.route(p2, r2, xgate.getInput(3, xgater));
            }
            */
            if (fpslicCell.wi() != NONE) {
                g.color(LOCAL_WIRE_COLOR);
                int layer = fpslicCell.wi() - L0;
                P p2 = new P(r.minx()+2*(layer+1), r.miny()+2*(layer+1));
                R r2 = new R(r.minx()+2*(layer+1), r.miny()+2*(layer+1),
                             r.maxx()-2*(layer+1), r.maxy()-2*(layer+1));
                g.route(p2, r2, xgate.getInput(2, xgater));
                g.line(xgate.getInput(2, xgater), xgate.getInputDest(2, xgater));
            }
            xgate.draw(g, xgater, XGATE_COLOR);
        }
        if (ygater != null) {
            /*
            if (fpslicCell.zi() != NONE) {
                g.color(LOCAL_WIRE_COLOR);
                int layer = fpslicCell.zi() - L0;
                P p2 = new P(r.minx()+2*(layer+1), r.miny()+2*(layer+1));
                R r2 = new R(r.minx()+2*(layer+1), r.miny()+2*(layer+1),
                             r.maxx()-2*(layer+1), r.maxy()-2*(layer+1));
                g.route(p2, r2, ygate.getInput(3, ygater));
            }
            */
            if (fpslicCell.wi() != NONE) {
                g.color(LOCAL_WIRE_COLOR);
                int layer = fpslicCell.wi() - L0;
                P p2 = new P(r.minx()+2*(layer+1), r.miny()+2*(layer+1));
                R r2 = new R(r.minx()+2*(layer+1), r.miny()+2*(layer+1),
                             r.maxx()-2*(layer+1), r.maxy()-2*(layer+1));
                g.route(p2, r2, ygate.getInput(2, ygater));
                g.line(ygate.getInput(2, ygater), ygate.getInputDest(2, ygater));
            }
            ygate.draw(g, ygater, YGATE_COLOR);
        }
    }
}