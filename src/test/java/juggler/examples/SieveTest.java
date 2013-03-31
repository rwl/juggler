package juggler.examples;

import static juggler.Juggler.go;

import juggler.Channel;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Sieve of Eratosthenes
 *
 * http://golang.org/doc/go_tutorial.html#tmp_353
 */
public class SieveTest extends TestCase {

    /**
     * send the sequence 2,3,4, ... to returned channel
     */
    private Channel<Integer> generate(List<Channel<Integer>> channels) {
        final Channel<Integer> ch = new Channel<Integer>();
        channels.add(ch);
        go(new Runnable() {
            @Override
            public void run() {
                int i = 1;
                while (true) {
                    ch.send(i += 1);
                }
            }
        });

        return ch;
    }

    /**
     * Filter out input values divisible by *prime*, send rest to returned channel
     */
    private Channel<Integer> filter(final Channel<Integer> in_channel,
                                    final int prime,
                                    List<Channel<Integer>> channels) {
        final Channel<Integer> out = new Channel<Integer>();
        channels.add(out);

        go(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    int i = in_channel.receive();
                    if (i % prime != 0) {
                        out.send(i);
                    }
                }
            }
        });

        return out;
    }

    private Channel<Integer> sieve(final List<Channel<Integer>> channels) {
        final Channel<Integer> out = new Channel<Integer>();
        channels.add(out);

        go(new Runnable() {
            @Override
            public void run() {
                Channel<Integer> ch = generate(channels);
                while (true) {
                    int prime = ch.receive();
                    out.send(prime);
                    ch = filter(ch, prime, channels);
                }
            }
        });
        return out;
    }

    public void testChannels() {
        // run the sieve
        int n = 20;
        boolean nth = false;
        List<Channel<Integer>> channels = new LinkedList<Channel<Integer>>();

        Channel<Integer> primes = sieve(channels);
        List<Integer> result = new ArrayList<Integer>();

        if (nth) {
            for (int i = 0; i < n; i++) {
                primes.receive();
            }
            System.out.print(primes.receive());
        } else {
            while (true) {
                int p = primes.receive();

                if (p <= n) {
                    result.add(p);
                } else {
                    break;
                }
            }
        }

        int[] ref = new int[] {2, 3, 5, 7, 11, 13, 17, 19};
        for (int i = 0; i < result.size(); i++) {
            assertTrue(result.get(i) == ref[i]);
        }
        for (Channel<Integer> ch : channels) {
            ch.close();
        }
    }
}
