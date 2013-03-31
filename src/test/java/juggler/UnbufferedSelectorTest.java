package juggler;

import static juggler.Selector.select;
import static juggler.Juggler.go;

import juggler.errors.BlockMissingError;
import juggler.errors.ChannelClosedError;
import junit.framework.TestCase;

import java.util.LinkedList;

public class UnbufferedSelectorTest extends TestCase {

    Channel<Integer> c;

    protected void setUp() throws Exception {
        c = new Channel<Integer>();
    }

    protected void tearDown() throws Exception {
        if (c.isOpen()) {
            c.close();
        }
    }

    /**
     * It should evaluate select statements top to bottom.
     */
    public void testOrder() {
        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.sendCase(c, 1, null);
                s.receiveCase(c, null);
                s.defaultCase(null);
                UnbufferedSelectorTest.assertEquals(3, s.cases.size());
            }
        });
    }

    /**
     * It should not raise an error when a block is missing on receive.
     */
    public void testMissingReceive() {
        try {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.receiveCase(c, null);
                    s.defaultCase(null);
                }
            });
        } catch (BlockMissingError e) {
            fail();
        }
    }

    /**
     * It should not raise an error when a block is missing on send.
     */
    public void testMissingSend() {
        try {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.sendCase(c, 1, null);
                    s.defaultCase(null);
                }
            });
        } catch (BlockMissingError e) {
            fail();
        }
    }

    /**
     * It should scan all cases to identify available actions and execute first available one.
     */
    public void testScan() {
        final LinkedList<Integer> r = new LinkedList<Integer>();
        go(new Runnable() {
            @Override
            public void run() {
                c.send(1);
            }
        });

        try {
            Thread.sleep(10); // make sure the goroutine executes, brittle
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.sendCase(c, 1, new Selector.SendBlock() {
                    @Override
                    public void yield() {
                        r.add(1);
                    }
                });
                s.receiveCase(c, new Selector.ReceiveBlock<Integer>() {
                    @Override
                    public void yield(Integer value) {
                        r.add(2);
                    }
                });
                s.receiveCase(c, new Selector.ReceiveBlock<Integer>() {
                    @Override
                    public void yield(Integer value) {
                        r.add(3);
                    }
                });
            }
        });

        assertEquals(1, r.size());
        assertTrue(r.getFirst() == 2);
    }

    /**
     * It should evaluate default case immediately if no other cases match.
     */
    public void testDefaultCase() {
        final LinkedList<Integer> r = new LinkedList<Integer>();

        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.sendCase(c, 1, new Selector.SendBlock() {
                    @Override
                    public void yield() {
                        r.add(1);
                    }
                });
                s.defaultCase(new Selector.ReceiveBlock<Boolean>() {
                    @Override
                    public void yield(Boolean value) {
                        r.add(-99);
                    }
                });
            }
        });

        assertTrue(r.size() == 1);
        assertTrue(r.getFirst() == -99);
    }

    /**
     * It should raise an error if the channel is closed out from under it.
     */
    public void testChannelClose() {
        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                c.close();
            }
        });

        try {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.sendCase(c, 1);
                    s.receiveCase(c);
                }
            });
            fail();
        } catch (ChannelClosedError e) {
        }
    }

    /* Select immediately available channel */

    /**
     * It should select read channel.
     */
    public void testRead() {
        final Channel<Integer> c = new Channel<Integer>();
        go(new Runnable() {
            @Override
            public void run() {
                c.send(1);
            }
        });

        try {
            Thread.sleep(10); // make sure the goroutine executes, brittle
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        final LinkedList<String> r = new LinkedList<String>();
        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.sendCase(c, 1, new Selector.SendBlock() {
                    @Override
                    public void yield() {
                        r.add("send");
                    }
                });
                s.receiveCase(c, new Selector.ReceiveBlock<Integer>() {
                    @Override
                    public void yield(Integer value) {
                        r.add("receive");
                    }
                });
                s.defaultCase(new Selector.ReceiveBlock<Boolean>() {
                    @Override
                    public void yield(Boolean value) {
                       r.add("empty");
                    }
                });
            }
        });

        assertEquals(1, r.size());
        assertEquals("receive", r.getFirst());
        c.close();
    }

    /**
     * It should select write channel.
     */
    public void testWrite() {
        final Channel<Integer> c = new Channel<Integer>();

        go(new Runnable() {
            @Override
            public void run() {
                c.receive();
            }
        });

        try {
            Thread.sleep(10); // make sure the goroutine executes, brittle
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        final LinkedList<String> r = new LinkedList<String>();
        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.sendCase(c, 1, new Selector.SendBlock() {
                    @Override
                    public void yield() {
                        r.add("send");
                    }
                });
                s.receiveCase(c, new Selector.ReceiveBlock<Integer>() {
                    @Override
                    public void yield(Integer value) {
                        r.add("receive");
                    }
                });
                s.defaultCase(new Selector.ReceiveBlock<Boolean>() {
                    @Override
                    public void yield(Boolean value) {
                        r.add("empty");
                    }
                });
            }
        });

        assertEquals(1, r.size());
        assertEquals("send", r.getFirst());
        c.close();
    }

    /* Select busy channel */

    /**
     * It should select busy read channel.
     */
    public void testBusyRead() {
        final Channel<Integer> c = new Channel<Integer>();
        final LinkedList<Integer> r = new LinkedList<Integer>();

        // brittle.. counting on select to execute within 0.5s
        long now = System.currentTimeMillis();
        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                c.send(1);
            }
        });

        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.receiveCase(c, new Selector.ReceiveBlock<Integer>() {
                    @Override
                    public void yield(Integer value) {
                        r.add(value);
                    }
                });
            }
        });

        assertEquals(1, r.size());
        long t = System.currentTimeMillis() - now;
        assertTrue(t > 100);
        assertTrue(t < 300);
        c.close();
    }

    /**
     * It should select busy write channel.
     */
    public void testBusyWrite() {
        final Channel<Integer> c = new Channel<Integer>();

        // brittle.. counting on select to execute within 0.5s
        long now = System.currentTimeMillis();
        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                assertTrue(c.receive() == 2);
            }
        });

        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.sendCase(c, 2);
            }
        });

        long t = System.currentTimeMillis() - now;
        assertTrue(t > 100);
        assertTrue(t < 300);
        c.close();
    }

    /**
     * It should select first available channel.
     */
    public void testFirst() {
        // create a write channel and a read channel
        final Channel<Integer> cw  = new Channel<Integer>();
        final Channel<Integer> cr  = new Channel<Integer>();
        final Channel<Boolean> ack = new Channel<Boolean>();

        final LinkedList<Integer> res = new LinkedList<Integer>();

        long now = System.currentTimeMillis();

        // read channel: wait for 0.3s before pushing a message into it
        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                cr.send(2);
            }
        });
        // write channel: wait for 0.1s before consuming the message
        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                res.add(cw.receive());
                ack.send(true);
            }
        });

        // wait until one of the channels become available
        // cw should fire first and push '3'
        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.receiveCase(cr, new Selector.ReceiveBlock<Integer>() {
                    @Override
                    public void yield(Integer value) {
                        res.add(value);
                    }
                });
                s.sendCase(cw, 3);
            }
        });

        ack.receive();

        assertEquals(1, res.size());
        assertTrue(res.getFirst().equals(3));

        // 0.3s goroutine should eventually fire
        assertTrue(cr.receive().equals(2));

        long t = System.currentTimeMillis() - now;
        assertTrue(t > 250);
        assertTrue(t < 350);
        cw.close();
        cr.close();
    }
}
