package juggler;

import junit.framework.TestCase;

public abstract class BufferedQueueTest extends TestCase {

    protected Buffered<String> queue;

    protected void setUp() throws Exception {
        queue = new Buffered<String>(2);
    }
}
