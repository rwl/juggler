package juggler;

public class EmptyBasicBufferedQueueTest extends BufferedQueueTest {

    /**
     * It should hold any attempts to pop from it.
     */
    public void testPop() {
        assertTrue(queue.operations.isEmpty());
        queue.deferredPop();
        assertFalse(queue.operations.isEmpty());
    }

    /**
     * It should be able to be pushed to.
     */
    public void testPush() {
        queue.push("1");
    }

    /**
     * It should increase in size when pushed to.
     */
    public void testPushSize() {
        assertEquals(0, queue.size());
        queue.push("1");
        assertEquals(1, queue.size());
    }

    /**
     * It should be pushable when empty.
     */
    public void testPushable() {
        assertTrue(queue.pushable());
    }

    /**
     * It should not be poppable.
     */
    public void testPoppable() {
        assertFalse(queue.poppable());
    }
}
