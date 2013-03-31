package juggler;

import static juggler.Selector.select;
import static juggler.Juggler.go;

import juggler.errors.BlockMissingError;
import juggler.errors.ChannelClosedError;
import junit.framework.TestCase;

import java.util.LinkedList;

public class BufferedSelectorTest extends TestCase {

    Channel<Integer> c;

    protected void setUp() throws Exception {
        c = new Channel<Integer>(1);
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
                s.sendCase(c, 1);
                s.receiveCase(c);
                assertEquals(2, s.cases.size());
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
                    s.receiveCase(c);
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
                    s.sendCase(c, 1);
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
        c.send(1);

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
        assertTrue(r.getFirst().equals(2));
    }

    /**
     * It should evaluate default case immediately if no other cases match.
     */
    public void testDefault() {
        final LinkedList<Integer> r = new LinkedList<Integer>();

        c.send(1);

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

        assertEquals(1, r.size());
        assertTrue(r.getFirst().equals(-99));
    }

    /**
     * It should raise an error if the channel is closed out from under it.
     */
    public void testClose() {
        c.send(1);

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
                    s.sendCase(c, 2);
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
        final Channel<Integer> c = new Channel<Integer>(1);
        c.send(1);

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
        final Channel<Integer> c = new Channel<Integer>(1);

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
        final Channel<Integer> c = new Channel<Integer>(1);
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
        final Channel<Integer> c = new Channel<Integer>(1);
        c.send(1);

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
                c.receive();
            }
        });

        select(new Selector.SelectorBlock() {
            @Override
            public void yield(Selector s) {
                s.sendCase(c, 2);
            }
        });

        assertTrue(c.receive().equals(2));
        long t = System.currentTimeMillis() - now;
        assertTrue(t > 100);
        assertTrue(t < 300);
        c.close();
    }

    /**
     * It should select first available channel.
     */
    public void testFirst() {
        // create a "full" write channel, and "empty" read channel
        final Channel<Integer> cw  = new Channel<Integer>(1);
        final Channel<Integer> cr  = new Channel<Integer>(1);
        final Channel<Boolean> ack = new Channel<Boolean>();

        cw.send(1);

        final LinkedList<Integer> res = new LinkedList<Integer>();

        long now = System.currentTimeMillis();
        // empty read channel: wait for 0.5s before pushing a message into it
        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                cr.send(2);
            }
        });
        // full write channel: wait for 0.2s before consuming the message
        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
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

        // 0.8s goroutine should have consumed the message first
        assertEquals(1, res.size());
        assertTrue(res.getFirst().equals(1));

        // send case should have fired, and we should have a message
        assertTrue(cw.receive().equals(3));

        long t = System.currentTimeMillis() - now;
        assertTrue(t > 100);
        assertTrue(t < 300);
        cw.close();
        cr.close();
    }
}
