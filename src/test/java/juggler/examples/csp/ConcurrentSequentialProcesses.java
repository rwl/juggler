/*
 * Copyright (C) 2013 Thomas Kappler
 * Copyright (C) 2013 Richard Lincoln
 */
package juggler.examples.csp;

import juggler.Channel;
import juggler.Juggler;
import juggler.Juggler.BiConsumer;
import juggler.Juggler.Consumer;
import juggler.Selector;
import juggler.examples.balance.Request;

import static juggler.Juggler.go;
import static juggler.Selector.select;

/**
 * The examples from Tony Hoare's seminal 1978 paper "Communicating
 * sequential processes" implemented in Go.
 * 
 * Go's design was strongly influenced by Hoare's paper [1]. Although
 * Go differs significantly from the example language used in the
 * paper, the examples still translate rather easily. The biggest
 * difference apart from syntax is that Go models the conduits of
 * concurrent communication explicitly as channels, while the
 * processes of Hoare's language send messages directly to each other,
 * similar to Erlang. Hoare hints at this possibility in section 7.3,
 * but with the limitation that "each port is connected to exactly one
 * other port in another process", in which case it would be a mostly
 * syntactic difference.
 *
 * [1]
 * http://blog.golang.org/2010/07/share-memory-by-communicating.html
 *
 * Implementing these examples, and the careful reading of the paper
 * required to do so, were a very enlightening experience. I found the
 * iterative array of 4.2, the concurrent routines changing their
 * behavior execution of 4.5, and the highly concurrent matrix
 * multiplication of 6.2 to be particularly interesting.
 *
 * I tried to name routines and variables like in the paper, which
 * explains the now outdated upper-case names. Similarly, I tried to
 * make the function signatures as similar to the paper as possible,
 * so we mostly work directly with channels, where one would hide this
 * implementation detail in real-world code.
 *
 * Most of the examples have tests, although I have not taken a lot of
 * care to test corner cases. The test of the S53_DiningPhilosophers
 * is not really a test, it simply runs the routine for ten seconds so
 * you can observe the philosophers behavior.
 *
 * Thomas Kappler <tkappler@gmail.com>
 */
public class ConcurrentSequentialProcesses {

    /**
     * 3.1 COPY
     *
     * > "Problem: Write a process X to copy characters output by process west
     * to process, east."
     *
     * In Go, the communication channel between two processes (goroutines) is
     * explicitly represented via the chan type. So we model west and east as
     * channels of runes. In an endless loop, we read from west and write the
     * result directly to east.
     *
     * As an addition to the paper's example, we stop when west is closed,
     * otherwise we would just hang at this point. To indicate this to the
     * client, we close the east channel.
     */
    public static BiConsumer<Channel<Character>, Channel<Character>> S31_COPY = new BiConsumer<Channel<Character>, Channel<Character>>() {

        @Override
        public void run(Channel<Character> west, Channel<Character> east) {
            while (west.isOpen()) {
                east.send(west.receive());
            }
            east.close();
        }
    };

    /**
     * 3.2 SQUASH
     *
     * > "Problem: Adapt the previous program [COPY] to replace every pair of
     * consecutive asterisks "**" by an upward arrow "↑". Assume that the final
     * character input is not an asterisk."
     *
     * If we get an asterisk from west, we receive the next character as well
     * and then decide whether to send the upward arrow or the last two
     * characters as-is. Go's UTF8 support allows to treat the arrow like any
     * other character.
     */
    public static BiConsumer<Channel<Character>, Channel<Character>> S32_SQUASH = new BiConsumer<Channel<Character>, Channel<Character>>() {

        @Override
        public void run(Channel<Character> west, Channel<Character> east) {
            while (west.isOpen()) {
                char c = west.receive();
                if (c != '*') {
                    east.send(c);
                } else {
                    char c2 = west.receive();
                    if (c2 != '*') {
                        east.send(c);
                        east.send(c2);
                    } else {
                        east.send('↑');
                    }
                }
            }
            east.close();
        }
    };

    /**
     * Hoare adds a remark to 3.2 SQUASH: "(2) As an exercise, adapt this
     * process to deal sensibly with input which ends with an odd number of
     * asterisks." This version handles this case by sending a single asterisk
     * from west to east if west did not supply another character after a
     * timeout, or if west was closed in the meantime.
     */
    public static BiConsumer<Channel<Character>, Channel<Character>> S32_SQUASH_EXT = new BiConsumer<Channel<Character>, Channel<Character>>() {

        @Override
        public void run(final Channel<Character> west, final Channel<Character> east) {
            while (west.isOpen()) {
                final char c = west.receive();
                if (c != '*') {
                    east.send(c);
                } else {
                    select(new Selector.SelectorBlock() {
                        @Override
                        public void yield(Selector s) {
                            s.timeout(10000, new Selector.SelectorBlock() {
                                @Override
                                public void yield(Selector s) {
                                    east.send(c);
                                }
                            });
                            s.receiveCase(west, new Selector.ReceiveBlock<Character>() {
                                @Override
                                public void yield(Character c2/*, boolean ok*/) {
                                    if (!ok) {  // west closed
                                        east.send(c);
                                        return; //break;
                                    }
                                    if (c2 != '*') {
                                        east.send(c);
                                        east.send(c2);
                                    } else {
                                        east.send('↑');
                                    }
                                }
                            });
                        }
                    });
                }
            }
            east.close();
        }
    };

    /**
     * 3.3 DISASSEMBLE
     *
     * > "Problem: to read cards from a cardfile and output to process X the
     * stream of characters they contain. An extra space should be inserted at
     * the end of each card."
     *
     * Trivially translated to Go. We don't need to care about the indices 1 to
     * 80, range handles this for us.
     */
    public static BiConsumer<Channel<Character[]>, Channel<Character>> S33_DISASSEMBLE = new BiConsumer<Channel<Character[]>, Channel<Character>> () {

        @Override
        public void run(Channel<Character[]> cardfile, Channel<Character> X) {
            while (cardfile.isOpen()) {
                final Character[] cardimage = cardfile.receive();
                for (char r : cardimage) {
                    X.send(r);
                }
                X.send(' ');
            }
            X.close();
        }
    };

    /**
     * 3.4 ASSEMBLE
     *
     * > "Problem: To read a stream of characters from process X and print them
     * in lines of 125 characters on a lineprinter. The last line should be
     * completed with spaces if necessary."
     */
    public static BiConsumer<Channel<Character>, Channel<Character[]>> S34_ASSEMBLE = new BiConsumer<Channel<Character>, Channel<Character[]>> () {

        @Override
        public void run(Channel<Character> X, Channel<Character[]> lineprinter) {
            int linelen = 125;
            Character[] lineimage = new Character[linelen];
            int i = 0;
            while (X.isOpen()) {
                char c = X.receive();
                if (c == 0) {
                    break;
                }
                lineimage[i] = c;
                i++;
                if (i == linelen) {
                    Character[] cc = new Character[linelen];
                    System.arraycopy(lineimage, 0, cc, 0, linelen);
                    lineprinter.send(cc);
                    i = 0;
                }
            }

            // Print the last line padded with spaces.
            if (i > 0) {
                for (int j = i; j < linelen; j++) {
                    lineimage[j] = ' ';
                }
                lineprinter.send(lineimage);
            }

            lineprinter.send(null);
        }
    };

    /**
     * 3.5 Reformat
     *
     * > "Problem: Read a sequence of cards of 80 characters each, and print
     * the characters on a lineprinter at 125 characters per line. Every card
     * should be followed by an extra space, and the last line should be
     * completed with spaces if necessary."
     *
     * This is a great example of how easily concurrent processes can be
     * combined in the manner Unix pipes. No extra code is required to let the
     * data flow through the two routines we wrote earlier.
     */
    public static BiConsumer<Channel<Character[]>, Channel<Character[]>> S35_Reformat = new BiConsumer<Channel<Character[]>, Channel<Character[]>>() {

        @Override
        public void run(Channel<Character[]> cardfile, Channel<Character[]> lineprinter) {
            Channel<Character> pipe = new Channel<Character>();
            go(S33_DISASSEMBLE, cardfile, pipe);
            S34_ASSEMBLE.run(pipe, lineprinter);
        }
    };

    /**
     * 3.6 Conway's Problem
     *
     * > "Problem: Adapt the above program to replace every pair of consecutive
     * asterisks by an upward arrow."
     *
     * The implementation in four lines is a testament to the expressive
     * power of modeling programs as communicating sequential processes.
     */
    public static BiConsumer<Channel<Character[]>, Channel<Character[]>> S36_Conway = new BiConsumer<Channel<Character[]>, Channel<Character[]>>() {

        @Override
        public void run(Channel<Character[]> cardfile, Channel<Character[]> lineprinter) {
            Channel<Character> pipe1 = new Channel<Character>();
            Channel<Character> pipe2 = new Channel<Character>();
            go(S33_DISASSEMBLE, cardfile, pipe1);
            go(S32_SQUASH_EXT, pipe1, pipe2);
            S34_ASSEMBLE.run(pipe2, lineprinter);
        }
    };

    /**
     * 4. Subroutines and Data Representations
     *
     * > "A coroutine acting as a subroutine is a process operating
     * concurrently with its user process in a parallel command:
     * [subr::SUBROUTINE||X::USER]. [...] The USER will call the subroutine by
     * a pair of commands: subr!(arguments); ...; subr?(results). Any commands
     * between these two will be executed concurrently with the subroutine."
     *
     * Here the paper's influence on Go comes out clearly: coroutines are
     * goroutines and launching them via "!" is the "go" command. Only reading
     * the results is quite different in Go because of the explicit
     * representation of the conduit between coroutine and main routine, the
     * channel.
     */

    /**
     * 4.1  Function: Division With Remainder
     *
     * > "Problem: Construct a process to represent a function-type subroutine,
     * which accepts a positive dividend and divisor, and returns their integer
     * quotient and remainder. Efficiency is of no concern."
     */
    public static Juggler.TriConsumer<Integer, Integer, Channel<DivResult>> S41_DIV = new Juggler.TriConsumer<Integer, Integer, Channel<DivResult>>() {

        @Override
        public void run(Integer x, Integer y, Channel<DivResult> res) {
            int quot = 0;
            int rem = x;
            while (rem >= y) {
                rem -= y;
                quot += 1;
            }
            res.send(new DivResult(quot, rem));
        }
    };

    public static class DivResult {
        int quot, rem;
        DivResult(int quot, int rem) {
            this.quot = quot;
            this.rem = rem;
        }
    }

    /**
     * 4.2 Recursion: Factorial
     *
     * > "Problem: Compute a factorial by the recursive method, to a given
     * limit."
     *
     * This example is fascinating. It introduces the "iterative array" which
     * kept me puzzled for a while, but made for a great a-ha moment when I got
     * it. It's an array of coroutines, so that for a given integer input i,
     * the coroutine at index i knows how to deal with it. By addressing its
     * neighbor coroutines at i-1 and i+1, a coroutine can communicate with the
     * others to break down the overall problem. That sounds pretty abstract
     * and will be clearer in the following examples.
     *
     * To compute n!, we use the simple recurrence `n! = n * (n-1)!`, with `0!
     * = 1! = 1`. In our iterative array of goroutines, when routine i receives
     * the value x, it sends x-1 up the chain, i.e., to the right in the
     * iterative array. This continues until the value is 0 or 1 and we hit the
     * base case of the recursion. The coroutines then pass values back down
     * the chain, i.e. leftwards, starting with 1. When routine i receives a
     * result passed back down the chain, it multiplies it with x and passes on
     * the result. When it arrives at routine 0, n! is computed.
     *
     * The caller doesn't see this process. We only need to expose goroutine 0,
     * which can compute any factorial by passing the value up the chain and
     * waiting for the result. Any factorial up to the limit of the length of
     * the iterative array, that is, which has to be given when creating the
     * factorial iterative array.
     *
     * Go models communication between goroutines with explicit channel values,
     * so in this implementation the iterative array is an array of channels.
     * When we create it, we launch the corresponding goroutines at the same
     * time.
     */
    public static Channel<Integer> S42_facM(int limit) {
        final Channel<Integer>[] fac = new Channel/*<Integer>(limit+1)*/[limit+1];
        fac[0] = new Channel<Integer>();

        for (int i = 1; i <= limit; i++) {
            fac[i] = new Channel<Integer>();
            go(new Juggler.Consumer<Integer>() {
                @Override
                public void run(Integer i) {
                    while (true) {
                        int n = fac[i-1].receive();
                        if (n == 0 || n == 1) {
                            fac[i-1].send(1);
                        } else {
                            fac[i].send(n - 1);
                            int r = fac[i].receive();
                            fac[i-1].send(n * r);
                        }
                    }
                }
            }, i);
        }
        return fac[0];
    }

    /**
     * 4.3 Data Representation: Small Set of Integers
     *
     * > "Problem: To represent a set of not more than 100 integers as a
     * process, S, which accepts two kinds of instruction from its calling
     * process X: (1) S!insert(n), insert the integer n in the set, and (2)
     * S!has(n); ... ; S?b, b is set true if n is in the set, and false
     * otherwise."
     */
    public static class S43_IntSet {
        int[] content;
        Object/*sync.RWMutex*/ writeLock;

        public S43_IntSet(int[] content) {
            this.content = content;
        }

        // If s contains n, return its index, otherwise return the next free index.
        public int search(int n) {
            for (int i = 0; i < content.length; i++) {
                int el = content[i];
                if (el == n) {
                    return i;
                }
            }
            return content.length;
        }

        public static S43_IntSet newIntSet() {
            return new S43_IntSet(new int[100]);
        }

        /**
         * Send true on res if s contains n, otherwise false.
         *
         * The caller needs to pass in the result channel because otherwise we'd
         * need to return a channel here to make the operation asynchronous. But
         * what channel? If we make a new one every time, it's wasteful. If we have
         * only one, we need to lock access to it, and we cannot close it, so the
         * caller might wait indefinitely on it (although that would be an error in
         * the client).
         */
        public final BiConsumer<Integer, Channel<Boolean>> Has = new BiConsumer<Integer, Channel<Boolean>>() {
            @Override
            public void run(final Integer n, final Channel<Boolean> res) {
                go(new Runnable() {
                    @Override
                    public void run() {
                        writeLock.RLock();
                        int i = search(n);
                        res.send(i < content.length);
                        writeLock.RUnlock();
                    }
                });
            }
        };

        /**
         * Insert the number n into the set, if there is still room. Note that in
         * Hoare's specification the client has no way of knowing whether the set
         * is full or not, safe for testing whether the insertion worked with
         * has().
         *
         * The client can also not know when the insertion is complete. Parallel
         * insertions and Has() queries are protected by a mutex. But there is no
         * guarantee that the Insert() has even started to run, i.e., actually
         * acquired the lock. To protect against seeing stale data, the client can
         * pass in an ack channel and block on it.
         */
        public final BiConsumer<Integer, Channel<Integer>> Insert = new BiConsumer<Integer, Channel<Integer>>() {

            @Override
            public void run(final Integer n, final Channel<Integer> ack) {
                go(new Runnable() {
                    @Override
                    public void run() {
                        writeLock.Lock();
                        int i = search(n);
                        int size = content.length;
                        // If i is < size, n is already in the set, see Has().
                        if (i == size && size <= 100) {
                            content = append(content, n);
                        }
                        writeLock.Unlock();
                        if (ack != null) {
                            ack.send(1);
                        }
                    }
                });
            }
        };

        /**
         * 4.4 Scanning a Set
         *
         * > "Problem: Extend the solution to 4.3 by providing a fast method for
         * scanning all members of the set without changing the value of the set."
         *
         * The implementation below looks quite different from Hoare's. Go's
         * channels in combination with `range` make the implementation trivial. An
         * implementation closer to the pseudocode in the paper might return a chan
         * int and a chan bool "noneleft" to the caller, sending a signal on
         * noneleft after the iteration.
         */
        public Channel<Integer> Scan() {
            final Channel<Integer> res = new Channel<Integer>();
            go(new Runnable() {
                @Override
                public void run() {
                    writeLock.RLock();
                    for (int c : content) {
                        res.send(c);
                    }
                    writeLock.RUnlock();
                    res.close();
                }
            });
            return res;
        }

        /**
         * 4.5 Recursive Data Representation: Small Set of Integers
         *
         * > "Problem: Same as above, but an array of processes is to be used to
         * achieve a high degree of parallelism. Each process should contain at
         * most one number. When it contains no number, it should answer "false" to
         * all inquiries about membership. On the first insertion, it changes to a
         * second phase of behavior, in which it deals with instructions from its
         * predecessor, passing some of them on to its successor. The calling
         * process will be named S(0). For efficiency, the set should be sorted,
         * i.e. the ith process should contain the ith largest number."
         *
         * We use the iterative array technique from 4.2 again here.
         *
         * I found this exercise to be the trickiest one. The *least*
         * operation had me puzzled for a while. I tried to make it work,
         * analogous to the other three, with a single channel used both to
         * communicate with the client and to communicate internally between
         * the goroutines.
         */
        public static class ParIntSetRes {
            Channel<Integer> insert;
            Channel<S45_HasQuery> has;
            Channel<Channel<Integer>> scan;
            Channel<S45_LeastQuery> leastQuery;
            public ParIntSetRes(Channel<Integer> insert, Channel<S45_HasQuery> has,
                    Channel<Channel<Integer>> scan, Channel<S45_LeastQuery> leastQuery) {
                this.insert = insert;
                this.has = has;
                this.scan = scan;
                this.leastQuery = leastQuery;
            }
        }

        public static ParIntSetRes S45_ParIntSet(final int limit) {
            final Channel<Integer>[] insert = new Channel[limit+1];
            final Channel<S45_HasQuery>[] has = new Channel[limit+1];
            final Channel<Channel<Integer>>[] scan = new Channel[limit+1];
            final Channel<S45_LeastResponse>[] least = new Channel[limit+1];
            /*
             * Only the first one of these will actually be created and used to
             * communicate with the client, but we make an array to be able to
             * use the same code for all goroutines.
             */
            final Channel<S45_LeastQuery>[] leastQuery = new Channel[limit+1];

            for (int i = 1; i <= limit; i++) {
                insert[i] = new Channel<Integer>();
                has[i] = new Channel<S45_HasQuery>();
                scan[i] = new Channel<Channel<Integer>>();
                least[i] = new Channel<S45_LeastResponse>();
                if (i == 1) {
                    leastQuery[i] = new Channel<S45_LeastQuery>();
                }

                go(new Juggler.Consumer<Integer>() {
                    @Override
                    public void run(final Integer i) {
                        // This goroutine stores n.
                        final int n;

//                        EMPTY:
                        while (true) {
                            select(new Selector.SelectorBlock() {
                                @Override
                                public void yield(Selector s) {
                                    s.receiveCase(has[i], new Selector.ReceiveBlock<S45_HasQuery>() {
                                        @Override
                                        public void yield(S45_HasQuery q) {
                                            q.Response.send(false);
                                        }
                                    });
                                    s.receiveCase(insert[i], new Selector.ReceiveBlock<Integer>() {
                                        @Override
                                        public void yield(Integer value) {
                                            break EMPTY;
                                        }
                                    });
                                    s.receiveCase(scan[i], new Selector.ReceiveBlock<Channel<Integer>>() {
                                        @Override
                                        public void yield(Channel<Integer> c) {
                                            c.close();
                                        }
                                    });
                                    s.sendCase(least[i], new S45_LeastResponse(true));
                                    s.receiveCase(leastQuery[i], new Selector.ReceiveBlock<S45_LeastQuery>() {
                                        @Override
                                        public void yield(S45_LeastQuery q) {
                                            q.send(new S45_LeastResponse(true));
                                        }
                                    });
                                }
                            });
                        }

                        // NONEMPTY:
                        while (true) {
                            select(new Selector.SelectorBlock() {
                                @Override
                                public void yield(Selector s) {
                                    s.receiveCase(has[i], new Selector.ReceiveBlock<S45_HasQuery>() {
                                        @Override
                                        public void yield(S45_HasQuery q) {
                                            if (q.N <= n) {
                                                q.Response.send(q.N == n);
                                            } else {
                                                // We don't have q, ask pass on the request.
                                                if (i == limit) {
                                                    q.Response.send(false);
                                                } else {
                                                    has[i+1].send(q);
                                                }
                                            }
                                        }
                                    });
                                    s.receiveCase(insert[i], new Selector.ReceiveBlock<Integer>() {
                                        @Override
                                        public void yield(Integer m) {
                                            // If m is larger than our number n, pass it on,
                                            // otherwise pass ours on and keep m.
                                            if (m < n) {
                                                if (i < limit) {
                                                    insert[i+1].send(n);
                                                }
                                                n = m;
                                            } else if (m > n && i < limit) {
                                                insert[i+1].send(m);
                                            }
                                        }
                                    });
                                    s.receiveCase(scan[i], new Selector.ReceiveBlock<Channel<Integer>>() {
                                        @Override
                                        public void yield(Channel<Integer> c) {
                                            // Send our value and pass on the response channel.
                                            c.send(n);
                                            scan[i+1].send(c);
                                        }
                                    });
                                    s.sendCase(least[i], new S45_LeastResponse(n, false), new Selector.SendBlock() {
                                        @Override
                                        public void yield() {
                                            // Get the least from the next goroutine.
                                            S45_LeastResponse nextL = least[i+1].receive();

                                            // Shift one to the left in our concurrent list.
                                            if (nextL.NoneLeft) {
                                                goto EMPTY;
                                            } else {
                                                n = nextL.Least;
                                            }
                                        }
                                    });
                                    s.receiveCase(leastQuery[i], new Selector.ReceiveBlock<S45_LeastQuery>() {
                                        @Override
                                        public void yield(S45_LeastQuery leastQ) {
                                            // This case is only for the client-facing goroutine 1.

                                            // Get the least from the next goroutine.
                                            S45_LeastResponse nextL = least[i+1].receive();

                                            // Send our number to the client, and the reply from
                                            // the next goroutine whether there are more to come.
                                            leastQ.send(new S45_LeastResponse(n, nextL.NoneLeft));

                                            // Shift one to the left in our concurrent list.
                                            if (nextL.NoneLeft) {
                                                goto EMPTY;
                                            } else {
                                                n = nextL.Least;
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }
                }, i);
            }

            return new ParIntSetRes(insert[1], has[1], scan[1], leastQuery[1]);
        }
    }

    public static class S45_HasQuery {
        int N;
        Channel<Boolean> Response;
        public S45_HasQuery(int N, Channel<Boolean> Response) {
            this.N = N;
            this.Response = Response;
        }
    }

    public static class S45_LeastQuery extends Channel<S45_LeastResponse> {}

    public static class S45_LeastResponse {
        int Least;
        boolean NoneLeft;
        public S45_LeastResponse(boolean NoneLeft) {
            this.NoneLeft = NoneLeft;
        }
        public S45_LeastResponse(int Least, boolean NoneLeft) {
            this.Least = Least;
            this.NoneLeft = NoneLeft;
        }
    }

    /**
     * 5.1 Bounded Buffer
     *
     * > "Problem: Construct a buffering process X to smooth variations in the
     * speed of output of portions by a producer process and input by a
     * consumer process. The consumer contains pairs of commands X!more( );
     * X?p, and the producer contains commands of the form X!p. The buffer
     * should contain up to ten portions."
     *
     * This is exactly what Go's buffered channels provide. We will do it
     * manually here. Even that would be trivial using a `select` containing
     * both the producer receive and the consumer send, so we'll do it without
     * select. The implementation strictly follows Hoare's pseudo-code from the
     * paper.
     *
     * We do actually use `select`, but only for single channel operations, in
     * order to exploit the semantics of its `default` case: try the channel
     * operation, but don't block if the other end isn't ready, instead just
     * skip it and continue.
     */
    public static Channel<Integer>[] S51_Buffer(final int bufSize) {
        final int[] buffer = new int[bufSize];
        final Channel<Integer> consumer = new Channel<Integer>();
        final Channel<Integer> producer = new Channel<Integer>();

        final int in = 0, out = 0;
        go(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (in < out+10) {
                        // We have room in the buffer, check the producer.
                        select(new Selector.SelectorBlock() {
                            @Override
                            public void yield(Selector s) {
                                s.receiveCase(producer, new Selector.ReceiveBlock<Integer>() {
                                    @Override
                                    public void yield(Integer i) {
                                        buffer[in%bufSize] = i;
                                        in++;
                                    }
                                });
                                s.defaultCase(null); // don't block
                            }
                        });
                    }

                    if (out < in) {
                        // We have something in the buffer, check the consumer.
                        select(new Selector.SelectorBlock() {
                            @Override
                            public void yield(Selector s) {
                                s.sendCase(consumer, buffer[out%bufSize], new Selector.SendBlock() {
                                    @Override
                                    public void yield() {
                                        out++;
                                    }
                                });
                                s.defaultCase(null); // don't block
                            }
                        });
                    }
                    // Sleep for a bit here to avoid busy waiting?
                }

            }
        });

        return new Channel[] {consumer, producer};
    }

    /**
     * 5.2 Integer Semaphore
     *
     * > "Problem: To implement an integer semaphore, S, shared among an array
     * X(i:I..100) of client processes. Each process may increment the
     * semaphore by S!V() or decrement it by S!P(), but the latter command must
     * be delayed if the value of the semaphore is not positive."
     *
     * This is a nice one to write using `select`. We use two channels, inc and
     * dec, for the two operations the semaphore offers. If dec isn't possible
     * because the semaphore is 0, the client's channel send blocks.
     *
     * The number of clients, 100 in the paper, doesn't matter for the inc and
     * dec operations since, in contrast to Hoare's pseudocode, we don't need
     * to explicitly scan all clients in the channel receive operations. We
     * would need to know the number if we wanted to keep track of active
     * clients and shut down the semaphore once they are all finished. I didn't
     * implement this, but it would be easy to do with an `activeClients int`
     * variable and a `done` channel that decrements it.
     *
     * A problem I faced initially was that I put both the inc and the dec
     * receive operations into one select. This would require a guard on the
     * dec receive, something like `case val > 0 && <- dec:`, but Go doesn't
     * support that. So I would receive from dec, thereby acknowledging the
     * operation to the caller, before knowing that the decrement was legal.
     * The solution is obvious in retrospect: try the dec receive in a separate
     * select, protected by a `val > 0` guard.
     */
    static class S52_Semaphore {
        Channel<Object> inc, dec;
        public void Inc() {
            inc.send(new Object());
        }
        public void Dec() {
            dec.send(new Object());
        }
    }

    public static S52_Semaphore S52_NewSemaphore() {
        final S52_Semaphore s = new S52_Semaphore();
        s.inc = new Channel<Object>();
        s.dec = new Channel<Object>();

        final int val = 0;

        go(new Runnable() {
            @Override
            public void run() {
                // We need at least one increment before we can react to dec.
                s.inc.receive();
                val++;

                while (true) {
                    select(new Selector.SelectorBlock() {
                        @Override
                        public void yield(Selector sel) {
                            sel.receiveCase(s.inc, new Selector.ReceiveBlock<Object>() {
                                @Override
                                public void yield(Object _) {
                                    val++;
                                }
                            });
                            sel.receiveCase(s.dec, new Selector.ReceiveBlock<Object>() {
                                @Override
                                public void yield(Object value) {
                                    val--;
                                    // If val is 0, we need an inc before we can continue.
                                    if (val == 0) {
                                        s.inc.receive();
                                        val++;
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        return s;
    }

    /**
     * 5.3 Dining Philosophers (Problem due to E.W. Dijkstra)
     *
     * > "Problem: Five philosophers spend their lives thinking and eating. The
     * philosophers share a common dining room where there is a circular table
     * surrounded by five chairs, each belonging to one philosopher. In the
     * center of the table there is a large bowl of spaghetti, and the table is
     * laid with five forks (see Figure 1). On feeling hungry, a philosopher
     * enters the dining room, sits in his own chair, and picks up the fork on
     * the left of his place. Unfortunately, the spaghetti is so tangled that
     * he needs to pick up and use the fork on his right as well. When he has
     * finished, he puts down both forks, and leaves the room. The room should
     * keep a count of the number of philosophers in it."
     *
     * The dining philosophers are famous in Computer Science because they
     * illustrate the problem of deadlock. As Hoare explains, "The solution
     * given above does not prevent all five philosophers from entering the
     * room, each picking up his left fork, and starving to death because he
     * cannot pick up his right fork."
     */
    public static void S53_DiningPhilosophers(int runFor) {
        // The room is a goroutine that listens on a channel to signal "enter"
        // and one to signal "exit".
        final Channel<Integer> enterRoom = new Channel<Integer>();
        final Channel<Integer> exitRoom = new Channel<Integer>();
        Runnable room = new Runnable() {

            @Override
            public void run() {
                int occupancy = 0;
                while (true) {
                    select(new Selector.SelectorBlock() {
                        @Override
                        public void yield(Selector s) {
                            s.receiveCase(enterRoom, new Selector.ReceiveBlock<Integer>() {
                                @Override
                                public void yield(Integer i) {
                                    if (occupancy < 4) {
                                        occupancy++;
                                    } else {
                                        // If all philosophers sit down to eat, they starve.
                                        // Wait for someone to leave.
                                        System.out.printf("%v wants to enter, but must wait.\n", i);
                                                exitRoom.receive();
                                        // Enter the room, occupancy stays the same.
                                        System.out.printf("%v can finally enter!\n", i);
                                    }
                                }
                            });
                            s.receiveCase(exitRoom, new Selector.ReceiveBlock<Integer>() {
                                @Override
                                public void yield(Integer _) {
                                    occupancy--;
                                }
                            });
                        }
                    });
                }
            }
        };

        // The forks are goroutines listening to pickup and putdown channels
        // like the room, but we need one channel per philosopher to
        // distinguish them so that we can match pickup and putdown of a fork.
        final Channel<Integer>[] pickup = new Channel[5];
        final Channel<Integer>[] putdown = new Channel[5];
        for (int i = 0; i < 5; i++) {
            pickup[i] = new Channel<Integer>();
            putdown[i] = new Channel<Integer>();
        }
        Consumer<Integer> fork = new Consumer<Integer>() {
            @Override
            public void run(final Integer i) {
                while (true) {
                    select(new Selector.SelectorBlock() {
                        @Override
                        public void yield(Selector s) {
                            s.receiveCase(pickup[i], new Selector.ReceiveBlock<Integer>() {
                                @Override
                                public void yield(Integer value) {
                                    putdown[i].receive();
                                }
                            });
                            s.receiveCase(pickup[abs(i-1)%5], new Selector.ReceiveBlock<Integer>() {
                                @Override
                                public void yield(Integer value) {
                                    putdown[abs(i-1)%5].receive();
                                }
                            });
                        }
                    });
                }
            }
        };

        // A philospher leads a simple life.
        Consumer<Integer> philosopher = new Consumer<Integer>() {
            @Override
            public void run(Integer i) {
                while (true) {
                    think(i);
                    enterRoom.send(i);
                    pickup[i].send(i);
                    pickup[(i+1)%5].send(i);
                    eat(i);
                    putdown[i].send(i);
                    putdown[(i+1)%5].send(i);
                    exitRoom.send(i);
                }
            }
        };

        // Launch the scenario.
        go(room);
        for (int i = 0; i < 5; i++) {
            go(fork, i);
        }
        for (int i = 0; i < 5; i++) {
            go(philosopher, i);
        }

        try {
            Thread.sleep(runFor);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Thinking and eating are sleeps followed by a log message so we know
    // what's going on.
    private static void think(int i) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("%v thought.\n", i);
    }

    private static void eat(int i) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("%v ate.\n", i);
    }

    private static int abs(int x) {
        if (x < 0) {
            return -x;
        }
        return x;
    }

    /**
     * 6.1 Prime Numbers: The Sieve of Eratosthenes
     *
     * > "Problem: To print in ascending order all primes less than 10000. Use
     * an array of processes, SIEVE, in which each process inputs a prime from
     * its predecessor and prints it. The process then inputs an ascending
     * stream of numbers from its predecessor and passes them on to its
     * successor, suppressing any that are multiples of the original prime."
     *
     * Here I ran into a problem that I suspect is with the algorithm itself.
     * It uses one process aka goroutine per prime number. The pseudocode in
     * the paper instantiates 101 processes, enough for the first 100 primes.
     * However, it sends the numbers up to 10.000 to the first process, which
     * contain the first 1229 primes. Maybe I overlooked something about how
     * the paper's pseudo-implementation handles this, but in my Go
     * implementation I get a deadlock after these first 100 primes.
     *
     * Not wanting to spend too much time on this, I changed the function
     * signature to accept a numPrimes parameter giving the number of
     * primes to generate.
     */
    public static void S61_SIEVE(final int numPrimes, final Channel<Integer> primes) {
        final Channel<Integer>[] sieve = new Channel[numPrimes];
        sieve[numPrimes-1] = new Channel<Integer>();
        final Channel<Boolean> done = new Channel<Boolean>();

        for (int i = 0; i < numPrimes-1; i++) {
            sieve[i] = new Channel<Integer>();

            go(new Consumer<Integer>() {
                @Override
                public void run(Integer ii) {
                    int p;
                    try {
                        p = sieve[ii].receive();
                    } catch (Error e) {
                        return;
                    }

                    primes.send(p);

                    int mp = p;  // mp is a multiple of p
                    while (sieve[ii].isOpen()) {
                        int m = sieve[ii].receive();
                        while (m > mp) {
                            mp += p;
                        }
                        if (m < mp) {
                            sieve[ii+1].send(m);
                        }
                    }
                }
            }, i);
        }

        go(new Runnable() {
            @Override
            public void run() {
                int p = sieve[numPrimes-1].receive();
                primes.send(p);
                done.send(true);
            }
        });

        // Send 2, then all odd numbers up to upto.
        sieve[0].send(2);
        int n = 3;
//        SENDNUMBERS:
        while (true) {
            select(new Selector.SelectorBlock() {
                @Override
                public void yield(Selector s) {
                    s.sendCase(sieve[1], n);
                    s.receiveCase(done, new Selector.ReceiveBlock<Boolean>() {
                        @Override
                        public void yield(Boolean _) {
                            break SENDNUMBERS;
                        }
                    };
                }
            });
            n += 2;
        }

        primes.send(-1);
    }

    // A matrix for use in example 6.2, matrix multiplication.
    static class S62_Matrix {
        double[][] A;
        Channel<Double>[] WEST, SOUTH;
        Channel<Double>[][] eastward, southward;

        public S62_Matrix(double[][] A) {
            this.A = A;
        }

        // A constant source of zeros from the top.
        public void NORTH(int col) {
            while (true) {
                southward[0][col].send(0.0);
            }
        }

        // A sink on the right.
        public void EAST(int row) {
            int rightmost = eastward[row].length - 1;
            while (eastward[row][rightmost].isOpen()) {
                eastward[row][rightmost].receive();
                // do nothing, just consume
            }
        }

        // A concurrent routine for a matrix cell that's not on an edge.
        public void CENTER(int row, int col) {
            while (eastward[row][col-1].isOpen()) {
                Double x = eastward[row][col-1].receive();
                eastward[row][col].send(x);
                Double sum = southward[row-1][col].receive();
                southward[row][col].send(A[row-1][col-1]*x + sum);
            }
        }
    }

    /**
     * 6.2 An Iterative Array: Matrix Multiplication
     *
     * > "Problem: A square matrix A of order 3 is given. Three streams
     * are to be input, each stream representing a column of an array IN.
     * Three streams are to be output, each representing a column of" the
     * product matrix IN × A."
     *
     * Make a new matrix with the given values (rows, then columns). This
     * constructor will launch the goroutines for NORTH, EAST and CENTER
     * as described in the paper. The client can then send the values of
     * row i of IN to WEST[i] and read column j of IN × A from SOUTH[j].
     * See the test for an example.
     */
    public static S62_Matrix S62_NewMatrix(double[][] values) {
        int numRows = values.length;
        int numCols = values[0].length;

        S62_Matrix m = new S62_Matrix(values);

        m.eastward = new Channel[numRows+1][];
        for (int i = 0; i < numRows+1; i++) {
            m.eastward[i] = new Channel[numCols+1];
            for (int j = 0; j < numCols+1; j++) {
                m.eastward[i][j] = new Channel<Double>();
            }
        }

        m.southward = new Channel[numRows+1][];
        for (int i = 0; i < numRows+1; i++) {
            m.southward[i] = new Channel[numCols+1];
            for (int j = 0; j < numCols+1; j++) {
                m.southward[i][j] = new Channel<Double>();
            }
        }

        m.WEST = new Channel[numRows];
        for (int row = 1; row <= numRows; row++) {
            m.WEST[row-1] = m.eastward[row][0];
        }

        m.SOUTH = m.southward[numRows][1:];

        for (int col = 1; col <= numCols; col++) {
            go(m.NORTH, col);
        }

        for (int row = 1; row <= numRows; row++) {
            go(m.EAST, row);
        }

        for (int row = 1; row <= numRows; row++) {
            for (int col = 1; col <= numCols; col++) {
                go(m.CENTER, row, col);
            }
        }

        return m;
    }
}
