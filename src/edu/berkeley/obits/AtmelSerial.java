package edu.berkeley.obits;

import edu.berkeley.obits.device.atmel.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class AtmelSerial {

    public static SerialPort detectObitsPort() throws Exception {
        Enumeration e = CommPortIdentifier.getPortIdentifiers();
        while(e.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier)e.nextElement();
            Log.info(AtmelSerial.class, "trying " + cpi.getName());
            if (cpi.getName().startsWith("/dev/cu.usbserial-")) return new RXTXPort(cpi.getName());
            if (cpi.getName().startsWith("/dev/ttyS0")) return new RXTXPort(cpi.getName());
        }
        Log.info(AtmelSerial.class, "returning null...");
        return null;
    }

    public static void main(String[] s) throws Exception {
        AvrDrone device = new AvrDrone(detectObitsPort());
        At40k at40k = new At40k.At40k10(device);
        int count = 0;
        try {
            long begin = System.currentTimeMillis();
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            for(String str = br.readLine(); str != null; str = br.readLine()) {
                long foo = Long.parseLong(str, 16);
                device.mode4((int)(foo >> 24), (int)(foo >> 16), (int)(foo >>  8), (int)(foo >>  0));
                count++;
                if (count % 100 == 0) Log.info(AtmelSerial.class, "wrote " + count + " configuration octets");
            }
            device.flush();
            long end = System.currentTimeMillis();
            Log.info(AtmelSerial.class, "finished in " + ((end-begin)/1000) + "s");
            Thread.sleep(3000);
            Log.info(AtmelSerial.class, "issuing command");

            //at40k.iob_top(2, true).oe(false);
            //at40k.iob_top(2, false).oe(false);
            //at40k.iob_top(1, true).oe(false);

            // this command confirmed to turn *on* led0
            //at40k.iob_top(1, false).output(0);
            /*
            for(int i=0; i<20; i++) {
                at40k.iob_bot(i, false).output(0);
                at40k.iob_bot(i, true).output(0);
            }
            */

            //System.out.println("tick");
                //Thread.sleep(3000);
                //System.out.println("tick");
                //at40k.cell(0x01, 0x17).xlut((byte)0x);

            at40k.cell(0x04, 0x17).xlut((byte)~0x10);
            at40k.cell(0x04, 0x17).ylut((byte)0x10);
            at40k.cell(0x04, 0x17).xo(true);


            /*
            at40k.cell(0x01, 0x17).xin(4);
            at40k.cell(0x01, 0x17).yin(4);
            at40k.cell(0x01, 0x16).ylut((byte)0x00);
            device.mode4(2, 0x17, 0x01, 0);

            for(int i=0; i<10; i++) {
                Thread.sleep(3000);
                System.out.println("tick");
                //at40k.cell(0x01, 0x17).xlut((byte)0xFF);
                at40k.cell(0x00, 0x17).ylut((byte)0x00);
                device.flush();
                Thread.sleep(3000);
                System.out.println("tick");
                //at40k.cell(0x01, 0x17).xlut((byte)0x00);
                at40k.cell(0x00, 0x17).ylut((byte)0xFF);
                device.flush();
            }
            */


            /*
            at40k.iob_top(0, true).output(0);
            at40k.iob_top(0, true).oe(false);
            at40k.iob_top(0, true).pullup();
            device.flush();
            Thread.sleep(3000);

            Log.info(AtmelSerial.class, "issuing command");
            at40k.iob_top(1, true).pulldown();
            device.flush();
            */
            Log.info(AtmelSerial.class, "done");
            System.exit(0);
        } catch (Exception e) { e.printStackTrace(); }
    }

}
