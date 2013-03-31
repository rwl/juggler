package juggler;

import juggler.errors.Rollback;
import junit.framework.TestCase;
import org.apache.commons.lang.SerializationUtils;

public class BlockingOncePopTest extends TestCase {

    BlockingOnce blocking_once;
    Pop<Long> pop;

    protected void setUp() throws Exception {
        blocking_once = new BlockingOnce();
        pop = new Pop<Long>(blocking_once);
    }

    /**
     * It should only send only once.
     */
    public void testSendOnce() {
        assertFalse(blocking_once.performed());
        pop.send(new Pop.PopBlock() {
            @Override
            public byte[] yield() {
                return SerializationUtils.serialize(1);
            }
        });
        assertTrue(pop.isReceived());
        assertTrue(blocking_once.performed());

        pop.send(new Pop.PopBlock() {
            @Override
            public byte[] yield() {
                return SerializationUtils.serialize(2);
            }
        });
        assertTrue(pop.getObject() == 1);

        try {
            pop.send(new Pop.PopBlock() {
                @Override
                public byte[] yield() {
                    throw new Error("an error");
                }
            });
        } catch (Error e) {
            fail();
        }
    }

    /**
     * It should be able to be gracefully rolled back.
     */
    public void testRollback() {
        assertFalse(blocking_once.performed());
        assertFalse(pop.isReceived());
        pop.send(new Pop.PopBlock() {
            @Override
            public byte[] yield() {
                throw new Rollback();
            }
        });
        assertFalse(blocking_once.performed());
        assertFalse(pop.isReceived());
    }
}
