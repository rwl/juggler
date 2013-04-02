package juggler.examples.balance;

import juggler.Channel;

import java.util.concurrent.Callable;

public class Request {

    Balance.IntFunction fn;
    Channel<Integer> c;

    Request(Balance.IntFunction fn, Channel<Integer> c) {
        this.fn = fn;
        this.c = c;
    }
}
