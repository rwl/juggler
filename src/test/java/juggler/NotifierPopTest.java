package juggler;

import junit.framework.TestCase;
import org.apache.commons.lang.SerializationUtils;

public class NotifierPopTest extends TestCase {

    Notifier<Pop<Long>> notifier;
    Pop<Long> pop;

    protected void setUp() throws Exception {
        notifier = new Notifier<Pop<Long>>();
        pop = new Pop<Long>(notifier);
    }

    /**
     * It should notify when being sent.
     */
    public void testSendNotify() {
        assertFalse(notifier.isNotified());
        pop.send(new Pop.PopBlock() {
            @Override
            public byte[] yield() {
                return SerializationUtils.serialize(1);
            }
        });
        assertTrue(notifier.isNotified());
    }

    /**
     * It should notify when being closed.
     */
    public void testCloseNotify() {
        assertFalse(notifier.isNotified());
        pop.close();
        assertTrue(notifier.isNotified());
    }
}
