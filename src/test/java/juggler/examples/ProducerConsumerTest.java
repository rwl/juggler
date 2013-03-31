package juggler.examples;

import static juggler.Juggler.go;

import juggler.Channel;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class ProducerConsumerTest extends TestCase {

    static class Producer implements Runnable {
        int n;
        Channel<Integer> c;
        Channel<String> s;

        public Producer(Channel<Integer> c, int n, Channel<String> s) {
            this.n = n;
            this.c = c;
            this.s = s;
        }

        @Override
        public void run() {
            // System.out.println("producer: starting");

            for (int i = 0; i < n; i++) {
                // System.out.printf("producer: %d of %d\n", i+1, n);
                c.send(i);
                // System.out.printf("producer sent: \n", i);
            }

            // System.out.println("producer: finished");

            s.send("producer finished");
        }
    }

    static class Consumer implements Runnable {
        int n;
        Channel<Integer> c;
        Channel<String> s;

        public Consumer(Channel<Integer> c, int n, Channel<String> s) {
            this.n = n;
            this.c = c;
            this.s = s;
        }

        @Override
        public void run() {
            // System.out.println("consumer: starting");

            for (int i = 0; i < n; i++) {
                // System.out.printf("consumer: %d of %d\n", i+1, n);
                int msg = c.receive();
                // System.out.printf("consumer got: %d\n", msg);
            }

            // System.out.println("consumer: finished");

            s.send("producer finished");
        }
    }

    public void testSynch() {
        Channel<Integer> c = new Channel<Integer>();
        Channel<String> s = new Channel<String>();

        go(Producer.class, c, 3, s);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        go(Consumer.class, c, 3, s);

        Set<String> messages = new HashSet<String>();
        messages.add(s.pop());
        messages.add(s.pop());
        assertTrue(messages.contains("producer finished"));
        assertTrue(messages.contains("consumer finished"));

        c.close();
        s.close();
    }

    static class Producer2 implements Runnable {
        Channel<Integer> c;

        public Producer2(Channel<Integer> c) {
            this.c = c;
        }

        @Override
        public void run() {
            int i = 0;
            while (true) {
                c.send(i += 1);
            }
        }
    }

    static class Generator {
        Channel<Integer> pipe;

        public Generator(Channel<Integer> pipe) {
            this.pipe = pipe;
        }
    }

    public void testGenerator() {
        Channel<Integer> c = new Channel<Integer>();
        Generator g = new Generator(c);

        go(Producer2.class, g);

        assertTrue(c.receive() == 1);
        assertTrue(c.receive() == 2);
        assertTrue(c.receive() == 3);

        c.close();
    }
}
