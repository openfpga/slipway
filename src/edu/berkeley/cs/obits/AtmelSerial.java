package edu.berkeley.cs.obits;

import edu.berkeley.cs.obits.device.atmel.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class AtmelSerial {

    public static SerialPort detectObitsPort() throws Exception {
        Enumeration e = CommPortIdentifier.getPortIdentifiers();
        while(e.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier)e.nextElement();
            Log.info(AtmelSerial.class, "trying " + cpi.getName());
        }
        return new RXTXPort("/dev/cu.usbserial-FTBUODP4");
    }

    public static void main(String[] s) throws Exception {
        AvrDrone device = new AvrDrone(detectObitsPort());
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
            System.exit(0);
        } catch (Exception e) { e.printStackTrace(); }
    }

}
