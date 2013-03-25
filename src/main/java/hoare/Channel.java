package hoare;

import hoare.errors.ChannelClosedError;
import hoare.errors.InvalidDirectionError;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;

public class Channel<T> implements Serializable {

	private static final long serialVersionUID = 8376740498686707230L;

	private String name;
	private Direction direction;
	private int max;

	private boolean closed;
	private Object close_mutex;
	private Queue<T> queue;

	public Channel() {
		this(0);
	}

	public Channel(int max) {
		this(null, null, max);
	}

	public Channel(String name, Direction direction, int max) {
		this.max = max;
		this.closed = false;
		this.name = name == null ? UUID.randomUUID().toString() : name;
		this.direction = direction == null ? Direction.BIDIRECTIONAL
				: direction;
		this.close_mutex = new Object();
		this.queue = Queues.<T>register(this.name, this.max);
	}

	public Queue<T> getQueue() {
		Queue<T> q = this.queue;
		if (q == null) {
			throw new ChannelClosedError();
		}
		return q;
	}

	// Serialization methods

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(closed);
        out.writeUTF(name);
        out.writeInt(max);
        out.writeObject(direction);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		closed = in.readBoolean();
		name = in.readUTF();
		max = in.readInt();
		direction = (Direction) in.readObject();
		queue = Queues.get(name);
		closed = queue == null || queue.isClosed();
    }

	// Sending methods

	public void send(T object/*, Map options*/) {
		check_direction(Direction.SEND);
		queue.push(object/*, options*/);
	}

	public void push(T object/*, Map options*/) {
		send(object/*, options*/);
	}

	public boolean pushable() {
		return queue.pushable();
	}

	public boolean send() {
		return pushable();
	}

	// Receiving methods

	public T receive(/*Map options*/) {
		check_direction(Direction.RECEIVE);
		return queue.pop(/*options*/);
	}

	public T pop() {
		return receive();
	}

	// alias :pop :receive

	public boolean poppable() {
		return queue.poppable();
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

	public void remove_operations(Operation<T>... operations) {
		// ugly, but it overcomes the race condition without synchronization
		// since instance variable access is atomic.
		Queue<T> q = this.queue;
		if (q != null) {
			q.remove_operations(operations);
		}
	}

	public Channel<T> as_send_only() {
		return as_direction_only(Direction.SEND);
	}

	public Channel<T> as_receive_only() {
		return as_direction_only(Direction.RECEIVE);
	}

	private Channel<T> as_direction_only(Direction direction) {
		synchronized (close_mutex) {
			if (closed) {
				throw new ChannelClosedError();
			}
			return new Channel<T>(name, direction, max);
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

//	public Class<?> getType() {
//		return T;
//	}

	public int getMax() {
		return max;
	}

}
