package juggler.examples.balance;

import static juggler.Juggler.go;

import juggler.Channel;
import juggler.Selector;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import static juggler.Selector.select;

public class Balancer {

    private final PriorityBlockingQueue<Worker> pool;
    Channel<Worker> done;

    public Balancer() {
        done = new Channel<Worker>(Balance.N_WORKER);
        pool = new PriorityBlockingQueue<Worker>(Balance.N_WORKER, new Comparator<Worker>() {
            @Override
            public int compare(Worker w1, Worker w2) {
                return w1.pending - w2.pending;
            }
        });
        initPool();
    }

    protected void initPool() {
        for (int i = 0; i < Balance.N_WORKER; i++) {
            Worker w = new Worker(new Channel<Request>(Balance.N_REQUESTER));
            pool.put(w);
            go(w, done);
        }
    }

    public void balance(final Channel<Request> work) {
        while (true) {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.receiveCase(work, new Selector.ReceiveBlock<Request>() {
                        @Override
                        public void yield(Request req) {
                            dispatch(req);
                        }
                    });
                    s.receiveCase(done, new Selector.ReceiveBlock<Worker>() {
                        @Override
                        public void yield(Worker w) {
                            completed(w);
                        }
                    });
                }
            });
            print();
        }
    }

    private void print() {
        int sum = 0;
        int sumsq = 0;
        for (Worker w : getPool()) {
            System.out.printf("%d ", w.pending);
            sum += w.pending;
            sumsq += w.pending * w.pending;
        }
        double avg = ((double) sum) / ((double) getPool().size());
        double variance = ((double) sumsq) / ((double) getPool().size()) - avg * avg;
        System.out.printf(" %.2f %.2f\n", avg, variance);
    }

    protected void dispatch(Request req) {
        Worker w = pool.poll();//.(*Worker)
        w.requests.send(req);
        w.pending++;
        // System.out.printf("started %p; now %d\n", w, w.pending);
        pool.put(w);
    }

    protected void completed(Worker w) {
        w.pending--;
        //	System.out.printf("finished %p; now %d\n", w, w.pending)
        pool.remove(w/*.i*/);
        pool.put(w);
    }

    protected Collection<Worker> getPool() {
        return pool;
    }
}
