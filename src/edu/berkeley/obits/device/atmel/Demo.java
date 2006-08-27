package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public class Demo {

    static {
        System.load(new File("build/"+System.mapLibraryName("Ftdi")).getAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        main2();
    }

    public static AvrDrone main2() throws Exception {
        Chip d = new ChipImpl();
        boolean pin;
        /*
        doConfig(d, new InputStreamReader(new FileInputStream("e6-off.bst")));
        pin = (d.readPins() & 0x2) != 0;
        System.out.println("e6-off => " + pin + " " + (pin ? red("BAD") : green("good")));

        doConfig(d, new InputStreamReader(new FileInputStream("e6-on.bst")));
        pin = (d.readPins() & 0x2) != 0;
        System.out.println("e6-on  => " + pin + " " + (pin ? green("good") : red("BAD")));
        */

        d.porte(4, true);

        doConfig(d, new InputStreamReader(new FileInputStream("bitstreams/usbdrone.bst")));
        System.out.println("       pins: " + pad(Integer.toString(d.readPins()&0xff,2),8));

        //try { Thread.sleep(1000); } catch (Exception e) { }        
        //((ChipImpl)d).dbangmode();

        ChipImpl ci = (ChipImpl)d;
        final InputStream is = new BufferedInputStream(ci.getInputStream());
        final OutputStream os = new BufferedOutputStream(ci.getOutputStream());
        int oldre=-1;

        /*
        new Thread() {
            public void run() {
                try {
                    while(true) {
                        for(int i=0; i<256; i++) {
                            os.write(i);
                        }
                        os.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        */
        for(int i=0; i<255; i++) {
            os.write(0);
        }
        os.flush();

        return new AvrDrone(is, os);
        /*
        while(true) {
            //d.porte(4, true);
            //try { Thread.sleep(1000); } catch (Exception e) { }
            //System.out.println("char: " + d.readChar());

            //System.out.println("e4=on  pins: " + pad(Integer.toString(d.readPins()&0xff,2),8));


            int inc = 256;
            for(int k=0; k<256; k += inc) {

            //for(int i=k; i<k+inc; i++) {
            //os.write(i);
            //}
            //os.flush();

                for(int i=0; i<inc; i++) {
                    int re = -1;
                    while(re == -1) {
                        re = is.read();
                        if (re != -1) {
                            System.out.print(((oldre == -1) || (re==((oldre+1)%256))) ? "... " : "BAD ");
                            System.out.println(" read " + re);
                            oldre = re;
                        }
                    }
                }
            }

            
            //System.out.println("e4=on  pins: " + ((ChipImpl)d).readChar());


            //d.porte(4, false);
            //try { Thread.sleep(1000); } catch (Exception e) { }
            //d.readPins();
            //System.out.println("e4=off pins: " + pad(Integer.toString(d.readPins()&0xff,2),8));
            */
    }

    public static void doConfig(Chip d, Reader r) throws Exception {
        boolean pin;

        d.doReset();
        d.config(0,10);
        d.con();
        d.config(Integer.parseInt("10110111", 2), 8);
        d.config(0,1);
        pin = d.initErr();
        System.out.println("good preamble   => " + pin + " " + (pin ? green("good") : red("BAD")));

        d.doReset();
        d.config(0,9);
        d.con();
        d.config(Integer.parseInt("10110111", 2), 8);
        d.config(0, 2);
        pin = d.initErr();
        System.out.println("bad preamble #2 => " + pin + " " + (pin ? red("BAD") : green("good")));

        d.doReset();
        d.config(0,10);
        d.con();
        d.config(Integer.parseInt("11110111", 2), 8);
        d.config(0, 1);
        pin = d.initErr();
        System.out.println("bad preamble #1 => " + pin + " " + (pin ? red("BAD") : green("good")));


        d.doReset();

        d.config(0,10);
        d.con();
        //d.config(Integer.parseInt("10110111", 2));
        //d.config(0);

        BufferedReader br = new BufferedReader(r);
        br.readLine();
        int bytes = 0;
        d.buffered();
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
            System.out.print("cts="+""+"  pins=" + pad(Integer.toString(d.readPins()&0xff,2),8)+"      \r");
            d.config(0,1);
        }

        System.out.println();
        System.out.println("avr reset => false");
        d.avrrst(false);
        try { Thread.sleep(500); } catch (Exception e) { }
        //System.out.println("cts="+""+"  pins=" + pad(Integer.toString(d.readPins()&0xff,2),8));

        //((ChipImpl)d).avr();

        //System.out.println("avr reset => true");
        ((ChipImpl)d).uart();
        ((ChipImpl)d).purge();

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

