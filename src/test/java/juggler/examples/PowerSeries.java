package juggler.examples;

import juggler.Channel;
import juggler.Juggler;
import juggler.Selector;
import sun.plugin.dom.exception.InvalidStateException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static juggler.Selector.select;
import static juggler.Juggler.go;


public class PowerSeries {

    private static class Rat {
        long num, den;	// numerator, denominator

        void pr() {
            if (den==1) {
                System.out.print(num);
            } else {
                System.out.print(num + "/" + den);
            }
            System.out.print(" ");
        }

        boolean eq(Rat c) {
            return num == c.num && den == c.den;
        }
    }

    private static class dch {
        Channel<Integer> req;
        Channel<Rat> dat;
        int nam;
    }

//    type dch2 [2] *dch

    private static String chnames;
    private static int chnameserial;
    private static int seqno;

    private static dch mkdch() {
        int c = chnameserial % chnames.length();
        chnameserial++;
        dch d = new dch();
        d.req = new Channel<Integer>();
        d.dat = new Channel<Rat>();
        d.nam = c;
        return d;
    }

    private static dch[] mkdch2() {
        dch[] d2 = new dch[2];
        d2[0] = mkdch();
        d2[1] = mkdch();
        return d2;
    }

    // split reads a single demand channel and replicates its
    // output onto two, which may be read at different rates.
    // A process is created at first demand for a rat and dies
    // after the rat has been sent to both outputs.

    // When multiple generations of split exist, the newest
    // will service requests on one channel, which is
    // always renamed to be out[0]; the oldest will service
    // requests on the other channel, out[1].  All generations but the
    // newest hold queued data that has already been sent to
    // out[0].  When data has finally been sent to out[1],
    // a signal on the release-wait channel tells the next newer
    // generation to begin servicing out[1].

    private static Juggler.TriConsumer<dch, dch[], Channel<Integer>> dosplit = new Juggler.TriConsumer<dch, dch[], Channel<Integer>>() {
        @Override
        public void run(final dch in, final dch[] out, final Channel<Integer> wait) {
            final AtomicBoolean both = new AtomicBoolean(false);	// do not service both channels

            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.receiveCase(out[0].req);

                    s.receiveCase(wait, new Selector.ReceiveBlock<Integer>() {
                        @Override
                        public void yield(Integer value) {
                            both.set(true);

                            select(new Selector.SelectorBlock() {
                                @Override
                                public void yield(Selector ss) {
                                    ss.receiveCase(out[0].req);

                                    ss.receiveCase(out[1].req, new Selector.ReceiveBlock<Integer>() {
                                        @Override
                                        public void yield(Integer value) {
                                            dch temp = out[0];
                                            out[0] = out[1];
                                            out[1] = temp;
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });

            seqno++;
            in.req.send(seqno);
            Channel<Integer> release = new Channel<Integer>();
            go(dosplit, in, out, release);
            Rat dat = in.dat.receive();
            out[0].dat.send(dat);
            if (!both.get()) {
                wait.receive();
            }
            out[1].req.receive();
            out[1].dat.send(dat);
            release.send(0);
        }
    };

    private static final Juggler.BiConsumer<dch, dch[]> split = new Juggler.BiConsumer<dch, dch[]>() {
        @Override
        public void run(dch in, dch[] out) {
            Channel<Integer> release = new Channel<Integer>();
            go(dosplit, in, out, release);
            release.send(0);
        }
    };

    private static void put(Rat dat, dch out) {
        out.req.receive();
        out.dat.send(dat);
    }

    private static Rat get(dch in) {
        seqno++;
        in.req.send(seqno);
        return in.dat.receive();
    }

    // Get one rat from each of n demand channels

    private static Rat[] getn(final dch[] in) {
        int n = in.length;
        if (n != 2) {
            throw new IllegalArgumentException("bad n in getn");
        }
        final Channel<Integer>[] req = new Channel[2];
        final Channel<Rat>[] dat = new Channel[2];
        final Rat[] out = new Rat[2];
        int i;
        Rat it;
        for (i=0; i<n; i++) {
            req[i] = in[i].req;
            dat[i] = null;
        }
        for (n=2*n; n>0; n--) {
            seqno++;

            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.sendCase(req[0], seqno, new Selector.SendBlock() {
                        @Override
                        public void yield() {
                            dat[0] = in[0].dat;
                            req[0] = null;
                        }
                    });
                    s.sendCase(req[1], seqno, new Selector.SendBlock() {
                        @Override
                        public void yield() {
                            dat[1] = in[1].dat;
                            req[1] = null;
                        }
                    });
                    s.receiveCase(dat[0], new Selector.ReceiveBlock<Rat>() {
                        @Override
                        public void yield(Rat it) {
                            out[0] = it;
                            dat[0] = null;
                        }
                    });
                    s.receiveCase(dat[1], new Selector.ReceiveBlock<Rat>() {
                        @Override
                        public void yield(Rat it) {
                            out[1] = it;
                            dat[1] = null;
                        }
                    });
                }
            });
        }
        return out;
    }

    // Get one rat from each of 2 demand channels

    private static Rat[] get2(dch in0, dch in1) {
        return getn(new dch[] {in0, in1});
    }

    private static void copy(dch in, dch out) {
        while (true) {
            out.req.receive();
            out.dat.send(get(in));
        }
    }

    private static final Juggler.BiConsumer<Rat, dch> repeat = new Juggler.BiConsumer<Rat, dch>() {
        @Override
        public void run(Rat dat, dch out) {
            while (true) {
                put(dat, out);
            }
        }
    };

    public static class PS extends dch {}	// power series
//    type PS2 *[2] PS // pair of power series

    public static PS Ones;
    public static PS Twos;

    private static PS mkPS() {
        return (PS) mkdch();  // FIXME: upcast
    }

    static PS[] mkPS2() {
        dch[] dhc2 = mkdch2();
        return new PS[] {(PS) dhc2[0], (PS) dhc2[1]};  // FIXME: upcast
    }

    // Conventions
    // Upper-case for power series.
    // Lower-case for rationals.
    // Input variables: U,V,...
    // Output variables: ...,Y,Z

    // Integer gcd; needed for rational arithmetic

    static long gcd(long u, long v) {
        if (u < 0) { return gcd(-u, v); }
        if (u == 0) { return v; }
        return gcd(v%u, u);
    }

    // Make a rational from two ints and from one int

    static Rat i2tor(long u, long v) {
        long g = gcd(u, v);
        Rat r = new Rat();
        if (v > 0) {
            r.num = u/g;
            r.den = v/g;
        } else {
            r.num = -u/g;
            r.den = -v/g;
        }
        return r;
    }

    static Rat itor(long u) {
        return i2tor(u, 1);
    }

    static Rat zero;
    static Rat one;


    // End mark and end test

    static Rat finis;

    static long end(Rat u) {
        if (u.den==0) { return 1; }
        return 0;
    }

    // Operations on rationals

    static Rat add(Rat u, Rat v) {
        long g = gcd(u.den, v.den);
        return  i2tor(u.num*(v.den/g)+v.num*(u.den/g),u.den*(v.den/g));
    }

    static Rat mul(Rat u, Rat v) {
        long g1 = gcd(u.num, v.den);
        long g2 = gcd(u.den, v.num);
        Rat r = new Rat();
        r.num = (u.num/g1)*(v.num/g2);
        r.den = (u.den/g2)*(v.den/g1);
        return r;
    }

    static Rat neg(Rat u) {
        return i2tor(-u.num, u.den);
    }

    static Rat sub(Rat u, Rat v) {
        return add(u, neg(v));
    }

    static Rat inv(Rat u) {	// invert a rat
        if (u.num == 0) {
            throw new IllegalArgumentException("zero divide in inv");
        }
        return i2tor(u.den, u.num);
    }

    // print eval in floating point of PS at x=c to n terms
    static void evaln(Rat c, PS U, int n) {
        double xn = 1.0;
        double x = new Double(c.num)/new Double(c.den);
        double val = 0.0;
        for (int i=0; i<n; i++) {
            Rat u = get(U);
            if (end(u) != 0) {
                break;
            }
            val = val + x * new Double(u.num)/new Double(u.den);
            xn = xn*x;
        }
        System.out.print(val + "\n");
    }

    // Print n terms of a power series
    static void printn(PS U, int n) {
        boolean done = false;
        for (; !done && n>0; n--) {
            Rat u = get(U);
            if (end(u) != 0) {
                done = true;
            } else {
                u.pr();
            }
        }
        System.out.print("\n");
    }

    // Evaluate n terms of power series U at x=c
    static Rat eval(Rat c, PS U, int n) {
        if (n==0) { return zero; }
        Rat y = get(U);
        if (end(y) != 0) { return zero; }
        return add(y,mul(c,eval(c,U,n-1)));
    }

    // Power-series constructors return channels on which power
    // series flow.  They start an encapsulated generator that
    // puts the terms of the series on the channel.

    // Make a pair of power series identical to a given power series

    static PS[] Split(PS U) {
        dch[] UU = mkdch2();
        go(split,U,UU);
        return new PS[] {(PS) UU[0], (PS) UU[1]};  // FIXME: unchecked cast
    }

    // Add two power series
    static PS Add(final PS U, final PS V) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                Rat[] uv;
                while (true) {
                    Z.req.receive();
                    uv = get2(U,V);
                    switch ((int) (end(uv[0]) + 2*end(uv[1]))) {  // FIXME long switch
                        case 0:
                            Z.dat.send(add(uv[0], uv[1]));
                        case 1:
                            Z.dat.send(uv[1]);
                            copy(V,Z);
                        case 2:
                            Z.dat.send(uv[0]);
                            copy(U,Z);
                        case 3:
                            Z.dat.send(finis);
                    }
                }
            }
        });
        return Z;
    }

    // Multiply a power series by a constant
    static PS Cmul(final Rat c, final PS U) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                boolean done = false;
                while (!done) {
                    Z.req.receive();
                    Rat u = get(U);
                    if (end(u) != 0) {
                        done = true;
                    } else {
                        Z.dat.send(mul(c,u));
                    }
                }
                Z.dat.send(finis);
            }
        });
        return Z;
    }

    // Subtract

    static PS Sub(PS U, PS V) {
        return Add(U, Cmul(neg(one), V));
    }

    // Multiply a power series by the monomial x^n

    static PS Monmul(final PS U, int nn) {
        final AtomicInteger n = new AtomicInteger(nn);
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                for (; n.get()>0; n.getAndDecrement()) {
                    put(zero,Z);
                }
                copy(U,Z);
            }
        });
        return Z;
    }

    // Multiply by x

    static PS Xmul(PS U) {
        return Monmul(U,1);
    }

    static PS Rep(Rat c) {
        PS Z = mkPS();
        go(repeat, c, Z);
        return Z;
    }

    // Monomial c*x^n

    static PS Mon(final Rat c, int nn) {
        final AtomicInteger n = new AtomicInteger(nn);
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                if(c.num!=0) {
                    for (; n.get()>0; n.getAndDecrement()) {
                        put(zero,Z);
                    }
                    put(c,Z);
                }
                put(finis,Z);
            }
        });
        return Z;
    }

    static PS Shift(final Rat c, final PS U) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                put(c,Z);
                copy(U,Z);
            }
        });
        return Z;
    }

    // simple pole at 1: 1/(1-x) = 1 1 1 1 1 ...

    // Convert array of coefficients, constant term first
    // to a (finite) power series

    /*
    func Poly(a []rat) PS {
        Z:=mkPS()
        begin func(a []rat, Z PS) {
            j:=0
            done:=0
            for j=len(a); !done&&j>0; j=j-1)
                if(a[j-1].num!=0) done=1
            i:=0
            for(; i<j; i=i+1) put(a[i],Z)
            put(finis,Z)
        }()
        return Z
    }
    */

    // Multiply. The algorithm is
    //	let U = u + x*UU
    //	let V = v + x*VV
    //	then UV = u*v + x*(u*VV+v*UU) + x*x*UU*VV

    static PS Mul(final PS U, final PS V) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                Z.req.receive();
                Rat[] uv = get2(U, V);
                if (end(uv[0])!=0 || end(uv[1]) != 0) {
                    Z.dat.send(finis);
                } else {
                    Z.dat.send(mul(uv[0],uv[1]));
                    PS[] UU = Split(U);
                    PS[] VV = Split(V);
                    PS W = Add(Cmul(uv[0], VV[0]), Cmul(uv[1], UU[0]));
                    Z.req.receive();
                    Z.dat.send(get(W));
                    copy(Add(W,Mul(UU[1],VV[1])),Z);
                }
            }
        });
        return Z;
    }

    // Differentiate

    public static PS Diff(final PS U) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                Z.req.receive();
                Rat u = get(U);
                if (end(u) == 0) {
                    boolean done = false;
                    for (int i = 1; !done; i++) {
                        u = get(U);
                        if (end(u) != 0) {
                            done = true;
                        } else {
                            Z.dat.send(mul(itor((long) i), u));
                            Z.req.receive();
                        }
                    }
                }
                Z.dat.send(finis);
            }
        });
        return Z;
    }

    // Integrate, with const of integration
    public static PS Integ(final Rat c, final PS U) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                put(c,Z);
                boolean done = false;
                for (int i=1; !done; i++) {
                    Z.req.receive();
                    Rat u = get(U);
                    if (end(u) != 0) { done = true; }
                    Z.dat.send(mul(i2tor(1, (long) i), u));
                }
                Z.dat.send(finis);
            }
        });
        return Z;
    }

    // Binomial theorem (1+x)^c

    public static PS Binom(final Rat cc) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                Rat c = cc;  // TODO: double check
                int n = 1;
                Rat t = itor(1);
                while (c.num!=0) {
                    put(t,Z);
                    t = mul(mul(t,c),i2tor(1,(long) n));
                    c = sub(c,one);
                    n++;
                }
                put(finis,Z);
            }
        });
        return Z;
    }

    // Reciprocal of a power series
    //	let U = u + x*UU
    //	let Z = z + x*ZZ
    //	(u+x*UU)*(z+x*ZZ) = 1
    //	z = 1/u
    //	u*ZZ + z*UU +x*UU*ZZ = 0
    //	ZZ = -UU*(z+x*ZZ)/u

    public static PS Recip(final PS U) {
        final PS Z=mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                PS[] ZZ = mkPS2();
                Z.req.receive();
                Rat z = inv(get(U));
                Z.dat.send(z);
                split.run(Mul(Cmul(neg(z),U),Shift(z,ZZ[0])),ZZ);
                copy(ZZ[1],Z);
            }
        });
        return Z;
    }

    // Exponential of a power series with constant term 0
    // (nonzero constant term would make nonrational coefficients)
    // bug: the constant term is simply ignored
    //	Z = exp(U)
    //	DZ = Z*DU
    //	integrate to get Z

    public static PS Exp(PS U) {
        PS[] ZZ = mkPS2();
        split.run(Integ(one,Mul(ZZ[0],Diff(U))),ZZ);
        return ZZ[1];
    }

    // Substitute V for x in U, where the leading term of V is zero
    //	let U = u + x*UU
    //	let V = v + x*VV
    //	then S(U,V) = u + VV*S(V,UU)
    // bug: a nonzero constant term is ignored

    public static PS Subst(final PS U, final PS V) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                PS[] VV = Split(V);
                Z.req.receive();
                Rat u = get(U);
                Z.dat.send(u);
                if (end(u) == 0) {
                    if (end(get(VV[0])) != 0) {
                        put(finis,Z);
                    } else {
                        copy(Mul(VV[0],Subst(U,VV[1])),Z);
                    }
                }
            }
        });
        return Z;
    }

    // Monomial Substition: U(c x^n)
    // Each Ui is multiplied by c^i and followed by n-1 zeros

    public static PS MonSubst(final PS U, final Rat c0, final int n) {
        final PS Z = mkPS();
        go(new Runnable() {
            @Override
            public void run() {
                Rat c = one;
                while (true) {
                    Z.req.receive();
                    Rat u = get(U);
                    Z.dat.send(mul(u, c));
                    c = mul(c, c0);
                    if (end(u) != 0) {
                        Z.dat.send(finis);
                        break;
                    }
                    for (int i = 1; i < n; i++) {
                        Z.req.receive();
                        Z.dat.send(zero);
                    }
                }
            }
        });
        return Z;
    }


    public static void Init() {
        chnameserial = -1;
        seqno = 0;
        chnames = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        zero = itor(0);
        one = itor(1);
        finis = i2tor(1,0);
        Ones = Rep(one);
        Twos = Rep(itor(2));
    }

    private static void check(PS U, Rat c, int count, String str) {
        for (int i = 0; i < count; i++) {
            Rat r = get(U);
            if (!r.eq(c)) {
                System.out.print("got: ");
                r.pr();
                System.out.print("should get ");
                c.pr();
                System.out.print("\n");
                throw new IllegalStateException(str);
            }
        }
    }

    public static final int N = 10;

    private static void checka(PS U, Rat[] a, String str) {
        for (int i = 0; i < N; i++) {
            check(U, a[i], 1, str);
        }
    }

    public static void main(String[] Args) {
        Init();
        if (Args.length > 1) {  // print
            System.out.print("Ones: "); printn(Ones, 10);
            System.out.print("Twos: "); printn(Twos, 10);
            System.out.print("Add: "); printn(Add(Ones, Twos), 10);
            System.out.print("Diff: "); printn(Diff(Ones), 10);
            System.out.print("Integ: "); printn(Integ(zero, Ones), 10);
            System.out.print("CMul: "); printn(Cmul(neg(one), Ones), 10);
            System.out.print("Sub: "); printn(Sub(Ones, Twos), 10);
            System.out.print("Mul: "); printn(Mul(Ones, Ones), 10);
            System.out.print("Exp: "); printn(Exp(Ones), 15);
            System.out.print("MonSubst: "); printn(MonSubst(Ones, neg(one), 2), 10);
            System.out.print("ATan: "); printn(Integ(zero, MonSubst(Ones, neg(one), 2)), 10);
        } else {  // test
            check(Ones, one, 5, "Ones");
            check(Add(Ones, Ones), itor(2), 0, "Add Ones Ones");  // 1 1 1 1 1
            check(Add(Ones, Twos), itor(3), 0, "Add Ones Twos"); // 3 3 3 3 3
            Rat[] a = new Rat[N];
            PS d = Diff(Ones);
            for (int i=0; i < N; i++) {
                a[i] = itor((long) i+1);
            }
            checka(d, a, "Diff");  // 1 2 3 4 5
            PS in = Integ(zero, Ones);
            a[0] = zero;  // integration constant
            for (int i=1; i < N; i++) {
                a[i] = i2tor(1, (long) i);
            }
            checka(in, a, "Integ");  // 0 1 1/2 1/3 1/4 1/5
            check(Cmul(neg(one), Twos), itor(-2), 10, "CMul");  // -1 -1 -1 -1 -1
            check(Sub(Ones, Twos), itor(-1), 0, "Sub Ones Twos");  // -1 -1 -1 -1 -1
            PS m = Mul(Ones, Ones);
            for (int i=0; i < N; i++) {
                a[i] = itor((long) i+1);
            }
            checka(m, a, "Mul");  // 1 2 3 4 5
            PS e = Exp(Ones);
            a[0] = itor(1);
            a[1] = itor(1);
            a[2] = i2tor(3,2);
            a[3] = i2tor(13,6);
            a[4] = i2tor(73,24);
            a[5] = i2tor(167,40);
            a[6] = i2tor(4051,720);
            a[7] = i2tor(37633,5040);
            a[8] = i2tor(43817,4480);
            a[9] = i2tor(4596553,362880);
            checka(e, a, "Exp");  // 1 1 3/2 13/6 73/24
            PS at = Integ(zero, MonSubst(Ones, neg(one), 2));
            int c = 1;
            for (int i = 0; i < N; i++) {
                if (i%2 == 0) {
                    a[i] = zero;
                } else {
                    a[i] = i2tor((long) c, (long) i);
                    c *= -1;
                }
            }
            checka(at, a, "ATan");  // 0 -1 0 -1/3 0 -1/5
/*
		t := Revert(Integ(zero, MonSubst(Ones, neg(one), 2)))
		a[0] = zero
		a[1] = itor(1)
		a[2] = zero
		a[3] = i2tor(1,3)
		a[4] = zero
		a[5] = i2tor(2,15)
		a[6] = zero
		a[7] = i2tor(17,315)
		a[8] = zero
		a[9] = i2tor(62,2835)
		checka(t, a, "Tan")  // 0 1 0 1/3 0 2/15
*/
        }
    }
}
