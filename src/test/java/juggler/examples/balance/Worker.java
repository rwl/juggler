package juggler.examples.balance;

import juggler.Channel;
import juggler.Juggler;

public class Worker implements Juggler.Consumer<Channel<Worker>> {

    int i;
    Channel<Request> requests;
    int pending;

    public Worker(Channel<Request> requests) {
        this.requests = requests;
    }

    @Override
    public void run(Channel<Worker> done) {
        while (true) {
            Request req = requests.receive();
            req.c.send(req.fn.call());
            done.send(this);
        }
    }
}
