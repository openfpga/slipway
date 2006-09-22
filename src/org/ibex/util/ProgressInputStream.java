package org.ibex.util;
import java.io.*;

public class ProgressInputStream extends FilterInputStream {

    private int size = -1;
    private int bytes = 0;
    private String title;

    public ProgressInputStream(String title, InputStream o) { this(title, o, -1); }

    public ProgressInputStream(String title, InputStream o, int size) {
        super(o);
        this.size = size;
        this.title = title;
    }

    public int read() throws IOException {
        int ret = super.read();
        if (ret != -1) bytes++;
        return ret;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int ret = super.read(b, off, len);
        if (ret != -1) bytes += ret;
        update();
        return ret;
    }

    private void update() {
        System.out.print("\r");
        System.out.print(title);
        if (size != -1) {
            int frac = (100 * bytes) / size;
            String fracs = frac+"";
            while(fracs.length()<3) fracs = " "+fracs;
            System.out.print(" ");
            System.out.print("\033[32m");
            System.out.print(fracs+"%");
            System.out.print("\033[0m");
        }
        System.out.print(" ");
        System.out.print(bytes);
        System.out.print(" bytes ");
        System.out.print("                         ");
    }

    public void close() throws IOException {
        super.close();
        bytes = size;
        update();
        System.out.println();
    }
}
