package edu.berkeley.obits;

public abstract class Bits {
    /*
    public abstract boolean get(long bit);
    public abstract void    set(long bit, boolean val);

    public int get(long bit, int len) {
        int ret = 0;
        for(long i=bit; i<bit+len; i++) ret = (ret << 1) | get(i);
        return ret;
    }
    public void set(long bit, int len, int val) {
        for(long i=bit+len-1; i>=bit; i--) {
            set(i, (val & 1) != 0);
            val >>= 1;
        }
    }

    public final boolean get(int offset, int bit) { return get(offset*8 + bit); }
    public final int     get(int offset, int bit, int len) { return get(offset*8 + bit, num); }
    public final void    set(int offset, int bit, boolean b) { set(offset*8 + bit, b); }
    public final void    set(int offset, int bit, int len, int val) { set(offset*8 + bit, num, val); }

    public static class Offset extends Bits {
        private final Bits bits;
        private final long off;

        public Offset(Bits bits, long offset) { this.off = offset; this.bits = bits; }
        public Offset(Bits bits, int offset, int bit) { this(bits, offset*8+bit); }

        public boolean get(long bit) { return bits.get(bit+off); }
        public int     get(long bit, int len) { return bits.get(bit+off, len); }
        public void    set(long bit, boolean val) { bits.set(bit+off, val); }
        public void    set(long bit, int len, int val) { bits.set(bit+off, len, val); }
    }

    public static class Arr extends Bits {
        private byte[] bits;
        public Bits(int capacity) { this.bits = new byte[(capacity / 8) + (capacity%8 == 0 ? 0 : 1)]; }

        public boolean get(long bit) {
            if (bit / 8 >= bits.length) return false;
            int ret = bits[bit/8];
            ret >> 8-(bit-((bit/8)*8));
            return (ret & 1) != 0;
        }

        public void set(long bit, boolean b) {
            if (bit / 8 >= bits.length) {
                if (!b) return;
                byte[] bits2 = new byte[Math.max((bit/8)+1, (bits.length * 2))];
                System.arraycopy(bits, 0, bits2, 0, bits.length);
                bits = bits2;
                set(bit, b);
                return;
            }
            byte mask = (byte)(1 << (8-(bit-((bit/8)*8))));
            if (b) {
                bits[bit/8] |=  mask;
            } else {
                bits[bit/8] &= ~mask;
            }
        }
        
    }
    */
}
