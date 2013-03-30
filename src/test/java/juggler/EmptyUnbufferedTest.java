package juggler;

import junit.framework.TestCase;

/**
 * Tests when there are no operations waiting.
 */
public class EmptyUnbufferedTest extends UnbufferedTest {

    public void testPoppable() {
        assertFalse(queue.poppable());
    }

    public void testPushable() {
        assertFalse(queue.pushable());
    }

    /**
     * It should queue pushes.
     */
    public void testPush() {
        assertEquals(0, queue.operations.size());
        Push<String> push = queue.deferredPush("1");
        assertFalse(push.sent());
        assertEquals(1, queue.operations.size());
    }

    /**
     * It should queue pops.
     */
    public void testPop() {
        assertEquals(0, queue.operations.size());
        Pop<String> pop = queue.deferredPop();
        assertFalse(pop.received());
        assertEquals(1, queue.operations.size());
    }
}
