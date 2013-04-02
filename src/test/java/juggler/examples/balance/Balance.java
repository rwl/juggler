package juggler.examples.balance;

import juggler.Channel;
import juggler.Juggler;

import java.util.Random;

public class Balance {

    static final int N_REQUESTER = 100;
    static final int N_WORKER = 10;

    public void main(String[] args) {
        // use round-robin scheduling
        boolean roundRobin = args.length > 0 && args[0].equals("r");

        Channel<Request> work = new Channel<Request>();
        for (int i = 0; i < N_REQUESTER; i++) {
            Juggler.go(new Juggler.Consumer<Channel<Request>>() {
                @Override
                public void run(Channel<Request> work) {
                    Channel<Integer> c = new Channel<Integer>();
                    while (true) {
                        try {
                            Thread.sleep(new Random()
                                    .nextInt((int) (N_WORKER * 2e+9)));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        work.send(new Request(op, c));
                        c.receive();
                    }
                }
            }, work);
        }

        if (roundRobin) {
            new RoundRobin().balance(work);
        } else {
            new Balancer().balance(work);
        }
    }

    public interface IntFunction {
        int call();
    }

    IntFunction op = new IntFunction() {
        @Override
        public int call() {
            int n = new Random().nextInt((int) 1e+9);
            try {
                Thread.sleep(N_WORKER * n);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return n;
        }
    };
}
