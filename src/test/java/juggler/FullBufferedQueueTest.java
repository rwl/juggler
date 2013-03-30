package juggler;

public class FullBufferedQueueTest extends BufferedQueueTest {

    protected void setUp() throws Exception {
        super.setUp();
        queue.push("1");
        queue.push("1");
    }

    /**
     * It should hold any attempts to push to it.
     */
    public void testPush() {
        assertTrue(queue.operations.isEmpty());
        queue.push("1", /*deferred =*/true);
        assertFalse(queue.operations.isEmpty());
    }

    /**
     * It should be able to be popped from.
     */
    public void testPop() {
        assertEquals("1", queue.pop());
    }

    /**
     * It should not be pushable.
     */
    public void testPushable() {
        assertFalse(queue.pushable());
    }

    /**
     * It should be poppable.
     */
    public void testPoppable() {
        assertTrue(queue.poppable());
    }
}
