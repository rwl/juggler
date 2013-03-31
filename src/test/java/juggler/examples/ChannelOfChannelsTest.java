package juggler.examples;

import static juggler.Juggler.go;

import juggler.Channel;
import juggler.errors.ReceiveError;
import junit.framework.TestCase;

public class ChannelOfChannelsTest extends TestCase {

    static class Request {
        public int args;
        public Channel<Integer> resultChan;

        Request(int args, Channel<Integer> resultChan) {
            this.args = args;
            this.resultChan = resultChan;
        }
    }

    static class Server implements Runnable {
        Channel<Request> reqs;

        Server(Channel<Request> reqs) {
            this.reqs = reqs;
        }

        @Override
        public void run() {
            for (int n = 0; n < 2; n++) {
                Request res = new Request(n, new Channel<Integer>());

                reqs.send(res);
                assertTrue(res.resultChan.receive() == n + 1);
                res.resultChan.close();
            }
        }
    }

    static class Worker implements Runnable {
        Channel<Request> reqs;

        Worker(Channel<Request> reqs) {
            this.reqs = reqs;
        }

        @Override
        public void run() {
            while (true) {
                Request req = null;
                try {
                    req = reqs.receive();
                } catch (ReceiveError e) {
                    break;
                }
                req.resultChan.send(req.args + 1);
            }
        }
    }

    /**
     * It should be able to pass channels as first class citizens.
     */
    public void testChannels() {
        Channel<Request> clientRequests = new Channel<Request>();

        Thread s = go(Server.class, clientRequests);
        Thread c = go(Worker.class, clientRequests);

        try {
            s.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clientRequests.close();
    }

    public void testMultipleWorkers() {
        Channel<Request> clientRequests = new Channel<Request>();

        // start multiple workers
        go(Worker.class, clientRequests);
        go(Worker.class, clientRequests);

        // start server
        Thread s = go(Server.class, clientRequests);

        try {
            s.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clientRequests.close();
    }
}
