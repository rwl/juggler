package juggler;

/**
 * Tests when there is a push waiting.
 */
public class PushUnbufferedTest extends UnbufferedTest {

    Push<String> push;

    protected void setUp() throws Exception {
        super.setUp();
        push = queue.deferredPush("1");
    }

    public void testPoppable() {
        assertTrue(queue.poppable());
    }

    public void testPushable() {
        assertFalse(queue.pushable());
    }

    public void testPush() {
        assertEquals(1, queue.operations.size());
        push = queue.deferredPush("1");
        assertFalse(push.sent());
        assertEquals(2, queue.operations.size());
    }

    /**
     * It should execute a pop and the waiting push immediately.
     */
    public void testPop() {
        Pop<String> pop = queue.deferredPop();
        assertTrue(push.sent());
        assertTrue(pop.received());
        assertEquals("1", pop.getObject());
    }
}
