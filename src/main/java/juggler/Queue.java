package juggler;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import juggler.errors.ChannelClosedError;

abstract class Queue<T> {

	protected List<T> queue;
	protected LinkedList<Operation<T>> operations;
	protected LinkedList<Push<T>> pushes;
	protected LinkedList<Pop<T>> pops;
	protected Object mutex;

	private boolean closed;

	public Queue() {
		// if (type == null) {
		// throw new UntypedError();
		// }
		// if (!(type.isAssignableFrom(Module.class))) {
		// throw new InvalidTypeError();
		// }

		this.closed = false;

		this.queue = new ArrayList<T>();
		this.operations = new LinkedList<Operation<T>>();
		this.pushes = new LinkedList<Push<T>>();
		this.pops = new LinkedList<Pop<T>>();

		this.mutex = new Object();

		reset_custom_state();
	}

	public abstract boolean isBuffered();

	public abstract boolean isUnbuffered();

	public abstract boolean poppable();

	public abstract boolean pushable();

	public void close() {
		synchronized (mutex) {
			if (closed) {
				throw new ChannelClosedError();
			}
			closed = true;
			for (Operation<T> o : operations) {
				o.close();
			}
			operations.clear();
			queue.clear();
			pushes.clear();
			pops.clear();

			reset_custom_state();
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isOpen() {
		return !closed;
	}

	public Push<T> push(T object) {
		return push(object, false);
	}

	public Push<T> push(T object, boolean deferred/* , Map options */) {

		Push<T> push = new Push<T>(object/* , options */);

		synchronized (mutex) {
			if (closed) {
				throw new ChannelClosedError();
			}
			operations.add(push);
			pushes.add(push);
			process();
		}

		if (deferred) {
			return push;
		}

		push.await();
		return push;
	}

//	public T pop() {
//		return pop(false);
//	}

	public T pop(/*boolean deferred*//* , Map options */) {
		Pop<T> pop = new Pop<T>(/* options */);

		synchronized (mutex) {
			if (closed) {
				throw new ChannelClosedError();
			}
			operations.add(pop);
			pops.add(pop);
			process();
		}

//		if (deferred) {
//			return pop;
//		}

		boolean ok = pop.await();
		return pop.getObject();// , ok;
	}

	public void remove_operations(Operation<T>[] ops) {
		synchronized (mutex) {
			if (closed) {
				return;
			}

			for (Object operation : ops) {
				operations.remove(operation);
			}

			pushes.clear();
			pops.clear();

			for (Operation<T> operation : operations) {
				if (operation instanceof Push) {
					pushes.add((Push<T>) operation);
				} else {
					pops.add((Pop<T>) operation);
				}
			}

			reset_custom_state();
		}
	}

	protected void reset_custom_state() {
		// implement in subclass...or not...
	}

	protected abstract void process();
}
