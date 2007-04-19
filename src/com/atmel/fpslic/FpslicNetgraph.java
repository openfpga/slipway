package com.atmel.fpslic;

/** a higher-level, lower-performance, write-only API */
public class FpslicNetgraph {
    /*
    public interface Net {
    }

    public interface Mux extends Net {
    }

    public interface PassGate {
    }

    public class Cell {
        public Cell north() { return cell(col,row+1); }
        public Cell south() { return cell(col,row-1); }
        public Cell west()  { return cell(col-1,row); }
        public Cell east()  { return cell(col+1,row); }
        public Cell ne()    { return cell(col+1,row+1); }
        public Cell se()    { return cell(col+1,row-1); }
        public Cell nw()    { return cell(col-1,row+1); }
        public Cell sw()    { return cell(col-1,row-1); }

        public Net xi       = new XI(new Net[] { wire(0), wire(1), wire(2), wire(3), wire(4),
                                                 nw().xo, ne().xo, sw().xo, se().xo
                                               });
        public Net yi       = new YI(new Net[] { wire(0), wire(1), wire(2), wire(3), wire(4),
                                                 north().yo, south().yo, east().yo, west().yo
                                               });
        public Net wi       = new WI(new Net[] { wire(0), wire(1), wire(2), wire(3), wire(4) });
        public Net zi       = new ZI(new Net[] { wire(0), wire(1), wire(2), wire(3), wire(4) });

        public Net cmux     = new CMux();
        public Net ylut     = new YLut();
        public Net xlut     = new XLut();
        public Net yo       = new Yo();
        public Net xo       = new Xo();
        public Net register = new Register();
    }
    */
}
