package edu.berkeley.slipway.gui;

import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;

public class GuiCell {
    
    public static final int BORDER_COLOR      = 0x00BBBBBB;
    public static final int BODY_COLOR        = 0x00555555;
    public static final int GLOBAL_WIRE_COLOR = 0x00008000;
    public static final int DRIVEN_WIRE_COLOR = 0x00ff0000;
    public static final int LOCAL_WIRE_COLOR  = 0x000FF000;
    public static final int XGATE_COLOR       = 0x00800000;
    public static final int YGATE_COLOR       = 0x00000080;

    private static final int LOCAL_ROUTING_CHANNEL_WIDTH = 7;

    public boolean val = false;
    public final FpslicDevice.Cell fpslicCell;
    private GuiGate xgate = new GuiGate(this);
    private GuiGate ygate = new GuiGate(this);

    R gateArea = null;

    public GuiCell(FpslicDevice.Cell fpslicCell) {
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
            xgate.disabled = !fpslicCell.xlut_relevant();
            ygate.disabled = !fpslicCell.ylut_relevant();
            drawBody(g, r);
        }
    }

    private void drawGlobalRouting(G g, R r) {
        for(int i=1; i<6; i++) {
            g.color(fpslicCell.out(L0 + i-1) && fpslicCell.vx(L0+i-1)
                    ? DRIVEN_WIRE_COLOR
                    : GLOBAL_WIRE_COLOR);
            g.line(r.corner(SW).translate(2*i,0), r.corner(NW).translate(2*i,0));
            g.color(fpslicCell.out(L0 + i-1) && fpslicCell.hx(L0+i-1)
                    ? DRIVEN_WIRE_COLOR
                    : GLOBAL_WIRE_COLOR);
            g.line(r.corner(SW).translate(0,2*i), r.corner(SE).translate(0,2*i));
        }
        
    }

    private void drawBody(G g, R r) {
        if (xgate.disabled && ygate.disabled) return;

        gateArea =
            r.plus(12+LOCAL_ROUTING_CHANNEL_WIDTH,
                   12+LOCAL_ROUTING_CHANNEL_WIDTH,
                   -(4+LOCAL_ROUTING_CHANNEL_WIDTH),
                   -(4+LOCAL_ROUTING_CHANNEL_WIDTH));

        double factor = gateArea.width()/2;
        xgate.r = xgate.disabled || ygate.disabled ? gateArea : gateArea.plus(factor, 0, 0, -factor);
        ygate.r = xgate.disabled || ygate.disabled ? gateArea : gateArea.plus(0, 0, -factor, -factor);

        P xip   = r.corner(fpslicCell.xi());
        P yip   = r.corner(fpslicCell.yi());
        R xring = gateArea.plus(-4, -4, 4, 4);
        R yring = gateArea.plus(-6, -6, 6, 6);
        xgate.rotation(fpslicCell.yi());
        ygate.rotation(fpslicCell.yi());
        if (xip != null) xgate.route(g, xip, xring, 0, XGATE_COLOR);
        if (yip != null) xgate.route(g, yip, yring, 2, YGATE_COLOR);
        if (xip != null) ygate.route(g, xip, xring, 2, XGATE_COLOR);
        if (yip != null) ygate.route(g, yip, yring, 0, YGATE_COLOR);

        drawWin(g, r);
        drawGates(g);
    }

    private void drawWin(G g, R r) {
        int layer = fpslicCell.wi() - L0;
        P   wip   = r.corner(SW).translate(2*(layer+1), 2*(layer+1));
        R   wring = r.plus(2*(layer+1), 2*(layer+1), -2*(layer+1), -2*(layer+1));
        ygate.route(g, wip, wring, 1, LOCAL_WIRE_COLOR);
        xgate.route(g, wip, wring, 1, LOCAL_WIRE_COLOR);
    }

    private void drawGates(G g) {
        xgate.draw(g, XGATE_COLOR, val ? XGATE_COLOR : 0xffffff);
        ygate.draw(g, YGATE_COLOR, val ? YGATE_COLOR : 0xffffff);
    }

}