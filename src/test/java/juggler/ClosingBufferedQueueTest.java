package juggler;

import juggler.errors.ChannelClosedError;

public class ClosingBufferedQueueTest extends BufferedQueueTest {

    Push<String> push1, push2, push3;

    protected void setUp() throws Exception {
        super.setUp();
        push1 = queue.push("1", /*deferred =*/ true);
        push2 = queue.push("1", /*deferred =*/ true);
        push3 = queue.push("1", /*deferred =*/ true);
    }

    /**
     * It should go from open to closed.
     */
    public void testClose() {
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
        assertTrue(push1.sent());
        assertTrue(push2.sent());
        assertFalse(push3.sent());
        assertFalse(push3.isClosed());

        queue.close();

        assertTrue(push3.isClosed());
    }

    /**
     * It should clear all waiting operations.
     */
    public void testClearOperations() {
        assertEquals(1, queue.operations.size());
        assertEquals(1, queue.pushes.size());
        queue.close();
        assertEquals(0, queue.operations.size());
        assertEquals(0, queue.pushes.size());
    }

    /**
     * It should clear all elements at rest.
     */
    public void testClear() {
        assertEquals(2, queue.queue.size());
        queue.close();
        assertEquals(0, queue.queue.size());
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
        } catch (ChannelClosedError e) {
        }
    }
}
