package juggler;

import junit.framework.TestCase;

public class NotifierPushTest extends TestCase {

    Notifier<String> notifier;
    Push<String> push;

    protected void setUp() throws Exception {
        notifier = new Notifier<String>();
        push = new Push<String>("1", notifier);
    }

    /**
     * It should notify when being sent.
     */
    public void testSendNotify() {
        assertFalse(notifier.isNotified());
        push.receive(new Push.PushBlock<String>() {
            @Override
            public void yield(String v) {
            }
        });
        assertTrue(notifier.isNotified());
    }

    /**
     * It should notify when being closed.
     */
    public void testCloseNotify() {
        assertFalse(notifier.isNotified());
        push.close();
        assertTrue(notifier.isNotified());
    }
}
