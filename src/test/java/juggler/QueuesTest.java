package juggler;

import junit.framework.TestCase;

public class QueuesTest extends TestCase {

    protected void tearDown() throws Exception {
        Queues.clear();
    }

    public void testRegister() {
        Queues.<String>register("foo", 10);
        assertTrue(Queues.<String>get("foo") instanceof Buffered);
        assertEquals(10, ((Buffered<String>) Queues.<String>get("foo")).max());
    }

    public void testDelete() {
        Queues.<String>register("foo", 10);
        Queues.delete("foo");
        assertNull(Queues.get("foo"));
    }

    public void testClear() {
        Queues.<String>register("foo", 10);
        Queues.<String>register("bar", 10);
        Queues.clear();
        assertNull(Queues.<String>get("foo"));
        assertNull(Queues.<String>get("bar"));
    }
}
