package juggler;

import junit.framework.TestCase;

public class UnbufferedTest extends TestCase {

    Unbuffered<String> queue;

    protected void setUp() throws Exception {
        queue = new Unbuffered<String>();
    }
}
