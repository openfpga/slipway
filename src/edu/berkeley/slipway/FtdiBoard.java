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
            }
        }
        os.close();
    }

    public static String pad(String s, int i) {
        if (s.length() >= i) return s;
        return "0"+pad(s, i-1);
    }
}
