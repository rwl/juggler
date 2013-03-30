package juggler;

import junit.framework.TestCase;

public class NotifierPopTest extends TestCase {

    Notifier<Long> notifier;
    Pop<Long> pop;

    protected void setUp() throws Exception {
        notifier = new Notifier<Long>();
        pop = new Pop<Long>(notifier);
    }

    /**
     * It should notify when being sent.
     */
    public void testSendNotify() {
        assertFalse(notifier.isNotified());
        pop.send(new Pop.PopBlock<Long>() {
            @Override
            public Long yield() {
                return Marshal.dump(1);
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
