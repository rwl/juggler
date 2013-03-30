package juggler;

import juggler.errors.InvalidQueueSizeError;
import junit.framework.TestCase;

public class BasicBufferedQueueTest extends BufferedQueueTest {

    public void testBuffered() {
        assertTrue(queue.isBuffered());
    }

    public void testUnbuffered() {
        assertFalse(queue.isUnbuffered());
    }

    /**
     * It should raise an error if the queue size is <= 0.
     */
    public void testInvalidQueueSize() {
        try {
            new Buffered<String>(0);
            fail();
        } catch (InvalidQueueSizeError e) {
        }
        try {
            new Buffered<String>(-1);
            fail();
        } catch (InvalidQueueSizeError e) {
        }
    }

    /**
     * It should enqueue and dequeue in order.
     */
    public void testOrder() {
        for (int i = 0; i < 20; i++) {
            queue.deferredPush(String.valueOf(i));
        }

        int previous = -1;

        for (int i = 0; i < 20; i++) {
            int o = Integer.valueOf(queue.pop());
            assertTrue(o > previous);
            previous = o;
        }
    }
}
