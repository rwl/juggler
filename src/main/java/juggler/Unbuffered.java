package juggler;

import juggler.Pop.PopBlock;
import juggler.Push.PushBlock;
import juggler.errors.Rollback;

public class Unbuffered<T> extends Queue<T> {

	private int waiting_pushes;
	private int waiting_pops;

	@Override
	public boolean isBuffered() {
		return false;
	}

	@Override
	public boolean isUnbuffered() {
		return true;
	}

	@Override
	public boolean push() {
		return waiting_pops > 0;
	}

	@Override
	public boolean pop() {
		return waiting_pushes > 0;
	}

	@Override
	protected void reset_custom_state() {
		waiting_pushes = pushes.size();
		waiting_pops = pops.size();
	}

	@Override
	protected void process() {
		Operation operation = operations.getLast();

		if (operation instanceof Push) {
			waiting_pushes += 1;

			for (final Pop pop_operation : pops/* .clone() */) {
				if (operation.getBlockingOnce() != null
						&& operation.getBlockingOnce().equals(
								pop_operation.getBlockingOnce())) {
					continue;
				}

				Error error = null;
				try {
					((Push) operation).receive(new PushBlock() {
						@Override
						public void yield(final byte[] value) {
							try {
								pop_operation.send(new PopBlock() {
									@Override
									public byte[] yield() {
										return value;
									}
								});

								waiting_pops -= 1;
								operations.remove(pop_operation);
								pops.remove(pop_operation);
							} catch (Error err) {
								throw new Rollback();
							}
						}
					});
				} catch (Error err) {
					error = err;
				}

				if (error == null || error.isMessage(Once.ERROR_MSG)) {
					waiting_pushes -= 1;
					operations.pop();
					pushes.pop();
					break;
				}
			}
		} else { // Pop
			waiting_pops += 1;

			for (final Push push_operation : pushes/* .clone() */) {
				if (operation.getBlockingOnce() != null
						&& operation.getBlockingOnce().equals(
								push_operation.getBlockingOnce())) {
					continue;
				}

				Error error = null;
				try {
					((Pop) operation).send(new PopBlock() {
						@Override
						public byte[] yield() {
							final byte[] value = null;

							try {
								push_operation.receive(new PushBlock() {
									@Override
									public void yield(byte[] v) {
										value = v;
									}
								});
							} catch (Error err) {
								throw new Rollback();
							}

							waiting_pushes -= 1;
							operations.remove(push_operation);
							pushes.remove(push_operation);
							return value;
						}
					});
				} catch (Error err) {
					error = err;
				}

				if (error == null || error.isMessage(Once.ERROR_MSG)) {
					waiting_pops -= 1;
					operations.pop();
					pops.pop();
					break;
				}
			}

		}
	}

}
