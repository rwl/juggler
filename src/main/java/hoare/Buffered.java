package hoare;

import hoare.Pop.PopBlock;
import hoare.Push.PushBlock;
import hoare.errors.InvalidQueueSizeError;

public class Buffered<T> extends Queue<T> {

	private int size;
	private int max;

	public Buffered(int max) {
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
	public boolean pushable() {
		return max > size;
	}

	@Override
	public boolean poppable() {
		return size > 0;
	}

	@Override
	protected void reset_custom_state() {
		size = queue.size();
	}

	@Override
	protected void process() {
		if ((pops.isEmpty() && !pushable())
				|| (pushes.isEmpty() && !poppable())) {
			return;
		}

		Operation<T> operation = operations.getFirst();
		while (true) {
			if (operation instanceof Push) {
				if (pushable()) {
					((Push<T>) operation).receive(new PushBlock() {
						@Override
						public void yield(byte[] obj) {
							size += 1;
							queue.add(obj);
						}
					});

					operations.remove(operation);
					pushes.remove(operation);
				} else if (poppable() && operation.equals(pops.getFirst())) {
					continue;
				} else {
					break;
				}
			} else { // Pop
				if (poppable()) {
					((Pop<T>) operation).send(new PopBlock() {
						@Override
						public byte[] yield() {
							size -= 1;
							return queue.remove(0);
						}
					});
					operations.remove(operation);
					pops.remove(operation);
				} else if (pushable() && operation.equals(pushes.getFirst())) {
					continue;
				} else {
					break;
				}
			}

			operation = operations.getFirst();
			if (operation == null) {
				break;
			}
		}
	}
}
