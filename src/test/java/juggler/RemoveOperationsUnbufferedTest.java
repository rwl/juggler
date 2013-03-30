package juggler;

public class RemoveOperationsUnbufferedTest extends UnbufferedTest {

    Push<String>[] pushes;

    protected void setUp() throws Exception {
        super.setUp();
        pushes = new Push[8];
        for (int i = 1; i <= 8; i++) {
            pushes[i] = queue.deferredPush(String.valueOf(i));
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
            assertFalse(i == "6");
            assertFalse(i == "7");
        }
    }
}
