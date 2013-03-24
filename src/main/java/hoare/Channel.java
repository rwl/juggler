package hoare;

import hoare.errors.ChannelClosedError;
import hoare.errors.InvalidDirectionError;
import hoare.errors.InvalidTypeError;

import java.util.Map;
import java.util.UUID;

public class Channel {

	private String name;
	private Direction direction;
	private Class<?> type;
	private int max;

	private boolean closed;
	private Object close_mutex;
	private Queue queue;

	public Channel(Class<?> type) {
		this(type, 0);
	}

	public Channel(Class<?> type, int max) {
		this(null, null, type, max);
	}

	public Channel(String name, Direction direction, Class<?> type, int max) {
		this.type = type;
		this.max = max;
		this.closed = false;
		this.name = name == null ? UUID.randomUUID().toString() : name;
		this.direction = direction == null ? Direction.BIDIRECTIONAL
				: direction;
		this.close_mutex = new Object();
		this.queue = Queues.register(this.name, this.type, this.max);
	}

	public Queue getQueue() {
		Queue q = this.queue;
		if (q == null) {
			throw new ChannelClosedError();
		}
		return q;
	}

	// Serialization methods

	public Channel marshal_load(boolean closed, String name, int max,
			Class<?> type, Direction direction) {
		this.closed = closed;
		this.name = name;
		this.max = max;
		this.type = type;
		this.direction = direction;
		this.queue = Queues.get(this.name);
		this.closed = this.queue == null || this.queue.isClosed();
		return this;
	}

	public Object[] marshal_dump() {
		return new Object[] { closed, name, max, type, direction };
	}

	// Sending methods

	public void send(Object object, Map options/* ={} */) {
		check_direction(Direction.SEND);
		queue.push(object, options);
	}

	public void push(Object object, Map options/* ={} */) {
		send(object, options);
	}

	public boolean push() {
		return queue.push();
	}

	public boolean send() {
		return push();
	}

	// Receiving methods

	public Object receive(Map options/* ={} */) {
		check_direction(Direction.RECEIVE);
		return queue.pop(options);
	}

	// alias :pop :receive

	public boolean pop() {
		return queue.pop();
	}

	// alias :receive? :pop?

	// Closing methods

	public void close() {
      synchronized (close_mutex) {
        if (closed) {
        	throw new ChannelClosedError();
        }
        closed = true;
        queue.close();
        queue = null;
        Queues.delete(name);
      }
    }

	public boolean isClosed() {
		return closed;
	}

	public boolean isOpen() {
		return !closed;
	}

	public void remove_operations(Operation... operations) {
		// ugly, but it overcomes the race condition without synchronization
		// since instance variable access is atomic.
		Queue q = this.queue;
		if (q != null) {
			q.remove_operations(operations);
		}
	}

	public Channel as_send_only() {
		return as_direction_only(Direction.SEND);
	}

	public Channel as_receive_only() {
		return as_direction_only(Direction.RECEIVE);
	}

	private Channel as_direction_only(Direction direction) {
		synchronized (close_mutex) {
			if (closed) {
				throw new ChannelClosedError();
			}
			return new Channel(name, direction, type, max);
		}
	}

	private void check_type(Object object) {
		if (!(object.getClass().equals(type))) {
			throw new InvalidTypeError();
		}
	}

	private void check_direction(Direction direction) {
		if (this.direction == Direction.BIDIRECTIONAL) {
			return;
		}
		if (this.direction != direction) {
			throw new InvalidDirectionError();
		}
	}

	public String getName() {
		return name;
	}

	public Direction getDirection() {
		return direction;
	}

	public Class<?> getType() {
		return type;
	}

	public int getMax() {
		return max;
	}

}
