package hoare;

import hoare.Once.Performable;
import hoare.errors.ChannelClosedError;
import hoare.errors.Rollback;

import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Pop implements Operation {

	private UUID uuid;
	private BlockingOnce blocking_once;
	private Notifier notifier;
	private Object object;

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

	public void send() {
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
//							object = Marshal.load(yield);
							received = true;
							cvar.signal();
							if (notifier != null) {
								notifier.notify(this);
							}

								return object;
						}
					});
				} catch (Error error) {
//					return error;
				}
			} else {
				try {
//					this.object = Marshal.load(yield);
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
}
