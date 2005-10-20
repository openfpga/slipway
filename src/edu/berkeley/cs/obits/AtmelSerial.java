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
        return new RXTXPort("/dev/cu.usbserial-FTBUODP4");
    }

    public static void main(String[] s) throws Exception {
        SerialPort sp = detectObitsPort();
        sp.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.setFlowControlMode(sp.FLOWCONTROL_NONE);
        OutputStream out = sp.getOutputStream();
        InputStream in = sp.getInputStream();
        int count = 0;
        byte[] b = InputStreamToByteArray.convert(System.in);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
        /*
        pw.println("Y38,N,8,1");
        pw.flush();
        sp.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        pw.println("GI");
        pw.flush();
        */
        pw.println("OD");
        pw.println("N");
        pw.println("D14");
        pw.println("S1");
        pw.println("Q609,24");
        pw.println("q754");
        //pw.println("R0,0");

        int len = 228;
        pw.println("GK\"IMG\"");
        pw.println("GM\"IMG\""+len);
        System.out.println("flushing");
        pw.flush();
        System.out.println("flushed");
        DataOutputStream dos = new DataOutputStream(out);
        
        dos.flush();

        /*
        pw.println("A170,5,0,1,5,5,N,\"WORLDWIDE\"");
        pw.println("LO5,230,765,10");
        pw.println("A10,265,0,1,3,3,R,\"MODEL:\"");
        pw.println("A280,265,0,1,3,3,N,\"Bar Code Printer\"");
        pw.println("A10,340,0,1,3,3,R,\"  CODE: \"");
        pw.println("B280,340,0,3C,2,6,120,B,\"BCP-1234\"");
        pw.println("LO5,520,765,10");
        pw.println("A100,550,0,1,2,2,N,\"ISO9000     Made In USA\"");
        pw.println("GG650,535,\"CE_5M\"");
        */
        pw.println("GG0,0,\"IMG\"");
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
