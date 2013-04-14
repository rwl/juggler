package juggler.examples.csp;

import juggler.Channel;
import juggler.Juggler;
import juggler.Juggler.BiConsumer;
import juggler.Selector;
import junit.framework.TestCase;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static juggler.Juggler.go;
import static juggler.Selector.select;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S31_COPY;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S32_SQUASH_EXT;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S33_DISASSEMBLE;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S34_ASSEMBLE;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S36_Conway;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S42_facM;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S43_IntSet;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S43_IntSet.ParIntSetRes;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S43_IntSet.S45_ParIntSet;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S45_HasQuery;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S45_LeastQuery;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S45_LeastResponse;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S51_Buffer;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S52_NewSemaphore;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S52_Semaphore;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S53_DiningPhilosophers;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S61_SIEVE;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S62_NewMatrix;
import static juggler.examples.csp.ConcurrentSequentialProcesses.S62_Matrix;

public class CSPTest extends TestCase {

    // Test abstraction for the programs of section 3 that send runes from
    // a coroutine west to a coroutine east, possibly transforming the
    // string in the process.
    private void testWestEastProgram(BiConsumer<Channel<Character>, Channel<Character>> routine, final String input, String expected/*, Object t testing.T*/) {
        final Channel<Character> west = new Channel<Character>();
        final Channel<Character> east = new Channel<Character>();

        go(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < input.length(); i++) {
                    char r = input.charAt(i);
                    west.send(r);
                }
                west.close();
            }
        });

        go(routine, west, east);

        StringBuilder received = new StringBuilder(50);
        while (east.isOpen()) {
            Character r = east.receive();
            received.append(r);
        }
        if (!received.toString().equals(expected)) {
            fail(received.toString());
        }
    }

    public void testCOPY() {
        testWestEastProgram(S31_COPY, "Hello ** World***", "Hello ** World***");
    }

    public void testSQUASH_EXT() {
        testWestEastProgram(S32_SQUASH_EXT, "Hello ** World***", "Hello ↑ World↑*");
    }

    public void testDISASSEMBLE() {
        final Channel<Character[]> cardfile = new Channel<Character[]>();
        final Channel<Character> X = new Channel<Character>();

        final char[][] in = new char[][] {
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".toCharArray(),
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb".toCharArray()
        };

        String expected = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb ";

        go(new Runnable() {
            @Override
            public void run() {
                for (char[] card : in) {
                    cardfile.send(ArrayUtils.toObject(card));
                }
                cardfile.close();
            }
        });

        go(S33_DISASSEMBLE, cardfile, X);

        StringBuilder actual = new StringBuilder(162);
        while (X.isOpen()) {
            Character r = X.receive();
            actual.append(r);
        }

        if (!actual.toString().equals(expected)) {
            fail("Got " + actual.toString());
        }
    }

    private void stringsMustBeEqual(Character[] a, String b) {
        StringBuilder sb = new StringBuilder(a.length);
        for (Character c : a)
            sb.append(c.charValue());
        if (!sb.toString().equals(b)) {
            fail(String.format("Expected '%v' (len %v), got '%v' (len %v)", sb.toString(), a.length, b, b.length()));
        }
    }

    private void mustBeNLines(List<Character[]> lines, int n) {
        if (lines.size() != n) {
            fail(String.format("Expected %v lines, got %v", n, lines.size()));
        }
    }

    public void testASSEMBLE() {
        final Channel<Character> X = new Channel<Character>();
        final Channel<Character[]> lineprinter = new Channel<Character[]>();

        // 125 a's, 100 b's, the missing 25 at the end should be padded as spaces.
        String line1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String line2 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        final String in = line1 + line2;
        go(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < in.length(); i++) {
                    Character r = in.charAt(i);
                    X.send(r);
                }
                X.close();
            }
        });

        go(S34_ASSEMBLE, X, lineprinter);

        List<Character[]> actual = new LinkedList<Character[]>();
        while (lineprinter.isOpen()) {
            Character[] line = lineprinter.receive();
            if (line.length == 0) {
                break;
            }
            actual.add(line);
        }

        mustBeNLines(actual, 2);
        stringsMustBeEqual(actual.get(0), line1);
        stringsMustBeEqual(actual.get(1), line2+"                         ");
    }

    public void testConway() {
        final Channel<Character[]> cardfile = new Channel<Character[]>();
        final Channel<Character[]> lineprinter = new Channel<Character[]>();

        final String card1 = "**aa*aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        final String card2 = "b**bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb*b*";

        String expected = (card1+" "+card2+" ").replace("**", "↑");
        String expected1 = expected.substring(0, 125);  // first line
        String expected2 = expected.substring(125);  // second line
        // "the last line should be completed with spaces if necessary"
        expected2 += StringUtils.repeat(" ", 125 - expected.substring(125).length());

        go(new Runnable() {
            @Override
            public void run() {
                cardfile.send(ArrayUtils.toObject(card1.toCharArray()));
                cardfile.send(ArrayUtils.toObject(card2.toCharArray()));
                cardfile.close();
            }
        });

        go(S36_Conway, cardfile, lineprinter);

        List<Character[]> actual = new LinkedList<Character[]>();
        while (lineprinter.isOpen()) {
            Character[] line = lineprinter.receive();
            if (line.length == 0) {
                break;
            }
            actual.add(line);
        }

        mustBeNLines(actual, 2);
        stringsMustBeEqual(actual.get(0), expected1);
        stringsMustBeEqual(actual.get(1), expected2);
    }

    public void testFac() {
        Channel<Integer> f = S42_facM(8);
        int[][] pairs = new int[][] {
                new int[] {0, 1},
                new int[] {1, 1},
                new int[] {4, 24},
                new int[] {8, 40320}
        };
        for (int[] pair : pairs) {
            f.send(pair[0]);
            Integer res = f.receive();
            if (res != pair[1]) {
                fail(String.format("Expected %v! == %v, but got %v\n", pair[0], pair[1], res));
            }
        }
    }

    private void expect(boolean b, String op) {
        if (!b) {
            fail(String.format("%v failed", op));
        }
    }

    public void testIntSet() {
        S43_IntSet set = S43_IntSet.newIntSet();
        Channel<Boolean> hasChan = new Channel<Boolean>();

        set.Has.run(0, hasChan);
        expect(!hasChan.receive(), "empty !has(0)");
        set.Has.run(1, hasChan);
        expect(!hasChan.receive(), "empty !has(1)");
        set.Has.run(100, hasChan);
        expect(!hasChan.receive(), "empty !has(100)");

        set.Insert.run(34523, null);
        set.Has.run(0, hasChan);
        expect(!hasChan.receive(), "{34523} !has(0)");
        set.Has.run(34523, hasChan);
        expect(hasChan.receive(), "{34523} has(34523)");

        // Parallel use, Insert() must lock.
//        runtime.GOMAXPROCS(runtime.NumCPU())
        int n = 10;

        Channel<Integer> ack = new Channel<Integer>();
        for (int i = 0; i < n; i++) {
            go(set.Insert, i, ack);
        }
        // Wait until all are inserted.
        for (int i = 0; i < n; i++) {
            ack.receive();
        }

        for (int i = 0; i < n; i++) {
            set.Has.run(i, hasChan);
            expect(hasChan.receive(), "parallel insertions");
        }
    }

    public void testIntSetScan() {
        S43_IntSet set = S43_IntSet.newIntSet();

        int n = 10;
        int[] expected = new int[n];
        for (int i = 0; i < n; i++) {
            expected[i] = i + 23;
        }

        Channel<Integer> ack = new Channel<Integer>();
        for (int nn : expected) {
            set.Insert.run(nn, ack);
        }
        for (int i = 0; i < n; i++) {
            ack.receive();
        }

        List<Integer> actual = new LinkedList<Integer>(/*n*/);
        Channel<Integer> elements = set.Scan();
        while (elements.isOpen()) {
            actual.add(elements.receive());
        }
        Collections.sort(actual);

        if (actual.size() != expected.length) {
            fail(String.format("Expected %v elements, got %v: %v", expected.length,
                    actual.size(), actual));
        }
    }

    public void testParIntSet() {
//        runtime.GOMAXPROCS(runtime.NumCPU())

        ParIntSetRes res = S45_ParIntSet(10);
        Channel<Integer> insert = res.insert;
        Channel<S45_HasQuery> has = res.has;
        Channel<Channel<Integer>> scan = res.scan;
        Channel<S45_LeastQuery> least = res.leastQuery;

        // The empty set does not contain any number,
        Channel<Boolean> hasResponseChan = new Channel<Boolean>();
        for (int num : new int[] {0, 10}) {
            has.send(new S45_HasQuery(num, hasResponseChan));
            if (hasResponseChan.receive()) {
                fail("ParSet contains " + num);
            }
        }

        // After insertion, it contains that number but no other ones..
        for (int num : new int[] {0, 7, 5}) {
            insert.send(num);

            has.send(new S45_HasQuery(num, hasResponseChan));
            Boolean h = hasResponseChan.receive();
            if (!h) {
                fail("ParSet doesn't contain " + num);
            }
        }
        has.send(new S45_HasQuery(1, hasResponseChan));
        if (hasResponseChan.receive()) {
            fail("ParSet {0} contains 1");
        }

        // Scan.
        Channel<Integer> scanRcvr = new Channel<Integer>();
        scan.send(scanRcvr);
        int[] expected = new int[] {0, 5, 7};
        int i = 0;
        while (scanRcvr.isOpen()) {
            Integer n = scanRcvr.receive();
            if (n != expected[i]) {
                fail(String.format("Expected %v as %vth number in Scan.", expected[i], i));
            }
            i++;
        }

        // Successively ask for the least member and check that it was removed.
        i = 0;
        Channel<S45_LeastResponse> leastResp = new Channel<S45_LeastResponse>();
        while (true) {
            least.send(leastResp);
            S45_LeastResponse l = leastResp.receive();
            if (l.NoneLeft) {
                break;
            }
            if (l.Least != expected[i]) {
                fail(String.format("Expected %v as %vth least number, got %v.", expected[i], i, l.Least));
            }

            // The least operation is defined as removing the returned
            // element from the set.
            has.send(new S45_HasQuery(expected[i], hasResponseChan));
            if (hasResponseChan.receive()) {
                fail(String.format("Just removed %v from the set, but has says it's still there.", expected[i]));
            }

            i++;
        }
        if (i != 2) {
            fail(String.format("Expected to get %v least members, got %v.", expected.length, i));
        }
    }

    // 5.1
    public void testBuffer() {
//        runtime.GOMAXPROCS(runtime.NumCPU())

        final Channel<Integer>[] res = S51_Buffer(10);
        final Channel<Integer> consumer = res[0];
        final Channel<Integer> producer = res[1];

        for (int i = 0; i < 10; i++) {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.sendCase(producer, i);
                    s.timeout(2000, new Selector.SelectorBlock() {
                        @Override
                        public void yield(Selector s) {
                            fail(String.format("Producer couldn't send %vth value", i));
                        }
                    });
                }
            });
        }
        // We should get here without blocking since the buffer stores the
        // ten portions.

        final int[] received = new int[10];
        for (final int i = 0; i < 10; i++) {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.receiveCase(consumer, new Selector.ReceiveBlock<Integer>() {
                        @Override
                        public void yield(Integer r) {
                            received[i] = r;
                            if (received[i] != i) {
                                fail(String.format("Received %v as %vth number.", received[i], i));
                            }
                        }
                    });
                    s.timeout(2000, new Selector.SelectorBlock() {
                        @Override
                        public void yield(Selector s) {
                            fail(String.format("Consumer couldn't receive %vth value", i));
                            return;
                        }
                    });
                }
            });
        }
    }

    public void testSemaphore() {
        final S52_Semaphore s = S52_NewSemaphore();

        // We cannot decrement before an increment.
        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector sel) {
                sel.sendCase(s.dec, new Object(), new Selector.SendBlock() {
                    @Override
                    public void yield() {
                        fail("Shouldn't be able to dec before inc");
                    }
                });
                sel.timeout(2000, new Selector.SelectorBlock() {
                    @Override
                    public void yield(Selector s) {
                        // ok, dec blocked
                    }
                });
            }
        });

        // After an inc, dec will work.
        s.inc.send(new Object());
        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector sel) {
                sel.sendCase(s.dec, new Object());
                sel.timeout(2000, new Selector.SelectorBlock() {
                    @Override
                    public void yield(Selector s) {
                        fail("Should be able to dec after inc");
                    }
                });
            }
        });
    }

    /**
     * That's not actually a test, we just let the scenario run for 10
     * seconds so we can observe the log.
     */
    public void testDiningPhilosophers() {
//        runtime.GOMAXPROCS(runtime.NumCPU())
        S53_DiningPhilosophers(10000);
    }

    public void testPrimeSieve() {
//        runtime.GOMAXPROCS(runtime.NumCPU())

        int numPrimes = 100;

        // Copied from http://primes.utm.edu/lists/small/10000.txt
        String first100PrimesStr = "2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97 101 103 107 109 113 127 131 137 139 149 151 157 163 167 173 179 181 191 193 197 199 211 223 227 229 233 239 241 251 257 263 269 271 277 281 283 293 307 311 313 317 331 337 347 349 353 359 367 373 379 383 389 397 401 409 419 421 431 433 439 443 449 457 461 463 467 479 487 491 499 503 509 521 523 541";
        String[] first100Primes = first100PrimesStr.split(" ");

        final List<Integer> primes = new ArrayList<Integer>(numPrimes);
        final Channel<Integer> primeChan = new Channel<Integer>();

        final Channel<Boolean> doneChan = new Channel<Boolean>();
        go(new Runnable() {
            @Override
            public void run() {
                while (primeChan.isOpen()) {
                    int p = primeChan.receive();
                    if (p == -1) {
                        doneChan.send(true);
                        return;
                    }
                    primes.add(p);
                }
            }
        });

        S61_SIEVE(numPrimes, primeChan);
        doneChan.receive();

        int l = first100Primes.length;
        if (primes.size() != l) {
            fail(String.format("Expected %v primes, but got %v.", l, primes.size()));
        }

        // As SIEVE runs concurrently on possibly multiple cores, the
        // primes can arrive out of order.
        Collections.sort(primes);

        for (int i = 0; i < l; i++) {
            if (primes.get(i) != Integer.valueOf(first100Primes[i])) {
                fail(String.format("Expected %v as %vnth prime, got %v.", first100Primes[i], i, primes.get(i)));
            }
        }
    }

    public void testMatrixMultiply() {
//        runtime.GOMAXPROCS(runtime.NumCPU())

        double[][] A = new double[][] {
            new double[] {1, 2, 3},
            new double[] {4, 5, 6},
            new double[] {7, 8, 9}
        };
        final S62_Matrix matrix = S62_NewMatrix(A);

        final double[][][] other = new double[][][] {
                new double[][] {
                        new double[] {1, 1, 1},
                        new double[] {1, 1, 1},
                        new double[] {1, 1, 1}
                },
                new double[][] {
                        new double[] {1, 1, 1},
                        new double[] {2, 2, 2},
                        new double[] {3, 3, 3}
                },
                new double[][] {
                        new double[] {1, 2, 3},
                        new double[] {1, 2, 3},
                        new double[] {1, 2, 3}
                }
        };

        final double[][][] expected = new double[][][] {
                new double[][] {
                        new double[] {12, 15, 18},
                        new double[] {12, 15, 18},
                        new double[] {12, 15, 18}
                },
                new double[][] {
                        new double[] {12, 15, 18},
                        new double[] {24, 30, 36},
                        new double[] {36, 45, 54}
                },
                new double[][] {
                        new double[] {30, 36, 42},
                        new double[] {30, 36, 42},
                        new double[] {30, 36, 42}
                }
        };
        assert other.length == expected.length;

        for (int a = 0; a < other.length; a++) {
            final double[][] other_a = other[a];
            final double[][] expected_a = expected[a];

            final double[][] result = new double[3][];
            final Object/*sync.WaitGroup*/ wg;
            for (int i = 0; i < 3; i++) {
                result[i] = new double[3];
                wg.add(1);
                go(new Juggler.Consumer<Integer>() {
                    @Override
                    public void run(Integer ii) {
                        for (int j = 0; j < 3; j++) {
                            Double val = matrix.SOUTH[ii].receive();
                            result[j][ii] = val;
                        }
                        wg.done();
                    }
                }, i);
            }

            for (int i = 0; i < 3; i++) {
                go(new Juggler.Consumer<Integer>() {
                    @Override
                    public void run(Integer ii) {
                        for (int j = 0; j < 3; j++) {
                            matrix.WEST[j].send(other_a[ii][j]);
                        }
                    }
                }, i);
            }

            wg.wait();

            if (!matricesEqual(result, expected_a)) {
                fail(String.format("Expected \n%v, got \n%v",
                        printMatrix(expected_a), printMatrix(result)));
            }
        }
    }

    private boolean matricesEqual(double[][] a, double[][] b) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (a[i][j] != b[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private String printMatrix(double[][] m) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                b.append(String.format("%v ", m[i][j]));
            }
            b.append("\n");
        }

        return b.toString();
    }
}
