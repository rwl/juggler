package juggler;

import static juggler.Juggler.go;

import juggler.errors.ChannelClosedError;
import juggler.errors.Rollback;
import junit.framework.TestCase;

import java.util.concurrent.atomic.AtomicInteger;

public class PushTest extends TestCase {

    Push<String> push;
    Channel<Long> ack;

    protected void setUp() throws Exception {
        push = new Push<String>("1");
        ack = new Channel<Long>();
    }

    public void testClose() {
        assertFalse(push.isClosed());
        push.close();
        assertTrue(push.isClosed());
    }

    /**
     * It should run multiple times.
     */
    public void testMultiRun() {
        final AtomicInteger i = new AtomicInteger(0);
        push.receive(new Push.PushBlock<String>() {
            @Override
            public void yield(String v) {
                i.getAndIncrement();
            }
        });
        assertTrue(push.sent());
        push.receive(new Push.PushBlock<String>() {
            @Override
            public void yield(String obj) {
                i.getAndIncrement();
            }
        });
        assertTrue(i.intValue() == 2);
    }

    /**
     * It should continue when sent.
     */
    public void testContinue() {
        go(new Runnable() {
            public void run() {
                push.await();
                ack.send(System.currentTimeMillis());
            }
        });
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final long[] s = new long[1];
        push.receive(new Push.PushBlock<String>() {
            @Override
            public void yield(String v) {
                s[0] = ack.receive();
            }
        });

        long t = System.currentTimeMillis() - s[0];
        assertTrue(t > -10);
        assertTrue(t < 10);
    }

    /**
     * It should raise an error on the waiter when closed.
     */
    public void testError() {
        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                push.close();
            }
        });
        try {
            push.await();
            fail();
        } catch (ChannelClosedError e) {
        }
    }

    /**
     * It should be able to be gracefully rolled back.
     */
    public void testRollback() {
        assertFalse(push.sent());
        push.receive(new Push.PushBlock<String>() {
            @Override
            public void yield(String v) {
                throw new Rollback();
            }
        });
        assertFalse(push.sent());
    }
}
