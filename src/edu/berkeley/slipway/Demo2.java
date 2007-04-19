package edu.berkeley.slipway;

import edu.berkeley.slipway.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static com.atmel.fpslic.FpslicUtil.*;
import edu.berkeley.slipway.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class Demo2 {

    public static void main(String[] s) throws Exception {
        FtdiBoard device = new FtdiBoard();
        Fpslic at40k = device;
        long begin = System.currentTimeMillis();
        //FpslicUtil.readMode4(new ProgressInputStream("configuring fabric", System.in, 111740), device);
        long end = System.currentTimeMillis();
        Log.info(Demo.class, "finished in " + ((end-begin)/1000) + "s");
        Thread.sleep(1000);
        Log.info(Demo.class, "issuing command");

        Fpslic.Cell root = at40k.cell(5,5);

        root.ylut(LUT_SELF);
        root.yi(NORTH);
        root.xi(NW);
        root.wi(L0);
        root.zi(L2);

        root = root.north();
        root.ylut(LUT_SELF);
        root.yi(WEST);
        root.wi(L1);
        root.zi(L3);

        root = root.west();
        root.xi(SE);
        root.ylut(LUT_SELF);
        root.yi(SOUTH);
        root.wi(L2);
        root.zi(L4);

        root = root.south();
        root.ylut(LUT_SELF);
        root.yi(EAST);
        root.wi(L3);
        root.zi(L0);
        //root = root.n();

        device.flush();

        Gui3 vis = new Gui3(at40k, device);
        Frame fr = new Frame();
        fr.setLayout(new BorderLayout());
        fr.add(vis, BorderLayout.CENTER);
        fr.pack();
        fr.setSize(900, 900);
        vis.repaint();
        fr.repaint();
        fr.show();
        synchronized(Demo.class) { Demo.class.wait(); }
    }

}
