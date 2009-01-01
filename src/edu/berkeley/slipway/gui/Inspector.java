package edu.berkeley.slipway.gui;

import static com.atmel.fpslic.FpslicConstants.*;
import static edu.berkeley.slipway.gui.GuiConstants.*;
import com.atmel.fpslic.*;
import edu.berkeley.slipway.*;
import java.awt.*;

public class Inspector {

    private static P bump(P p, R r) {
        p = p.translate(0, 15);
        if (p.y + 15 > r.maxy()) {
            p = new P(p.x + 200, r.miny() + 20);
        }
        return p;
    }
    public static void draw(G g, R r, FpslicDevice.Cell cell) {
        g.color(0x000000);
        r.fill(g);
        g.color(0xffffff);
        g.setFont(new Font("monospaced", 0, 14));
        if (cell==null) return;

        int line = (int)(r.miny() + 10);
        P p = new P(r.minx() + 10, r.miny() + 20);
        g.drawString("selected: " + cell.col + "," + cell.row,          p);   p = bump(p, r);
        g.drawString("    xlut: " + XLUT_EQUATIONS[cell.xlut() & 0xff], p);   p = bump(p, r);
        g.drawString("    ylut: " + YLUT_EQUATIONS[cell.ylut() & 0xff], p);   p = bump(p, r);
        String xi = "??";
        switch(cell.xi()) {
            case NW : xi = "NW"; break;
            case NE : xi = "NE"; break;
            case SW : xi = "SW"; break;
            case SE : xi = "SE"; break;
            case NONE  : xi = "."; break;
            default:  xi = "L"+(cell.xi()-L0); break;
        }
        g.drawString("x-in mux: " + xi, p);   p = bump(p, r);

        String yi = "??";
        switch(cell.yi()) {
            case NORTH : yi = "NORTH"; break;
            case SOUTH : yi = "SOUTH"; break;
            case EAST  : yi = "EAST"; break;
            case WEST  : yi = "WEST"; break;
            case NONE  : yi = "."; break;
            default:     yi = "L"+(cell.yi()-L0); break;
        }
        g.drawString("y-in mux: " + yi, p);   p = bump(p, r);

        g.drawString("w-in mux: " + (cell.wi()==NONE ? "." : ("L"+(cell.wi()-L0))),
                     p);   p = bump(p, r);
        g.drawString("z-in mux: " + (cell.zi()==NONE ? "." : ("L"+(cell.zi()-L0))),
                     p);   p = bump(p, r);

        String tm = "??";
        switch(cell.t()) {
            case TMUX_FB:       tm = "fb"; break;
            case TMUX_W_AND_FB: tm = "w&fb"; break;
            case TMUX_Z:        tm = "z"; break;
            case TMUX_W_AND_Z:  tm = "w&z"; break;
            case TMUX_W:        tm = "w"; break;
        }
        g.drawString("t-in mux: " + tm, p);   p = bump(p, r);

        g.drawString(" set/rst: " + (cell.ff_reset_value() ? "reset=SET" : "."),
                     p);   p = bump(p, r);

        String outs = "";
        for(int i=0; i<5; i++) outs += (cell.out(L0+i) ? (i+" ") : ". ");
        g.drawString("     out: " + outs,
                     p);   p = bump(p, r);
        String hs = "";
        for(int i=0; i<5; i++) hs += (cell.hx(L0+i) ? (i+" ") : ". ");
        g.drawString("  h conn: " + hs,
                     p);   p = bump(p, r);
        String vs = "";
        for(int i=0; i<5; i++) vs += (cell.vx(L0+i) ? (i+" ") : ". ");
        g.drawString("  v conn: " + vs,
                     p);   p = bump(p, r);
        g.drawString("out enab: " + (cell.oe()==H4 ? "H4" : cell.oe()==V4 ? "V4" : "."),
                     p);   p = bump(p, r);
        g.drawString("   c-mux: " + (cell.c()==ZMUX ? "zmux" : cell.c()==XLUT ? "x-lut" : "y-lut"),
                     p);   p = bump(p, r);
        g.drawString("  fb src: " + (cell.f() ? "clocked" : "."),
                     p);   p = bump(p, r);
        g.drawString("  bypass: " + (cell.b() ? "clocked" : "."),
                     p);   p = bump(p, r);
        g.drawString("   x out: " + (cell.xo() ? (cell.b() ? "register" : "center") : "."),
                     p);   p = bump(p, r);
        g.drawString("   y out: " + (cell.yo() ? (cell.b() ? "register" : "center") : "."),
                     p);   p = bump(p, r);

    }
}