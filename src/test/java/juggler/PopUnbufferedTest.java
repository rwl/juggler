package juggler;

public class PopUnbufferedTest extends UnbufferedTest {

    Pop<String> pop;

    protected void setUp() throws Exception {
        super.setUp();
        pop = queue.deferredPop();
    }

    public void testPoppable() {
        assertFalse(queue.poppable());
    }

    public void testPushable() {
        assertTrue(queue.pushable());
    }

    /**
     * It should execute a push and the waiting pop immediately.
     */
    public void testPush() {
        Push<String> push = queue.deferredPush("1");
        assertTrue(pop.received());
        assertTrue(push.sent());
        assertEquals("1", pop.getObject());
    }

    /**
     * It should queue pops.
     */
    public void testPop() {
        assertEquals(1, queue.operations.size());
        pop = queue.deferredPop();
        assertFalse(pop.received());
        assertEquals(2, queue.operations.size());
    }
}
