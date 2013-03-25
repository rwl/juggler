package hoare;

import hoare.errors.InvalidQueueSizeError;

import java.util.HashMap;
import java.util.Map;

class Queues {

	private static final Object LOCK = new Object();

	private static final Map<String, Queue<?>> queues = new HashMap<String, Queue<?>>();

	private Queues() {
	}

	public static <T> Queue<T> register(String name, int max) {
		// raise Errors::Untyped unless type
		// raise Errors::InvalidType unless type.is_a?(Module)

		synchronized (LOCK) {
			@SuppressWarnings("unchecked")
			Queue<T> queue = (Queue<T>) queues.get(name);

			if (queue != null) {
				return queue;
//				if (queue.type.equals(type)) {
//					return queue;
//				} else {
//					throw new InvalidTypeError(String.format(
//							"Type %s is different than the queue's type (%s)",
//							type.getName(), queue.type.getName()));
//				}
			}

			if (max < 0) {
				throw new InvalidQueueSizeError("queue size must be at least 0");
			}

			if (max > 0) {
				queues.put(name, new Buffered<T>(max));
			} else {
				queues.put(name, new Unbuffered<T>());
			}

			return queue;
		}
	}

	public static void delete(String name) {
		synchronized (LOCK) {
			queues.remove(name);
		}
	}

	public static <T> Queue<T> get(String name) {
		synchronized (LOCK) {
			@SuppressWarnings("unchecked")
			Queue<T> queue = (Queue<T>) queues.get(name);
			return queue;
		}
	}

	public static void clear() {
		synchronized (LOCK) {
			queues.clear();
		}
	}
}
