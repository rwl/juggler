package juggler.examples;

import static juggler.Juggler.go;
import juggler.Channel;

public class Workers {

	/*
	 * First, we declare a new class, which will encapsulate several arguments,
	 * and then declare a clientRequests channel, which will carry our Request
	 * class.
	 */
	static class Request {
		public int args;
		public Channel<String> resultChan;

		Request(int args, Channel<String> resultChan) {
			this.args = args;
			this.resultChan = resultChan;
		}
	}

	/*
	 * Now, we create a new worker class, which takes in a “reqs” object, calls
	 * receive on it (hint, req’s is a Channel!), sleeps for a bit, and then
	 * sends back a timestamped result.
	 */
	static class Worker implements Runnable {
		Channel<Request> reqs;

		Worker(Channel<Request> reqs) {
			this.reqs = reqs;
		}

		@Override
		public void run() {
			while (true) {
				Request req = reqs.receive()/* [0] */;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
				req.resultChan.send(System.currentTimeMillis() + " : "
						+ (req.args + 1));
			}
		}
	}

	public static void main(String[] args) {
		// Set the size of our channel to two – we’ll see why in a second.
		Channel<Request> clientRequests = new Channel<Request>(2);

		// Start two workers
		go(Worker.class, clientRequests);
		go(Worker.class, clientRequests);

		// The rest is simple, we create two distinct requests, which carry a
		// number and a reply channel, and pass them to our clientRequests pipe,
		// on which our workers are waiting. Once dispatched, we simply call
		// receive and wait for the results!

		Request req1 = new Request(1, new Channel<String>());
		Request req2 = new Request(2, new Channel<String>());

		clientRequests.send(req1);
		clientRequests.send(req2);

		// retrieve results
		System.out.println(req1.resultChan.receive()/* [0] */);
		// => 2010-11-28 23:31:08 -0500 : 2
		System.out.println(req2.resultChan.receive()/* [0] */);
		// => 2010-11-28 23:31:08 -0500 : 3

		// Notice something interesting? Both results came back with the same
		// timestamp! Our
		// clientRequests channel allowed for up to two messages in the pipe,
		// which our workers
		// immediately received, executed, and returned the results. Once again,
		// not a thread
		// or a mutex in sight.

		clientRequests.close();
		req1.resultChan.close();
		req2.resultChan.close();
	}

}
