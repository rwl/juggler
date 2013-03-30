package juggler;

import juggler.errors.ChannelClosedError;

public class ClosingUnbufferedTest extends UnbufferedTest {

    Push<String> push1, push2;

    protected void setUp() throws Exception {
        push1 = queue.deferredPush("1");
        push2 = queue.deferredPush("1");
    }

    /**
     * It should go from open to closed.
     */
    public void testClosed() {
        assertFalse(queue.isClosed());
        assertTrue(queue.isOpen());
        queue.close();
        assertTrue(queue.isClosed());
        assertFalse(queue.isOpen());
    }

    /**
     * It should close all the waiting operations.
     */
    public void testCloseOperations() {
        assertFalse(push1.sent());
        assertFalse(push1.isClosed());
        assertFalse(push2.sent());
        assertFalse(push2.isClosed());

        queue.close();

        assertTrue(push1.isClosed());
        assertTrue(push2.isClosed());
    }

    /**
     * It should clear all waiting operations.
     */
    public void testClearOperations() {
        assertEquals(2, queue.operations.size());
        assertEquals(2, queue.pushes.size());
        queue.close();
        assertEquals(0, queue.operations.size());
        assertEquals(0, queue.pushes.size());
    }

    /**
     * It should raise an error when being acted upon afterwards.
     */
    public void testError() {
        queue.close();
        try {
            queue.close();
            fail();
        } catch (ChannelClosedError e) {
        }
        try {
            queue.push("1");
            fail();
        } catch (ChannelClosedError e) {
        }
        try {
            queue.pop();
            fail();
        } catch (ChannelClosedError e) {
        }
    }
}
