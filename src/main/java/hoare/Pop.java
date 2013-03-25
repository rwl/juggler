package hoare;

import hoare.Once.Performable;
import hoare.errors.ChannelClosedError;
import hoare.errors.Rollback;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.SerializationUtils;

final class Pop implements Operation {

	public interface PopBlock {
		byte[] yield();
	}

	private UUID uuid;
	private BlockingOnce blocking_once;
	private Notifier notifier;
	private Serializable object;

	private Lock mutex;
	private Condition cvar;
	private boolean received;
	private boolean closed;

	public Pop(UUID uuid, BlockingOnce blocking_once, Notifier notifier) {
		this.object = null;
		this.uuid = uuid == null ? UUID.randomUUID() : uuid;
		this.blocking_once = blocking_once;
		this.notifier = notifier;
		this.mutex = new ReentrantLock();
		this.cvar = mutex.newCondition();
		this.received = false;
		this.closed = false;
	}

	public boolean received() {
		return received;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	public boolean await() {
		mutex.lock();
		try {
			while (!(received || closed)) {
				cvar.await();
			}
			return received();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			mutex.unlock();
		}
		return received();
	}

	public void send(final PopBlock popBlock) throws Error {
		mutex.lock();
		try {
			if (closed) {
				throw new ChannelClosedError();
			}

			if (blocking_once != null) {
				try {
					blocking_once.perform(new Performable() {
						@Override
						public Object perform() {
							object = (Serializable) SerializationUtils.deserialize(popBlock.yield());
							received = true;
							cvar.signal();
							if (notifier != null) {
								notifier.notify(this);
							}

								return null;
						}
					});
				} catch (Error error) {
					throw error;
				}
			} else {
				try {
					this.object = (Serializable) SerializationUtils.deserialize(popBlock.yield());
					this.received = true;
					this.cvar.signal();
					if (notifier != null) {
						this.notifier.notify(this);
					}
				} catch (Rollback e) {
				}
			}
		} finally {
			mutex.unlock();
		}
	}

	@Override
	public void close() {
		mutex.lock();
		try {
			if (received) {
				return;
			}
			closed = true;
			cvar.signalAll();
			if (notifier != null) {
				notifier.notify(this);
			}
		} finally {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public BlockingOnce getBlockingOnce() {
		return blocking_once;
	}

	@Override
	public UUID getUUID() {
		return uuid;
	}
}
