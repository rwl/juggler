package juggler;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import juggler.errors.AlreadySelectedError;
import juggler.errors.BlockMissingError;
import juggler.errors.ChannelClosedError;
import juggler.errors.DefaultCaseAlreadyDefinedError;
import juggler.errors.InvalidDirectionError;


/**
 * A "select" statement chooses which of a set of possible communications will
 * proceed. It looks similar to a "switch" statement but with the cases all
 * referring to communication operations.
 *
 * - http://golang.org/doc/go_spec.html#Select_statements
 */
public class Selector {

	public interface SelectorBlock {
		void yield(Selector s);
	}

	public static void select(SelectorBlock block) {
		if (block == null) {
			throw new BlockMissingError();
		}
		Selector selector = new Selector();
		block.yield(selector);
		selector.select();
	}

	Map<UUID, Case> cases;

	class Case {
		public UUID uuid;
		public Channel channel;
		public Direction direction;
		public Object value;
		public SelectorBlock[] blk;

		Case(UUID uuid, Channel channel, Direction direction, Object value,
				SelectorBlock[] blk) {
			this.uuid = uuid;
			this.channel = channel;
			this.direction = direction;
			this.value = value;
			this.blk = blk;
		}
	}

	private List<Case> ordered_cases;
	private Map<Channel, Operation[]> operations;
	private BlockingOnce blocking_once;
	private Notifier notifier;
	private Case default_case;
	private boolean selected;

	private Selector() {
		ordered_cases = new ArrayList<Case>();
		cases = new HashMap<UUID, Selector.Case>();
		operations = new HashMap<Channel, Operation[]>();
		blocking_once = new BlockingOnce();
		notifier = new Notifier<Operation>();
		default_case = null;
		selected = false;
	}

    public interface DefaultBlock {
        public void yield();
    }

	public Case defaultCase(ReceiveBlock<Boolean> blk) {
		if (default_case != null) {
			throw new DefaultCaseAlreadyDefinedError();
		} else {
			default_case = receiveCase(new Channel<Boolean>(1), blk);
		}
		return default_case;
	}

	public void timeout(final long t, SelectorBlock blk) {
		final Channel<Boolean> s = new Channel<Boolean>(1);
		Juggler.go(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(t);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				s.send(true);
				s.close();
			}
		});
		add_case(s, Direction.TIMEOUT, null, blk);
	}

    public interface SendBlock {
        public void yield();
    }

    public interface ReceiveBlock<V> {
        public void yield(V value);
    }

    public <U> Case sendCase(Channel<U> chan, U value) {
        return add_case(chan, Direction.SEND, value, null);
    }

    public <U> Case sendCase(Channel<U> chan, U value, SendBlock blk) {
        return add_case(chan, Direction.SEND, value, blk);
    }

    public <U> Case receiveCase(Channel<U> chan) {
        return add_case(chan, Direction.RECEIVE, null, null);
    }

    public <U> Case receiveCase(Channel<U> chan, ReceiveBlock<U> blk) {
        return add_case(chan, Direction.RECEIVE, null, blk);
    }

//    <U> Case kase(Channel<U> chan, Direction direction, SelectorBlock blk) {
//        return kase(chan, direction, null, blk);
//    }
//
//    <U> Case kase(Channel<U> chan, Direction direction, U value,
//              SelectorBlock blk) {
//		if (direction != Direction.SEND && direction != Direction.RECEIVE) {
//			throw new InvalidDirectionError();
//		}
//		return add_case(chan, direction, value, blk);
//	}

	void select() {
      if (selected) {
    	  throw new AlreadySelectedError();
      }

      try {
	      if (!ordered_cases.isEmpty()) {
//	        for (Case cse : ordered_cases) {
//	          if (cse.direction == Direction.SEND) {
//	            operations.put(cse.channel, cse.channel.send(cse.value/*, :uuid => cse.uuid,
//	                                                                    :blocking_once => @blocking_once,
//	                                                                    :notifier => @notifier,
//	                                                                    :deferred => true*/));
//	          } else {  // :receive || :timeout
//	            operations.put(cse.channel, cse.channel.receive(/*:uuid => cse.uuid,
//	                                                            :blocking_once => @blocking_once,
//	                                                            :notifier => @notifier,
//	                                                            :deferred => true*/));
//	          }
	        }

	        if (default_case != null) {
	          default_case.channel.send(true/*, :uuid => @default_case.uuid, :blocking_once => @blocking_once, :notifier => @notifier, :deferred => true*/);
	        }

	        notifier.await();

//	        execute_case(notifier.getPayload());
      } finally {
    	  selected = true;
    	  close_default_channel();
    	  dequeue_operations();
      }
    }

	protected void dequeue_operations() {
		for (Entry<Channel, Operation[]> entry : operations.entrySet()) {
			entry.getKey().remove_operations(entry.getValue());
		}
	}

	protected void close_default_channel() {
		if (default_case != null) {
			default_case.channel.close();
		}
	}

	protected Case add_case(Channel chan, Direction direction,
			Object value/* =nil */, SelectorBlock[] blk) {
		UUID uuid = UUID.randomUUID();
		Case cse = new Case(uuid, chan, direction, value, blk);
		ordered_cases.add(cse);
		cases.put(uuid, cse);
		operations.put(chan, new Operation[0]);
		return cse;
	}

	protected void execute_case(Operation operation) {
		if (operation.isClosed()) {
			throw new ChannelClosedError();
		}

		Case cse = cases.get(operation.getUUID());
		// blk, direction = cse.blk, cse.direction

		if (cse.blk != null) {
//			if (cse.direction == Direction.SEND
//					|| cse.direction == Direction.TIMEOUT) {
//				cse.blk.call();
//			} else { // RECEIVE
//				cse.blk.call(operation.getObject());
//			}
		}
	}

}
