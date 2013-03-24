package hoare.queues;

import hoare.Queue;

public class Buffered extends Queue {

	private int size;
	private int max;

	public Buffered(Class<?> type) {
		this(type, 1);
	}

	public Buffered(Class<?> type, int max) {
		super(type);
		if (max < 1) {
			throw new InvalidQueueSizeError("queue size must be at least 1");
		}
		this.max = max;
	}

	@Override
	public boolean isBuffered() {
		return true;
	}

	@Override
	public boolean isUnbuffered() {
		return false;
	}

	@Override
	public boolean push() {
		return max > size;
	}

	@Override
	public boolean pop() {
		return size > 0;
	}

	@Override
    protected void reset_custom_state() {
        size = queue.getSize();
    }

	@Override
	protected void process() {

        if ((pops.isEmpty() && !push()) || (pushes.isEmpty() && !pop())) {
        	return;
        }

        Operation operation = operations.getFirst();

        while (true) {
          if (operation instanceof Push) {
            if (push()) {
            	Object obj = operation.receive(); {
                size += 1;
                queue.push(obj);
            	}
              operations.remove(operation);
              pushes.remove(operation);
            } else if (pop() && operation = pops[0]) {
              continue;
            } else {
              break;
            }
          } else {  // Pop
            if (pop()) {
              operation.send(); {
                size -= 1;
                queue.shift();
              }
              operations.remove(operation);
              pops.remove(operation);
            } else if (push() && operation = pushes[0]) {
              continue;
            } else {
              break;
            }
          }

          operation = operations[0];
          if (operation == null) {
        	  break;
          }
        }
	}
}
