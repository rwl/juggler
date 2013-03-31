// Copyright 2009 The Go Authors. All rights reserved.
package juggler.examples;

import static juggler.Juggler.go;
import juggler.Channel;
import juggler.Juggler;
import junit.framework.TestCase;

/**
 * Test that unbuffered channels act as pure fifos.
 */
public class Fifo extends TestCase {
    static final int N = 10;

    public void testAsynchFifo() {
        Channel<Integer> ch = new Channel<Integer>(N);
        for (int i = 0; i < N; i++) {
            ch.send(i);
        }
        for (int i = 0; i < N; i++) {
            if (ch.receive() != i) {
                fail();
            }
        }
    }

    /**
     * Thread together a daisy chain to read the elements in sequence
     */
    public void testAynchFifo() {
        Channel<Integer> ch = new Channel<Integer>();
        Channel<Integer> in = new Channel<Integer>();
        Channel<Integer> start = in;
        for (int i = 0; i < N; i++) {
            Channel<Integer> out = new Channel<Integer>();
            go(new Juggler.QuadConsumer<Channel<Integer>, Integer, Channel<Integer>, Channel<Integer>>() {
                @Override
                public void run(Channel<Integer> ch, Integer val, Channel<Integer> in, Channel<Integer> out) {
                    in.receive();
                    if (!ch.receive().equals(val)) {
                        fail(String.valueOf(val));
                    }
                    out.send(1);
                }
            }, ch, i, in, out);
            in = out;
        }
        start.send(0);
        for (int i = 0; i < N; i++) {
            ch.send(i);
        }
        in.receive();
    }
}
