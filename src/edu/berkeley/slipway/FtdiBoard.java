package edu.berkeley.slipway;

import com.ftdi.usb.*;
import com.atmel.fpslic.*;
import edu.berkeley.obits.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class FtdiBoard extends Board {

    static {
        System.load(new File("build/"+System.mapLibraryName("FtdiUartNative")).getAbsolutePath());
    }

    private final FpslicRaw chip;
    private final InputStream in;
    private final OutputStream out;

    public InputStream getInputStream() { return in; }
    public OutputStream getOutputStream() { return out; }

    public FtdiBoard() throws Exception {
        chip = new FpslicRaw(new FpslicPinsUsb(new FtdiUart(0x6666, 0x3133, 1500 * 1000)));
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
        chip.reset();
    }

    public void boot(Reader r) throws Exception {

        chip.selfTest();

        int total = 75090/9;
        OutputStream os = new ProgressOutputStream("bootstrap bitstream:", chip.getConfigStream(), total);
        BufferedReader br = new BufferedReader(r);

        int bytes = 0;
        while(true) {
            String s = br.readLine();
            if (s==null) break;
            bytes++;
            os.write((byte)Integer.parseInt(s, 2));
            if ((bytes % 1000)==0) os.flush();
        }
        os.close();
    }

    public static String pad(String s, int i) {
        if (s.length() >= i) return s;
        return "0"+pad(s, i-1);
    }
}
