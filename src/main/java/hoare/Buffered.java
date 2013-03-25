package hoare;

import hoare.Pop.PopBlock;
import hoare.Push.PushBlock;
import hoare.errors.InvalidQueueSizeError;

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
		size = queue.size();
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
					((Push) operation).receive(new PushBlock() {
						@Override
						public void yield(byte[] obj) {
							size += 1;
							queue.add(obj);
						}
					});

					operations.remove(operation);
					pushes.remove(operation);
				} else if (pop() && operation.equals(pops.getFirst())) {
					continue;
				} else {
					break;
				}
			} else { // Pop
				if (pop()) {
					((Pop) operation).send(new PopBlock() {
						@Override
						public byte[] yield() {
							size -= 1;
							return queue.remove(0);
						}
					});
					operations.remove(operation);
					pops.remove(operation);
				} else if (push() && operation.equals(pushes.getFirst())) {
					continue;
				} else {
					break;
				}
			}

			operation = operations.get(0);
			if (operation == null) {
				break;
			}
		}
	}
}
