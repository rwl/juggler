package juggler.examples.balance;

import juggler.Channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static juggler.Juggler.go;

class RoundRobin extends Balancer {

    private final List<Worker> workers = new ArrayList<Worker>(Balance.N_WORKER);
    private int i = 0;

    @Override
    protected void initPool() {
        for (int i = 0; i < Balance.N_WORKER; i++) {
            Worker w = new Worker(new Channel<Request>(Balance.N_REQUESTER));
            workers.add(w);
            go(w, done);
        }
    }

    @Override
    protected void dispatch(Request req) {
        Worker w = workers.get(i);
        w.requests.send(req);
        w.pending++;
        i++;
        if (i >= workers.size()) {
            i = 0;
        }
    }

    @Override
    protected void completed(Worker w) {
        w.pending--;
    }

    protected Collection<Worker> getPool() {
        return workers;
    }
}
