package hoare;

import java.util.HashMap;
import java.util.Map;

public class Queues {

	private static final Object LOCK = new Object();

	private static final Map<String, Queue> queues = new HashMap<String, Queue>();

	private Queues() {
	}

	public static Queue register(String name, Class<?> type, int max) {
//    raise Errors::Untyped unless type
//    raise Errors::InvalidType unless type.is_a?(Module)

    synchronized (LOCK) {
      Queue queue = queues.get(name);

      if (queue != null) {
        if (queue.getType().equals(type)) {
          return queue;
        } else {
          throw new InvalidTypeError(String.format("Type %s is different than the queue's type (%s)",
        		  type.getName(), queue.getType().getName()));
        }
      }

      if (max < 0) {
    	  throw new InvalidQueueSize("queue size must be at least 0")
      }

      if (max > 0) {
        queues.put(name, new Buffered(type, max));
      } else {
        queues.put(name, new Unbuffered(type));
      }
    }
	}

	public static void delete(String name) {
		synchronized (LOCK) {
			queues.remove(name);
		}
	}

	public static Queue get(String name) {
		synchronized (LOCK) {
			return queues.get(name);
		}
	}

	public static void clear() {
		synchronized (LOCK) {
			queues.clear();
		}
	}
}
