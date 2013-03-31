package juggler.examples;

import static juggler.Juggler.go;
import juggler.Channel;
import juggler.Juggler;

public class DaisyChain {

    public static void main(String[] args) {
        final int n = 100000;
        Channel<Integer> leftmost = new Channel<Integer>();
        Channel<Integer> right = leftmost;
        Channel<Integer> left = leftmost;
        for (int i = 0; i < n; i++) {
            right = new Channel<Integer>();
            go(new Juggler.BiConsumer<Channel<Integer>, Channel<Integer>>() {
                @Override
                public void run(Channel<Integer> l, Channel<Integer> r) {
                    l.send(1 + r.receive());
                }
            }, left, right);
            left = right;
        }
        go(new Juggler.Consumer<Channel<Integer>>() {
            @Override
            public void run(Channel<Integer> c) {
                c.send(1);
            }
        }, right);
        System.out.println(leftmost.receive());
    }
}
