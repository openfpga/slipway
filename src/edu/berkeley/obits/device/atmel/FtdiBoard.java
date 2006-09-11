package edu.berkeley.obits.device.atmel;

import edu.berkeley.obits.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class FtdiBoard extends Board {

    static {
        System.load(new File("build/"+System.mapLibraryName("Ftdi")).getAbsolutePath());
    }

    private final ChipImpl chip;
    private final InputStream in;
    private final OutputStream out;

    public InputStream getInputStream() { return in; }
    public OutputStream getOutputStream() { return out; }

    public FtdiBoard() throws Exception {
        chip = new ChipImpl();
        boot(new InputStreamReader(new FileInputStream("bitstreams/usbdrone.bst")));
        in = chip.getInputStream();
        out = chip.getOutputStream();
        for(int i=0; i<255; i++) out.write(0);
        out.flush();
    }

    public void reset() {
        chip.doReset();
    }

    public void boot(Reader r) throws Exception {
        boolean pin;
        Chip d = chip;
        /*
        d.buffered(false);
        d.doReset();
        d.config(0,10);
        d.con();
        d.flush();
        d.config(Integer.parseInt("10110111", 2), 8);
        d.config(0,1);
        d.flush();
        pin = d.initErr();
        System.out.println("good preamble   => " + pin + " " + (pin ? green("good") : red("BAD")));

        d.doReset();
        d.config(0,9);
        d.con();
        d.flush();
        d.config(Integer.parseInt("10110111", 2), 8);
        d.config(0, 2);
        d.flush();
        pin = d.initErr();
        System.out.println("bad preamble #2 => " + pin + " " + (pin ? red("BAD") : green("good")));

        d.doReset();
        d.config(0,10);
        d.con();
        d.flush();
        d.config(Integer.parseInt("11110111", 2), 8);
        d.config(0, 1);
        d.flush();
        pin = d.initErr();
        System.out.println("bad preamble #1 => " + pin + " " + (pin ? red("BAD") : green("good")));
        */
        d.doReset();

        d.config(0,10);
        d.con();
        //d.config(Integer.parseInt("10110111", 2));
        //d.config(0);

        BufferedReader br = new BufferedReader(r);
        br.readLine();
        int bytes = 0;
        //System.out.println("cts="+""+"  pins=" + pad(Integer.toString(d.readPins()&0xff,2),8));
        while(true) {
            String s = br.readLine();
            if (s==null) break;
            int in = Integer.parseInt(s, 2);
            bytes++;
            for(int i=7; i>=0; i--) {
                d.config((((in & 0xff) & (1<<i))!=0)?1:0, 1);
                boolean init = true; // d.initErr()
                if (bytes < 100 || (bytes % 1000)==0) {
                    d.flush();
                    init = d.initErr();
                    System.out.print("wrote " + bytes + " bytes, init="+init+"      \r");
                }
                if (!init)
                    throw new RuntimeException("initialization failed at byte " + bytes + ", bit " + i);
            }
        }


        d.flush();
        if (!d.initErr())
            throw new RuntimeException("initialization failed at " + bytes);
        //System.out.println("cts="+""+"  pins=" + pad(Integer.toString(d.readPins()&0xff,2),8));


        for(int i=0; i<100; i++) {
            d.flush();
            if (!d.initErr())
                throw new RuntimeException("initialization failed at " + bytes);
            try { Thread.sleep(20); } catch (Exception e) { }
            d.config(0,1);
        }

        System.out.println();
        System.out.println("avr reset => false");
        d.avrrst(false);
        try { Thread.sleep(500); } catch (Exception e) { }
        //System.out.println("cts="+""+"  pins=" + pad(Integer.toString(d.readPins()&0xff,2),8));

        //((ChipImpl)d).avr();

        //System.out.println("avr reset => true");
        chip.uart();
        chip.purge();
        
        //d.avrrst(true);
        //try { Thread.sleep(500); } catch (Exception e) { }
        //System.out.println("cts="+""+"  pins=" + pad(Integer.toString(d.readPins()&0xff,2),8));
    }

    public static String pad(String s, int i) {
        if (s.length() >= i) return s;
        return "0"+pad(s, i-1);
    }
    public static String red(Object o) { return "\033[31m"+o+"\033[0m"; }
    public static String green(Object o) { return "\033[32m"+o+"\033[0m"; }
}
