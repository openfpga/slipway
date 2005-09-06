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

    public static class AvrDrone {
        final DataInputStream in;
        final DataOutputStream out;
        final SerialPort sp;
        public AvrDrone(SerialPort sp) throws IOException, UnsupportedCommOperationException, InterruptedException {
            this.sp = sp;
            sp.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            sp.setFlowControlMode(sp.FLOWCONTROL_RTSCTS_OUT);
            this.out = new DataOutputStream(sp.getOutputStream());
            this.in = new DataInputStream(sp.getInputStream());
            while(in.available() > 0) in.read();
            reset();
            System.err.println("waiting...");
            if (in.readByte() != (byte)'O')  throw new RuntimeException("didn't get the proper signature");
            if (in.readByte() != (byte)'B')  throw new RuntimeException("didn't get the proper signature");
            if (in.readByte() != (byte)'I')  throw new RuntimeException("didn't get the proper signature");
            if (in.readByte() != (byte)'T')  throw new RuntimeException("didn't get the proper signature");
            if (in.readByte() != (byte)'S')  throw new RuntimeException("didn't get the proper signature");
            if (in.readByte() != (byte)'\n') throw new RuntimeException("didn't get the proper signature");
            System.err.println("ready.");
        }
        public void reset() throws InterruptedException {
            sp.setDTR(true);
            Thread.sleep(500);
            sp.setDTR(false);
            Thread.sleep(3000);
        }
        public void mode4(int z, int y, int x, int d) throws IOException {
            out.writeByte(1);
            out.writeByte(z);
            out.writeByte(y);
            out.writeByte(x);
            out.writeByte(d);
        }
        public void flush() throws IOException {
            out.flush();
        }
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
                if (count % 100 == 0) System.err.println("wrote " + count + " configuration octets");
            }
            device.flush();
            long end = System.currentTimeMillis();
            System.err.println("finished in " + ((end-begin)/1000) + "s");
            System.exit(0);
        } catch (Exception e) { e.printStackTrace(); }
    }

}
