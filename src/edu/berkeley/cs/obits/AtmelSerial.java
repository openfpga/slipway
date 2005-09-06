package edu.berkeley.cs.obits;

import java.io.*;
import java.util.*;
import gnu.io.*;

public class AtmelSerial {

    public static SerialPort detectObitsPort() throws Exception {
        Enumeration e = CommPortIdentifier.getPortIdentifiers();
        while(e.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier)e.nextElement();
            System.err.println("trying " + cpi.getName());
        }
        return new RXTXPort("/dev/cu.usbserial-FTBUODP4");
    }
    public static void main(String[] s) throws Exception {
        final SerialPort sp = detectObitsPort();
        sp.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.setFlowControlMode(sp.FLOWCONTROL_RTSCTS_OUT);
        final OutputStream out = sp.getOutputStream();
        final InputStream in = sp.getInputStream();
        while(in.available() > 0) in.read();
        sp.setDTR(true);
        Thread.sleep(500);
        sp.setDTR(false);
        Thread.sleep(3000);
        DataInputStream dis = new DataInputStream(in);
        System.err.println("waiting...");
        if (dis.readByte() != (byte)'O')  throw new RuntimeException("didn't get the proper signature");
        if (dis.readByte() != (byte)'B')  throw new RuntimeException("didn't get the proper signature");
        if (dis.readByte() != (byte)'I')  throw new RuntimeException("didn't get the proper signature");
        if (dis.readByte() != (byte)'T')  throw new RuntimeException("didn't get the proper signature");
        if (dis.readByte() != (byte)'S')  throw new RuntimeException("didn't get the proper signature");
        if (dis.readByte() != (byte)'\n') throw new RuntimeException("didn't get the proper signature");
        System.err.println("ready.");
        int count = 0;
        new Thread() {
            public void run() {
                try {
                    while(true) {
                        System.err.println(sp.isDTR() + " " + sp.isDSR() + " " + sp.isRTS() + " " + sp.isCTS());
                        Thread.sleep(250);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } }.start();
        new Thread() {
            public void run() {
                try {
                    while(true) {
                        int i2 = in.read();
                        if (i2==-1) { System.err.println("input closed"); System.exit(-1); }
                        System.out.print((char)i2);
                        System.out.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        try {
            Thread.sleep(1000);
            long begin = System.currentTimeMillis();
            DataOutputStream dos = new DataOutputStream(out);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            for(String str = br.readLine(); str != null; str = br.readLine()) {
                long foo = Long.parseLong(str, 16);
                dos.writeByte(1);
                dos.writeByte((int)(foo >> 24));
                dos.writeByte((int)(foo >> 16));
                dos.writeByte((int)(foo >>  8));
                dos.writeByte((int)(foo >>  0));
                count++;
                if (count % 100 == 0) System.err.println("wrote " + count + " configuration octets");
            }
            dos.flush();
            long end = System.currentTimeMillis();
            System.err.println("finished in " + ((end-begin)/1000) + "s");
            System.exit(0);
        } catch (Exception e) { e.printStackTrace(); }
    }

}
