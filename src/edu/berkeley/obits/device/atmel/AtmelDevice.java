package edu.berkeley.obits.device.atmel;

import edu.berkeley.obits.*;
//import static edu.berkeley.cs.obits.device.atmel.Wires.*;
import java.util.*;

public abstract class AtmelDevice extends Bits implements Device {

    /** issue a command to the device in Mode4 format; see Gosset's documentation for further details */
    public abstract void mode4(int z, int y, int x, int d) throws DeviceException;
    /*
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

    public static interface Driver { }

    public static interface HasPlane { public int getPlane(); }

    public static interface XI extends Driver { }
    public static interface YI extends Driver { }
    */
    /*
    public static void foo(Direction f) { }
    public static enum Direction implements Drives<Y> { N, S, E, W; }
    public static enum Bar { A, B, C; }
    public static Object B = new Object();
    public static Direction foo = null;
    static {
        switch(foo) {
            case N:
            case S:
            case W:
        }
    }

    public class Drives<E extends EPR<E>> { }
    public class EPR<E extends EPR<E>> {
        public abstract Drives<E> driver();
        public abstract void      driver(Drives<E>);
    }

    public class Joins<B extends Bus<B>> { }
    public abstract class Bus<B extends Bus<B>> {
        public Set<Joins<E>> drivers();
    }
    public interface Drives<E extends EPR> { }


    public static interface EPR {
    }
    public static interface Drives<E extends EPR> { }
    public static interface Bus extends EPR { }
    public static interface Joins<J extends Joins, B extends Bus & Joins<B,J>> extends Bus { }
    //public static interface Joins<B extends Bus> extends Bus { }
    public static interface Has<E extends EPR> {
        public Drives<E> getDriver(E e);
        public void      setDriver(E e, Drives<E> driver);
    }
    //public static interface HasBus<B extends Bus> { public Set<OnBus<B>> on(B b); }
    public interface Input { }
    public interface Output { }
    public interface InOut { }
    
    public static abstract class LUT<A extends EPR, B extends EPR, C extends EPR> implements EPR { }
    public static abstract class And<A extends EPR, B extends EPR> implements EPR { }
    public static abstract class Reg<A extends EPR> { }
    public static abstract class Mux<Sel extends EPR, Zero extends EPR, One extends EPR> { }
    public static abstract class Buf<Sel extends EPR> { }

    public enum DiagonalInputs   implements Input, Drives<X> { NW, SW, NE, SE; }
    public enum OrthogonalInputs implements Input, Drives<Y> { N,  S,  E,  W;  }

    public <X extends Drives<Y>, Y extends EPR> void connect(X x, Y y) { }

    public static enum Plane { P0, P1, P2, P3, P4; }
    public static class L<P extends Plane> implements Bus, Drives<X>, Drives<Y>, Drives<Z>, Drives<W> { }

    public final class Cell {
      public class X                         implements EPR, Drives<XLUT>, Drives<YLUT> { }
      public class Y                         implements EPR, Drives<XLUT>, Drives<YLUT> { }
      public class Z                         implements EPR, Drives<A>, Drives<WZ> { }
      public class F                         implements EPR, Drives<A>, Drives<WF>, Drives<OB> { }
      public class W                         implements EPR, Drives<A>, Drives<WZ>, Drives<WF> { }
      public class A                         implements EPR { }
      public class WZ   extends And<W, Z>    implements EPR, Drives<XLUT>, Drives<YLUT> { }
      public class WF   extends And<W, F>    implements EPR, Drives<XLUT>, Drives<YLUT> { }
      public class CM   extends Mux<Z, Y, X> implements EPR, Drives<C> { }
      public class XLUT extends LUT<X, Y, A> implements EPR, Drives<CM>, Drives<XO> { }
      public class YLUT extends LUT<A, X, Y> implements EPR, Drives<CM>, Drives<YO> { }
      public class C                         implements EPR, Drives<R>, Drives<F> { }
      public class R    extends Reg<C>       implements EPR, Drives<F>, Drives<XO>, Drives<YO>{ }
      public class XO                        implements EPR, Output { }
      public class YO                        implements EPR, Output { }
      public static class OB   extends Buf<F>>       implements EPR, Drives<L0>, Drives<L1>, Drives<L2>, Drives<L3>, Drives<L4> { }

    */
    //public static L1 L1 = new L1();
    /*
    public static class CellImpl implements
                                 Has<X>,
                                     Has<Y> {
        public void           setDriver(X x, Drives<X> d) { }
        public void           setDriver(Y y, Drives<Y> d) { }
        public void           setDriver(Z z, Drives<Z> d) { }
        public void           setDriver(W w, Drives<W> d) { }
        public Drives<X>      getDriver(X x)            { return null; }
        public Drives<Y>      getDriver(Y y)            { return null; }
        public Drives<Z>      getDriver(Z z)            { return null; }
        public Drives<W>      getDriver(W w)            { return null; }

        public Set<OnBus<L1>> on(L1 l1)                 { return null; }
        public Drives<Y>      getDriver(Y y)            { return null; }
        public void           setDriver(Y y, Drives<Y> d) { }
    }
    */
        /*
    public static abstract class L<D extends C> implements HasPlane, EPR<C> {
        private final int plane;
        L(int plane) { this.plane = plane; }
        public int getPlane() { return plane; }
        public boolean h();
        public void    h(boolean connected);
        public boolean v();
        public void    v(boolean connected);
        public boolean f();
        public void    f(boolean connected);
    }

    L0 = new L<L0>(0);
    L1 = new L<L1>(1);
    L2 = new L<L2>(2);
    L3 = new L<L3>(3);
    L4 = new L<L4>(4);

    

    public static enum L implements XI, YI {
        L0(0), L1(1), L2(2), L3(3) { public int foo() { return 2; } }, L4(4);

        public final int plane;
        public L(int plane) { this.plane = plane; }

    }
        */

    //public static interface L0 extends XI, YI { } public static L0 L0 = new L0() { };

    /*    

    public static enum Xi { NONE, L0, L1, L2, L3, NW, SW, NE, SE, L4; }
    public static enum Yi { NONE, L0, L1, L2, L3, E,  W,  S,  N,  L4; }
    public static enum Wi { NONE, L0, L1, L4, L3, L2; }
    public static enum Zi { NONE, L0, L1, L2, L3, L4; }
    public static enum Ti { W,  Z,  F,  WZ, WF, ONE; }
    public static enum Ci { X, Y, CMux; }
    public static enum Fi { R, C; }
    public static enum Bi { R, C; }
    public static enum Xo { X, B; }
    public static enum Yo { Y, B; }
    public static enum Oe { ONE, H4, V4; }

    public Cell cell(int col, int row) { return new Cell(col, row); }
    public final class Cell {
        public final int col;
        public final int row;
    */
        /** assumes LITTLE endian */
    /*
        protected int onehot(int val) {
            int ret = -1;
            for(int i=0; i<32; i++) {
                if ((val & (1 << i)) != 0) {
                    if (ret != -1) throw new Error("two bits set in a one-hot encoded value");
                    ret = i;
                }
            }
            return ret+1;
        }
        protected int bits(int octet, int bit, int count) {
            return AtmelDevice.this.bits.get(offset, count);
        }

        public void set(Xi xi) { }
        public void set(Yi xi) { }
        public void set(Ti xi) { }
        public void set(Ci xi) { }
        public void set(Fi xi) { }
        public void set(Bi xi) { }
        public void set(Xo xo) { }
        public void set(Yo yo) { }
        public void set(Oe oe) { }

        public Xi xi() { return Xi.values()[onehot((bits(3,3,1)<<8)|bits(5,0,8))]; }
        public Yi yi() { return Yi.values()[onehot((bits(2,1,1)<<8)|bits(4,0,8))]; }
        public Wi wi() { return Wi.values()[onehot((bits(3,0,3)<<2)|bits(3,4,2))]; }
        public Zi zi() { return Zi.values()[onehot((bits(2,0,1)<<4)|bits(2,2,4))]; }
        public Ti ti() { return null; }
        public Ci ci() { return null; }
        public Fi fi() { return null; }
        public Bi bi() { return null; }
        public Xo xo() { return Xo.values()[onehot(bits(1,6,1))]; }
        public Yo yo() { return Yo.values()[onehot(bits(1,7,1))]; }
        public Oe oe() { return Oe.values()[onehot(bits(3,6,2))]; }

        public Sector getSector() { return sector(col - (col % 4), row - (row % 4)); }
        public Cell(int col, int row) {
            this.row = row;
            this.col = col;
        }


        
        
        public static enum EPR { L0, L1, L2, L3, L4 }
        public static enum XInputDriver extends InputDriver {
        public static enum YInputDriver extends InputDriver { N, S, E, W }
        public XInputDriver xi() { }
        public void         xi(XInputDriver) { }
        public YInputDriver yi() { }
        public void         yi(YInputDriver) { }
        public InputDriver  zi() { }
        public void         zi(InputDriver) { }
        public InputDriver  wi() { }
        public void         wi(InputDriver) { }

        public byte         xlut() { }
        public              xlut(byte) { }
        public byte         ylut() { }
        public              ylut(byte) { }

        public static enum CInputDriver { XL, YL, C }
        public static enum FDriver      { R, C }

        public boolean      cRegistered() { }

    }
    */
}
