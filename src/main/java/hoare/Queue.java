package hoare;

import java.util.List;
import java.util.Map;

public abstract class Queue {

	private Class<?> type;
	private List queue;
	private List operations;
	private List pushes;
	private List pops;
	private Object mutex;

	private boolean closed;

	public Queue(Class<?> type) {
		this.type = type;

		if (type == null) {
			throw new UntypedError();
		}
		if (!(type.isAssignableFrom(Module.class))) {
			throw new InvalidTypeError();
		}

		this.closed = false;

		this.queue = new ArrayList();
		this.operations = new ArrayList();
		this.pushes = new ArrayList();
		this.pops = new ArrayList();

		this.mutex = new Object();

		reset_custom_state();
	}

    public abstract boolean isBuffered();

    public abstract boolean isUnbuffered();

    public abstract boolean pop();

    public abstract boolean push();

    public void close() {
      synchronized (mutex) {
        if (closed) {
        	throw new ChannelClosedError();
        }
        closed = true;
        for (Object o : operations) {
        	o.close;
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

    public Push push(Object object, Map options/*={}*/) {
//      raise Errors::InvalidType unless object.is_a?(@type)

    	Push push = new Push(object, options);

      synchronized (mutex) {
        if (closed) {
        	throw new ChannelClosedError();
        }
        operations.add(push);
        pushes.add(push);
        process();
      }

      if (options.get("deferred")) {
    	  return push;
      }

      push.wait();
      return push;
    }

    public Pop pop(Map options/*={}*/) {
      pop = new Pop(options);

      synchronized (mutex) {
        if (closed) {
        	throw new ChannelClosedError();
        }
        operations.add(pop);
        pops.add(pop);
        process();
      }

      if (options.get("deferred")) {
    	  return pop;
      }

      boolean ok = pop.wait();
      return pop.object, ok;
    }

    public void remove_operations(Operation[] ops) {
      synchronized (mutex) {
        if (closed) {
        	return;
        }

        for (Object operation : ops) {
          operations.remove(operation);
        }

        pushes.clear();
        pops.clear();

        for (Object operation : operations) {
          if (operation instanceof Push) {
            pushes.add(operation);
          } else {
            pops.add(operation);
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
