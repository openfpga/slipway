package edu.berkeley.slipway;

import edu.berkeley.obits.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

public interface Board {

    /** boot the board using an md4 configuration stream */
    public abstract void boot(Reader r) throws Exception;

    /** reset the board */
    public abstract void reset() throws IOException;

    /** the UART inputstream (after loading initial config) */
    public abstract InputStream getInputStream();

    /** the UART inputstream (after loading initial config) */
    public abstract OutputStream getOutputStream();

}
