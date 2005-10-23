package edu.berkeley.cs.obits;

import edu.berkeley.cs.obits.device.atmel.*;
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
        }
        SerialPort ret = new RXTXPort("/dev/cu.usbserial-FTBUODP4");
        Log.info(AtmelSerial.class, "returning " + ret);
        return ret;
    }

    public static void main(String[] s) throws Exception {
        SerialPort sp = detectObitsPort();
        sp.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        //sp.setFlowControlMode(sp.FLOWCONTROL_NONE);
        OutputStream out = sp.getOutputStream();
        InputStream in = sp.getInputStream();
        int count = 0;
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
        /*
        pw.println("Y38,N,8,1");
        pw.flush();
        sp.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        pw.println("GI");
        pw.flush();
        */

        pw.println();
        pw.flush();
        pw.println("^@");
        pw.println("^@");
        pw.println("^@");
        pw.flush();
        try { Thread.sleep(3000); } catch (Exception e) { }

        pw.println("GK\"IMG\"");
        pw.println("GK\"IMG\"");
        pw.println();
        pw.flush();
        try { Thread.sleep(1000); } catch (Exception e) { }
        /*
        pw.println("GI");
        pw.flush();
        */
        int[] data = new int[104 * 104];
        for(int i=0; i<104*104; i++) data[i] = 1;
        for(int i=0; i<104; i++) data[i*104+i] = 0;
        for(int i=0; i<104; i++) data[i*104+(104-i)] = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PCX.dump(104, 104, data, new DataOutputStream(baos));
        byte[] outb = baos.toByteArray();
        int len = outb.length;
        pw.println("GM\"IMG\""+len);
        pw.flush();
        DataOutputStream dout = new DataOutputStream(out);
        for(int i=0; i<len; i++) {
            System.out.println("wrote " + i + "/"+outb.length);
            dout.writeByte(outb[i]);
            dout.flush();
        }
        dout.flush();

        pw.println();
        pw.println("GI");
        pw.flush();

        try { Thread.sleep(2000); } catch (Exception e) { }

        pw.println();
        pw.println("OD");
        pw.println("N");
        pw.println("D14");
        pw.println("S1");
        pw.println("Q609,24");
        pw.println("q754");
        //pw.println("R0,0");
        pw.println("A170,5,0,1,5,5,N,\"WORLDWIDE\"");
        pw.println("LO5,230,765,10");
        pw.println("A10,265,0,1,3,3,R,\"MODEL:\"");
        pw.println("A280,265,0,1,3,3,N,\"Bar Code Printer\"");
        pw.println("A10,340,0,1,3,3,R,\"  CODE: \"");
        pw.println("B280,340,0,3C,2,6,120,B,\"BCP-1234\"");
        pw.println("LO5,520,765,10");
        pw.println("A100,550,0,1,2,2,N,\"ISO9000     Made In USA\"");
        pw.println("GG0,0,\"IMG2\"");
        pw.println("P1");
        pw.flush();

        /*
        */
        //Log.debug(this, "consuming any leftover data on the serial port");

        /*
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
        */
        System.exit(0);
    }

}
