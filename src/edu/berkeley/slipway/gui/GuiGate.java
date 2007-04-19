package edu.berkeley.slipway.gui;

import com.atmel.fpslic.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.color.*;
import static com.atmel.fpslic.FpslicConstants.*;

public class GuiGate {

    private GeneralPath gp = new GeneralPath();

    int rotation = 1;
    R gateArea;
    R r;
    boolean disabled = false;

    public GuiGate() {
        gp.moveTo(29.141f, 36.301f);
        gp.lineTo(29.141f, 36.301f-7.161f);
        gp.curveTo(27.71f, 11.24f, 23.413f, 9.45f, 14.82f, 0.5f);
        gp.curveTo(6.229f, 9.45f, 1.932f, 11.24f, 0.5f, 29.141f);
        gp.lineTo(0.5f, 29.141f+7.161f);
        float x = 0.5f;
        float y = 29.141f+7.161f;
        gp.curveTo(5.729f+x, -1.789f+y, 6.444f+x, -2.686f+y, 14.32f+x, -3.58f+y);
        gp.curveTo(22.697f, 33.616f, 23.413f, 34.512f, 29.141f, 36.301f);
        double minx   = gp.getBounds2D().getMinX();
        double miny   = gp.getBounds2D().getMinY();
        gp.transform(AffineTransform.getTranslateInstance(-minx, -miny));
        double width  = gp.getBounds2D().getWidth();
        double height = gp.getBounds2D().getHeight();
        double factor = Math.max(width, height);
        gp.transform(AffineTransform.getTranslateInstance(-width/2, -height/2));
        gp.transform(AffineTransform.getScaleInstance(1.0/factor, -1.0/factor));
    }

    public void draw(G g, int color) {
        if (disabled) return;
        g.pushTransform();
        g.g.translate(gateArea.cx(), gateArea.cy());
        g.g.rotate((2 * Math.PI * rotation)/4);
        g.g.translate(-1 * gateArea.cx(), -1 * gateArea.cy());
        AffineTransform at = AffineTransform.getTranslateInstance(r.cx(), r.cy());
        at.scale(r.getWidth(), r.getHeight());
        g.color(0xffffffff);
        g.g.fill(gp.createTransformedShape(at));
        g.color(color);
        g.g.draw(gp.createTransformedShape(at));
        g.popTransform();
    }

    public P getInput(int index) {
        AffineTransform at = new AffineTransform();
        at.translate(gateArea.cx(), gateArea.cy());
        at.rotate((2 * Math.PI * rotation)/4);
        at.translate(-1 * gateArea.cx(), -1 * gateArea.cy());
        return new P(r.minx() + ((index + 1) * r.width()) / 4,
                     r.miny() - 3).transform(at);
    }

    public P getInputDest(int index) {
        AffineTransform at = new AffineTransform();
        at.translate(gateArea.cx(), gateArea.cy());
        at.rotate((2 * Math.PI * rotation)/4);
        at.translate(-1 * gateArea.cx(), -1 * gateArea.cy());
        return new P(r.minx() + ((index + 1) * r.width()) / 4,
                     r.miny() + r.height()/2).transform(at);
    }

    public void rotation(int dir) {
        switch (dir) {
            case NORTH: rotation = 2; break;
            case SOUTH: rotation = 0; break;
            case WEST:  rotation = 3; break;
            case EAST:  rotation = 1; break;
            default:    rotation = 0;
        }
    }

    public void route(G g, P p, R ring, int input, int color) {
        if (disabled) return;
        g.color(color);
        g.route(p, ring, getInput(input));
        g.line(getInput(input), getInputDest(input));
    }

}