package juggler;

import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static juggler.Juggler.go;

public class OnceTest extends TestCase {

    Once<Integer> once = new Once<Integer>();

    /**
     * It should execute the block passed to it.
     */
    public void testPerform() {
        final LinkedList<Integer> r = new LinkedList<Integer>();

        once.perform(new Once.Performable<Integer>() {
            @Override
            public Integer perform() {
                r.add(1);
                return null;
            }
        });

        assertEquals(1, r.size());
        assertTrue(r.getFirst() == 1);
    }

    /**
     * It should only execute the first block passed to it.
     */
    public void testPerformTwice() {
        final LinkedList<Integer> r = new LinkedList<Integer>();

        once.perform(new Once.Performable<Integer>() {
            @Override
            public Integer perform() {
                r.add(1);
                return null;
            }
        });
        once.perform(new Once.Performable<Integer>() {
            @Override
            public Integer perform() {
                r.add(1);
                return null;
            }
        });

        assertEquals(1, r.size());
        assertTrue(r.getFirst() == 1);
    }

    /**
     * It should return the value returned from the block.
     */
    public void testPerformReturn() {
        Integer value = null;
        try {
            value = once.perform(new Once.Performable<Integer>() {
                @Override
                public Integer perform() {
                    return 1;
                }
            });
        } catch (Error error) {
            fail();
        }

        assertTrue(value == 1);
    }

    /**
     * It should return null for value and an error if it has already been used.
     */
    public void testPerformReturnError() {
        Integer value = null;
        try {
            value = once.perform(new Once.Performable<Integer>() {
                @Override
                public Integer perform() {
                    return 1;
                }
            });
        } catch (Error error) {
            fail();
        }
        assertTrue(value == 1);

        try {
            value = once.perform(new Once.Performable<Integer>() {
                @Override
                public Integer perform() {
                    return 2;
                }
            });
            fail();
        } catch (Error error) {
            assertEquals("already performed", error.getMessage());
        }
        assertNull(value);
    }

    // Using condition variables to maximize potential contention
    final Lock mutex = new ReentrantLock();
    final Condition condition = mutex.newCondition();

    /**
     * It should have minimal contention between threads when they contend for position.
     */
    public void testThreadContention() {
        final LinkedList<Integer> r = new LinkedList<Integer>();
        long s = System.currentTimeMillis();

        final Channel<Boolean> waiting_channel = new Channel<Boolean>(2);
        final Channel<Boolean> finished_channel = new Channel<Boolean>(2);

        go(new Runnable() {
            @Override
            public void run() {
                mutex.lock();
                try {
                    waiting_channel.send(true);
                    condition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    mutex.unlock();
                }
                once.perform(new Once.Performable<Integer>() {
                    @Override
                    public Integer perform() {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        r.add(1);
                        return null;
                    }
                });
                finished_channel.send(true);
            }
        });

        go(new Runnable() {
            @Override
            public void run() {
                mutex.lock();
                try {
                    waiting_channel.send(true);
                    condition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    mutex.unlock();
                }
                once.perform(new Once.Performable<Integer>() {
                    @Override
                    public Integer perform() {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        r.add(1);
                        return null;
                    }
                });
                finished_channel.send(true);
            }
        });

        // wait for both the goroutines to be waiting
        waiting_channel.receive();
        waiting_channel.receive();

        synchronized (mutex) {
            condition.signalAll();
        }

        // wait for the finished channel to be completed
        finished_channel.receive();
        finished_channel.receive();

        assertTrue(r.size() == 1);
        // Onlt the first sleep should be performed, so things should quickly
        long t = System.currentTimeMillis() - s;
        assertTrue(t > 100);
        assertTrue(t < 200);

        waiting_channel.close();
        finished_channel.close();
    }
}
