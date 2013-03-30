package juggler;

public class RemoveOperationsBufferedQueueTest extends BufferedQueueTest {

    Push<String>[] pushes;

    protected void setUp() throws Exception {
        super.setUp();
        pushes = new Push[8];
        for (int i = 1; i <= 8; i++) {
            pushes[i] = queue.push(String.valueOf(i), /*deferred =*/ true);
        }
    }

    /**
     * It should remove the operations.
     */
    public void testRemoveOperations() {
        queue.remove_operations(pushes[5], pushes[6]); // values "6" and "7"
        while (queue.poppable()) {
            String i = queue.pop();
            assertNotNull(i);
            assertTrue(i != "6");
            assertTrue(i != "7");
        }
    }
}
