package com.atmel.fpslic;

import java.io.*;
import java.util.*;

public class FpslicUtil {

    public static int lutSwap(int x) {
        return
            (x & 0x80)        |
            ((x & 0x20) << 1) |
            ((x & 0x40) >> 1) |
            (x & 0x10) |
            (x & 0x08)        |
            ((x & 0x02) << 1) |
            ((x & 0x04) >> 1) |
            (x & 0x01);
    }

    public static void readMode4(InputStream in, Fpslic fpslic) throws IOException {
        int count = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        for(String str = br.readLine(); str != null; str = br.readLine()) {
            long foo = Long.parseLong(str, 16);
            fpslic.mode4((int)(foo >> 24), (int)(foo >> 16), (int)(foo >>  8), (int)(foo >>  0));
            count++;
        }
        fpslic.flush();
        in.close();
    }

    public static void writeMode4(Writer w, Fpslic fpslic) throws IOException {
        for(int x=0; x<fpslic.getWidth(); x++)
            for(int y=0; y<fpslic.getWidth(); y++)
                for(int z=0; z<255; z++) {
                    if ((z > 0x09 && z < 0x10) ||
                        (z > 0x11 && z < 0x20) ||
                        (z > 0x29 && z < 0x30) ||
                        (z > 0x39 && z < 0x40) ||
                        (z > 0x41 && z < 0x60) ||
                        (z > 0x67 && z < 0x70) ||
                        (z > 0x77 && z < 0xD0) ||
                        (z > 0xD3))
                        continue;
                    w.write(hex2(z));
                    w.write(hex2(y));
                    w.write(hex2(x));
                    w.write(hex2(fpslic.mode4(z, y, x) & 0xff));
                    w.write('\n');
                }
        w.flush();
    }

    private static String hex2(int i) {
        String ret = Integer.toString(i, 16);
        while(ret.length() < 2) ret = "0"+ret;
        return ret.toUpperCase();
    }

    public static synchronized String printLut(int lut, String xn, String yn, String zn) {
        try {
            File f = File.createTempFile("mvsis", ".mvs");
            f.deleteOnExit();

            FileOutputStream fos = new FileOutputStream(f);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
            pw.println(".model clb");
            pw.println(".inputs "+xn+" "+yn+" "+zn);
            pw.println(".outputs O");
            pw.println(".table "+xn+" "+yn+" "+zn+/*("X_xor_Y X_xor_Z Y_xor_Z")+*/ " -> O");
            for(int i=8; i>=0; i--) {
                int x = ((i & 0x01)!=0 ? 1 : 0);
                int y = ((i & 0x02)!=0 ? 1 : 0);
                int z = ((i & 0x04)!=0 ? 1 : 0);
                pw.print(" "+x+" ");
                pw.print(" "+y+" ");
                pw.print(" "+z+" ");
                //pw.print(" "+(x ^ y)+" ");
                //pw.print(" "+(y ^ z)+" ");
                //pw.print(" "+(z ^ y)+" ");
                pw.print((lut & (1<<i))==0 ? 0 : 1);
                pw.println();
            }
            pw.println(".end");
            pw.flush();
            pw.close();
            Process p = Runtime.getRuntime().exec(new String[] { "mvsis", "-c", "simplify;print_factor", f.getAbsolutePath() });
            new Gobble("mvsis: ", p.getErrorStream()).start();
            //new Gobble("mvsis: ", p.getInputStream()).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String ret = br.readLine();
            //f.delete();
            return ret.trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "*mvsis_error*";
        }
    }

    public static class Gobble extends Thread {
        private final String header;
        private final BufferedReader br;
        public Gobble(String header, BufferedReader br) { this.br = br; this.header = header; }
        public Gobble(String header, Reader r)          { this(header, new BufferedReader(r)); }
        public Gobble(String header, InputStream is)    { this(header, new InputStreamReader(is)); }
        public void run() {
            try {
                for(String s = br.readLine(); s!=null; s=br.readLine())
                    System.err.println(header + s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
