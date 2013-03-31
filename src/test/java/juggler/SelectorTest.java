package juggler;

import static juggler.Selector.select;

import juggler.errors.BlockMissingError;
import junit.framework.TestCase;

import java.util.LinkedList;

public class SelectorTest extends TestCase {

    /**
     * It should return immediately on empty select block.
     */
    public void testEmptyBlock() {
        long s = System.currentTimeMillis();
        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector selector) {
            }
        });

        long t = System.currentTimeMillis() - s;
        assertTrue(t > -5);
        assertTrue(t < 5);
    }

    /**
     * It should timeout select statement.
     */
    public void testTimeout() {
        final LinkedList<String> r = new LinkedList<String>();
        long now = System.currentTimeMillis();
        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.timeout(100, new Selector.SelectorBlock() {
                    @Override
                    public void yield(Selector s) {
                        r.add("timeout");
                    }
                });
            }
        });

        assertEquals("timeout", r.getFirst());
        long t = System.currentTimeMillis() - now;
        assertTrue(t > 95);
        assertTrue(t > 105);
    }

    /**
     * It should not raise an error when a block is missing on default.
     */
    public void testMissingDefault() {
        try {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.defaultCase(null);
                }
            });
        } catch (BlockMissingError e) {
            fail();
        }
    }

    /**
     * It should not raise an error when a block is missing on timeout.
     */
    public void testMissingTimeout() {
        try {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.timeout(1, null);
                }
            });
        } catch (BlockMissingError e) {
            fail();
        }
    }
}
