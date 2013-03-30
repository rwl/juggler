package juggler;

import juggler.errors.Rollback;
import junit.framework.TestCase;


public class BlockingOncePushTest extends TestCase {

    BlockingOnce blocking_once;
    Push<String> push;

    protected void setUp() throws Exception {
        blocking_once = new BlockingOnce();
        push = new Push<String>("1", blocking_once);
    }

    /**
     * It should only send only once.
     */
    public void testSendOnce() {
        final int[] i = new int[] {0};

        assertFalse(blocking_once.performed());
        push.receive(new Push.PushBlock<String>() {
            @Override
            public void yield(String v) {
                i[0] = i[0] + 1;
            }
        });
        assertTrue(push.sent());
        assertTrue(blocking_once.performed());

        push.receive(new Push.PushBlock<String>() {
            @Override
            public void yield(String obj) {
                i[0] = i[0] + 1;
            }
        });
        assertEquals(1, i[0]);

        try {
            push.receive(new Push.PushBlock<String>() {
                @Override
                public void yield(String obj) {
                    throw new Error("an error");
                }
            });
        } catch (Error error) {
            fail();
        }
    }

    /**
     * It should be able to be gracefully rolled back.
     */
    public void testRollback() {
        assertFalse(blocking_once.performed());
        assertFalse(push.sent());
        push.receive(new Push.PushBlock<String>() {
            @Override
            public void yield(String v) {
                throw new Rollback();
            }
        });
        assertFalse(blocking_once.performed());
        assertFalse(push.sent());
    }
}
