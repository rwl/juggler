package juggler;


import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import juggler.Once.Performable;
import juggler.errors.ChannelClosedError;
import juggler.errors.Rollback;

import org.apache.commons.lang.SerializationUtils;

final class Push<T/* extends Serializable*/> implements Operation<T> {

	public interface PushBlock {
		void yield(byte[] obj);
	}

	private UUID uuid;
	private BlockingOnce blocking_once;
	private Notifier notifier;
	private byte[] object;

	private Lock mutex;
	private Condition cvar;
	private boolean sent;
	private boolean closed;

	public Push(T obj) {
		this(obj, null, null, null);
	}

	public Push(T obj, UUID uuid, BlockingOnce blocking_once,
			Notifier notifier) {
		this.object = SerializationUtils.serialize(obj);
		this.uuid = uuid == null ? UUID.randomUUID() : uuid;
		this.blocking_once = blocking_once;
		this.notifier = notifier;
		this.mutex = new ReentrantLock();
		this.cvar = mutex.newCondition();
		this.sent = false;
		this.closed = false;
	}

	public boolean sent() {
		return sent;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	public void await() {
		mutex.lock();
		try {
			while (!(sent || closed)) {
				cvar.await();
			}
			if (closed) {
				throw new ChannelClosedError();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			mutex.unlock();
		}
	}

	public void receive(final PushBlock pushBlock) throws Error {
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
							pushBlock.yield(object);
							sent = true;
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
					pushBlock.yield(object);
					sent = true;
					cvar.signal();
					if (notifier != null) {
						notifier.notify(this);
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
			if (sent) {
				return;
			}
			closed = true;
			cvar.signalAll();
			if (notifier != null) {
				notifier.notify(this);
			}
		} finally {
			mutex.unlock();
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
