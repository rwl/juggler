package juggler;

/**
 * Elements in the queue and still space left.
 */
public class HalfFullBufferedQueueTest extends BufferedQueueTest {

    protected void setUp() throws Exception {
        super.setUp();
        queue.push("1");
    }

    /**
     * It should be able to be pushed to
     */
    public void testPush() {
        queue.push("1");
    }

    public void testPushSize() {
        assertEquals(1, queue.size());
        queue.push("1");
        assertEquals(2, queue.size());
    }

    /**
     * It should be able to be popped from.
     */
    public void testPop() {
        assertEquals("1", queue.pop());
    }

    /**
     * It should decrease in size when popped from.
     */
    public void testPopSize() {
        assertEquals(1, queue.size());
        queue.pop();
        assertEquals(0, queue.size());
    }

    /**
     * It should be pushable.
     */
    public void testPushable() {
        assertTrue(queue.pushable());
    }

    /**
     * It should be poppable.
     */
    public void testPoppable() {
        assertTrue(queue.poppable());
    }
}
