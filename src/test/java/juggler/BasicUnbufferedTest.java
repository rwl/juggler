package juggler;

public class BasicUnbufferedTest extends UnbufferedTest {

    /**
     * It should not be buffered.
     */
    public void testBuffered() {
        assertFalse(queue.isBuffered());
    }

    /**
     * It should be unbuffered.
     */
    public void testUnbuffered() {
        assertTrue(queue.isUnbuffered());
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
