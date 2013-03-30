package juggler;

import junit.framework.TestCase;

import static juggler.Juggler.go;

public class NotifierTest extends TestCase {

    Notifier<Integer> notifier;

    protected void setUp() throws Exception {
        notifier = new Notifier<Integer>();
    }

    public void testNotifyWithPayload() {
        notifier.notify(1);
        assertTrue(notifier.getPayload() == 1);
    }

    /**
     * It should acknowledge notification.
     */
    public void testIsNotified() {
        assertFalse(notifier.isNotified());
        notifier.notify(1);
        assertTrue(notifier.isNotified());
    }

    /**
     * It should only notify once.
     */
    public void testNotifyOnce() {
        notifier.notify(1);
        notifier.notify(2);
        assertTrue(notifier.getPayload() == 1);
    }

    /**
     * It should return null when notified for the first time.
     */
    public void testNotifyReturn() {
        assertNull(notifier.notify(1));
    }

    /**
     * It should return an error when notified more than once.
     */
    public void testReturnError() {
        notifier.notify(1);
        Error err = notifier.notify(2);
        assertEquals("already notified", err.toString());
    }

    /**
     * It should allow waiting on a notification and should signal when it is notified.
     */
    public void testWaiting() {
        final Channel<Integer> ack = new Channel<Integer>();
        go(new Runnable() {
            @Override
            public void run() {
                notifier.await();
                ack.send(notifier.getPayload());
            }
        });
        try {
            Thread.sleep(100); // make sure the notifier in the goroutine is waiting
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        notifier.notify(1);
        int payload = ack.receive();
        assertEquals(1, payload);
    }
}
