package juggler;

import junit.framework.TestCase;

public class ErrorTest extends TestCase {

    static final String MSG = "msg";

    Error error;

    protected void setUp() throws Exception {
        error = new Error(MSG);
    }

    public void testToString() {
        assertEquals(MSG, error.toString());
    }

    public void testMessage() {
        assertEquals(MSG, error.getMessage());
    }

    public void testMatch() {
        assertTrue(error.isMessage(MSG));
    }
}
