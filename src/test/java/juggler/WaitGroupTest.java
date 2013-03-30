package juggler;

import static juggler.Juggler.go;

import juggler.errors.NegativeWaitGroupCountError;
import junit.framework.TestCase;

import java.sql.Time;

public class WaitGroupTest extends TestCase {

    WaitGroup wait_group;

    protected void setUp() {
        wait_group = new WaitGroup();
    }

    public void testAdd() {
        wait_group.add(1);
    }

    public void testAddNegative() {
        wait_group.add(2);
        wait_group.add(-1);
    }

    /**
     * It should decrement the cound when WaitGroup#done is called.
     */
    public void testDoneDecrement() {
        wait_group.add(1);
        assertEquals(1, wait_group.getCount());
        wait_group.done();
        assertEquals(0, wait_group.getCount());
    }

    /**
     * It should error when the count becomes negative via WaitGroup#add.
     */
    public void testNegativeCountOnAdd() {
        try {
            wait_group.add(-1);
            fail();
        } catch (NegativeWaitGroupCountError e) {
        }
    }

    /**
     * It should error when the count becomes negative via WaitGroup#done.
     */
    public void testNegativeCountOnDone() {
        try {
            wait_group.done();
            fail();
        } catch (NegativeWaitGroupCountError e) {
        }
    }

    /**
     * It should allow waiting on a wait_group and should signal when it is done.
     */
    public void testWaiting() {
        wait_group.add(1);

        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                wait_group.done();
            }
        });

        long t = System.currentTimeMillis();

        wait_group.await();

        long diff = System.currentTimeMillis() - t;
        assertTrue(diff > 190);
        assertTrue(diff < 210);
    }
}
