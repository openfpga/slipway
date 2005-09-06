package edu.berkeley.cs.obits.device.atmel;

import edu.berkeley.cs.obits.*;

public abstract class AtmelDevice extends Device {

    /** issue a command to the device in Mode4 format; see Gosset's documentation for further details */
    public void mode4(int z, int y, int x, int d) throws DeviceException;

    public Sector sector(int col, int row) { return new Sector(col, row); }
    public final class Sector {
        public final int col;
        public final int row;
        public Sector(int col, int row) {
            if (row % 4 != 0) throw new Error("Sector must be created with a multiple-of-4 row");
            if (col % 4 != 0) throw new Error("Sector must be created with a multiple-of-4 col");
            this.row = row;
            this.col = col;
        }
    }
    
    public Cell cell(int col, int row) { return new Cell(col, row); }
    public final class Cell {
        public final int col;
        public final int row;
        public Sector getSector() { return sector(col - (col % 4), row - (row % 4)); }
        public Cell(int col, int row) {
            this.row = row;
            this.col = col;
        }
    }

}
