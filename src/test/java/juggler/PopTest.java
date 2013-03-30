package juggler;

import juggler.errors.Rollback;
import junit.framework.TestCase;

import static juggler.Juggler.go;

public class PopTest extends TestCase {

    Pop<Long> pop;
    Channel<Long> ack;

    protected void setUp() throws Exception {
        pop = new Pop<Long>();
        ack = new Channel<Long>();
    }

    /**
     * It should close.
     */
    public void testClose() {
        assertFalse(pop.isClosed());
        pop.close();
        assertTrue(pop.isClosed());
    }

    /**
     * It should run multiple times.
     */
    public void testMultiRun() {
        pop.send(new Pop.PopBlock<Long>() {
            @Override
            public Long yield() {
                return Marshal.dump(1);
            }
        });
        assertTrue(pop.isReceived());
        pop.send(new Pop.PopBlock<Long>() {
            @Override
            public Long yield() {
                return Marshal.dump(2);
            }
        });
        assertTrue(pop.getObject() == 2);
    }

    /**
     * It should continue when received.
     */
    public void testContinueOnReceive() {
        go(new Runnable() {
            @Override
            public void run() {
                pop.await();
                ack.send(System.currentTimeMillis());
            }
        });
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pop.send(new Pop.PopBlock<Long>() {
            @Override
            public Long yield() {
                return Marshal.dump(1);
            }
        });

        long s = ack.receive();

        long t = System.currentTimeMillis() - s;
        assertTrue(t > -10);
        assertTrue(t < 10);
    }

    /**
     * It should continue when closed
     */
    public void testContinueOnClose() {
        go(new Runnable() {
            @Override
            public void run() {
                pop.await();
                ack.send(System.currentTimeMillis());
            }
        });
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pop.close();

        long s = ack.receive();
        long t = System.currentTimeMillis() - s;
        assertTrue(t > -10);
        assertTrue(t < 10);
    }

    /**
     * It should be able to be gracefully rolled back.
     */
    public void testRollback() {
        assertFalse(pop.isReceived());
        pop.send(new Pop.PopBlock<Long>() {
            @Override
            public Long yield() {
                throw new Rollback();
            }
        });
        assertFalse(pop.isReceived());
    }
}
