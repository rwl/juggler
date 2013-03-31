package juggler;

import static juggler.Juggler.go;
import juggler.Channel;
import juggler.Direction;
import juggler.errors.ChannelClosedError;
import juggler.errors.InvalidDirectionError;
import juggler.errors.ReceiveError;
import junit.framework.TestCase;
import org.apache.commons.lang.SerializationUtils;
import sun.management.Agent;

import java.util.ArrayList;
import java.util.List;


public class ChannelTest extends TestCase {

    Channel<String> channel;

    protected void setUp() throws Exception {
        channel = new Channel<String>();
    }

    protected void tearDown() throws Exception {
        if (channel.isOpen()) {
            channel.close();
        }
    }

    public void testSendDirection() {
        Channel<String> c = new Channel<String>(3, Direction.SEND);

        try {
            c.send("hello");
            c.push("hello");
            c.send("hello");
        } catch (Exception e) {
            fail();
        }

        assertTrue(c.getDirection() == Direction.SEND);

        try {
            c.pop();
            fail();
        } catch (InvalidDirectionError e) {
        }
        try {
            c.receive();
            fail();
        } catch (InvalidDirectionError e) {
        }
        c.close();
    }

    public void testReceiveDirection() {
        Channel<String> c = new Channel<String>(Direction.RECEIVE);

        try {
            c.send("hello");
            fail();
        } catch (InvalidDirectionError e) {
        }
        try {
            c.push("hello");
            fail();
        } catch (InvalidDirectionError e) {
        }
        try {
            c.send("hello");
            fail();
        } catch (InvalidDirectionError e) {
        }

        assertTrue(c.getDirection() == Direction.RECEIVE);

        // timeout blocking receive calls
        boolean timed_out = false;
//        select! do |s|
//                s.case(c, :receive)
//        s.timeout(0.1){ timed_out = true }
//        end
//        assertTrue(timed_out);
        c.close();
    }

    /**
     * should default to bi-directional communication
     */
    public void testBidirectionalChannel() {
        Channel<String> c = new Channel<String>(1);
        try {
            c.send("hello");
            c.receive();
        } catch (Exception e) {
            fail();
        }

        assertTrue(c.getDirection() == Direction.BIDIRECTIONAL);
    }

    /**
     * should be able to be dup'd as a uni-directional channel
     */
    public void testDuplication() {
        Channel<String> c = new Channel<String>(1);

        Channel<String> send_only = c.as_send_only();
        assertTrue(send_only.getDirection() == Direction.SEND);

        Channel<String> receive_only = c.as_receive_only();
        assertTrue(receive_only.getDirection() == Direction.RECEIVE);

        send_only.send("nifty");
        assertEquals("nifty", receive_only.receive());
    }

    /**
     * should not raise an error the first time close is called
     */
    public void testCloseOnce() {
        try {
            channel.close();
        } catch (Exception e) {
            fail();
        }
        assertTrue(channel.isClosed());
    }

    /**
     * should raise an error the second time it is called
     */
    public void testClosedTwice() {
        channel.close();
        try {
            channel.close();
            fail();
        } catch (ChannelClosedError e) {
        }
    }

    public void testIsClosed() {
        assertFalse(channel.isClosed());
        channel.close();
        assertTrue(channel.isClosed());
    }

    /**
     * should return that a receive was a failure when a channel is closed while being read from
     */
    public void testReceiveError() {
        go(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                channel.close();
            }
        });
        try {
            channel.receive();
            fail();
        } catch (ReceiveError e) {
        }
    }

    /**
     * should raise an error when sending to a channel that has already been closed
     */
    public void testChannelClosedErrorOnSend() {
        channel.close();
        try {
            channel.send("a");
            fail();
        } catch (ChannelClosedError e) {
        }

    }

    /**
     * should raise an error when receiving from a channel that has already been closed
     */
    public void testChannelClosedErrorOnReceive() {
        channel.close();
        try {
            channel.receive();
            fail();
        } catch (ChannelClosedError e) {
        }
    }

    /**
     * should default to unbuffered
     */
    public void testUnbuffered() {
        long n = System.currentTimeMillis();

        go(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                channel.send("hello");
            }
        });
        assertEquals(channel.receive(), "hello");

        long diff = System.currentTimeMillis() - n;
        assertTrue(diff > 100);
        assertTrue(diff < 200);
    }

    public void testBuffered() {
        Channel<String> c = new Channel<String>(2);
        List<String> r = new ArrayList<String>();

        c.send("hello 1");
        c.send("hello 2");

//        select! do |s|
//                s.case(c, :send, "hello 3")
//            s.timeout(0.1)
//        end

        assertEquals("hello 1", c.receive());
        assertEquals("hello 2", c.receive());
//        select! do |s|
//                s.case(c, :receive){|v| r.push(v) }
//        s.timeout(0.1)
//        end

        c.close();
    }

    /**
     * should be a first class, serializable value
     */
    public void testSerializableChannelsOfChannels() {
        Channel<String> c = new Channel<String>(1);

        try {
            SerializationUtils.serialize(c);
        } catch (Exception e) {
            fail();
        }
        try {
            Object cc = SerializationUtils.deserialize(SerializationUtils.serialize(c));
            assertTrue(cc instanceof Channel);
        } catch (Exception e) {
            fail();
        }
        if (!c.isClosed()) {
            c.close();
        }
    }

    /**
     * should be able to pass as a value on a different channel
     */
    public void testChannelsOfChannels() {
        Channel<String> c = new Channel<String>(1);

        c.send("hello");

        Channel<String> cm = (Channel<String>) SerializationUtils.deserialize(SerializationUtils.serialize(c));
        assertEquals("hello", cm.receive());

        if (!c.isClosed()) {
            c.close();
        }
    }
}
