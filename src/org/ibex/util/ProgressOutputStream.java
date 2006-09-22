package org.ibex.util;
import java.io.*;

public class ProgressOutputStream extends FilterOutputStream {

    private int size = -1;
    private int bytes = 0;
    private String title;

    public ProgressOutputStream(String title, OutputStream o) { this(title, o, -1); }

    public ProgressOutputStream(String title, OutputStream o, int size) {
        super(o);
        this.size = size;
        this.title = title;
    }

    public void write(int i) throws IOException {
        super.write(i);
        bytes++;
        update();
    }

    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        bytes += len;
        update();
    }

    private void update() {
        System.out.print("\r                                                              \r");
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
    }

    public void close() throws IOException {
        super.close();
        bytes = size;
        update();
        System.out.println();
    }
}
