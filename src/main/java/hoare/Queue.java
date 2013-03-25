package hoare;

import hoare.errors.ChannelClosedError;
import hoare.errors.UntypedError;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

abstract class Queue {

	protected Class<?> type;
	protected List<Object> queue;
	protected LinkedList<Operation> operations;
	protected LinkedList<Push> pushes;
	protected LinkedList<Pop> pops;
	protected Object mutex;

	private boolean closed;

	public Queue(Class<?> type) {
		this.type = type;

		if (type == null) {
			throw new UntypedError();
		}
//		if (!(type.isAssignableFrom(Module.class))) {
//			throw new InvalidTypeError();
//		}

		this.closed = false;

		this.queue = new ArrayList();
		this.operations = new LinkedList<Operation>();
		this.pushes = new LinkedList<Push>();
		this.pops = new LinkedList<Pop>();

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
        for (Operation o : operations) {
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
    	Pop pop = new Pop(options);

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

      boolean ok = pop.await();
      return pop.getObject();//, ok;
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
            pushes.add((Push) operation);
          } else {
            pops.add((Pop) operation);
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
