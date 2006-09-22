package edu.berkeley.slipway;

import edu.berkeley.obits.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class FakeBoard extends Board {

    public FakeBoard() {
        
    }

    public void reset() {
        System.err.println("FakeBoard: reset()");
    }
    public void boot(Reader r) throws Exception {

    }
    public InputStream getInputStream() { throw new Error(); }
    public OutputStream getOutputStream() { throw new Error(); }

}
