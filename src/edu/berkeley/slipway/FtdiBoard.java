package edu.berkeley.slipway;

import com.atmel.fpslic.*;
import edu.berkeley.obits.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class FtdiBoard extends Board {

    static {
        System.load(new File("build/"+System.mapLibraryName("FtdiUartNative")).getAbsolutePath());
    }

    private final Chip chip;
    private final InputStream in;
    private final OutputStream out;

    public InputStream getInputStream() { return in; }
    public OutputStream getOutputStream() { return out; }

    public FtdiBoard() throws Exception {
        chip = new ChipImpl();
        String bstFile = this.getClass().getName();
        bstFile = bstFile.substring(0, bstFile.lastIndexOf('.'));
        bstFile = bstFile.replace('.', '/')+"/slipway_drone.bst";
        boot(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(bstFile)));
        in = chip.getInputStream();
        out = chip.getOutputStream();
        for(int i=0; i<255; i++) out.write(0);
        out.flush();
    }

    public void reset() throws IOException {
        chip.doReset();
    }

    public void boot(Reader r) throws Exception {
        Chip d = chip;

        d.selfTest();

        OutputStream os = d.getConfigStream();

        BufferedReader br = new BufferedReader(r);
        br.readLine();

        int bytes = 0;
        while(true) {
            String s = br.readLine();
            if (s==null) break;
            int in = Integer.parseInt(s, 2);
            bytes++;
            os.write((byte)in);
            if ((bytes % 1000)==0) {
                os.flush();
                System.out.print("wrote " + bytes + " bytes\r");
                d.rcon();
            }
        }

        d.flush();
        if (!d.initErr())
            throw new RuntimeException("initialization failed at " + bytes);

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

        //((Chip)d).avr();

        //System.out.println("avr reset => true");
        ((ChipImpl)chip).purge();
        ((ChipImpl)chip).uart_and_cbus_mode(1<<1, 1<<1);
        
        //d.avrrst(true);
        //try { Thread.sleep(500); } catch (Exception e) { }
        //System.out.println("cts="+""+"  pins=" + pad(Integer.toString(d.readPins()&0xff,2),8));
    }

    public static String pad(String s, int i) {
        if (s.length() >= i) return s;
        return "0"+pad(s, i-1);
    }
}
