package juggler;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Notifier {

	private Object payload;

	private Lock lock;
	private Condition cvar;
	private boolean notified;

	public Notifier() {
		lock = new ReentrantLock();
		cvar = lock.newCondition();
		notified = false;
		payload = null;
	}

	public boolean isNotified() {
		return notified;
	}

	public void await() {
		lock.lock();
		try {
			while (!notified) {
				cvar.await();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			lock.unlock();
		}
	}

	public Error notify(Object payload) {
		lock.lock();
		try {
			if (notified) {
				return new Error("already notified");
			}
			this.payload = payload;
			notified = true;
			cvar.signal();
			return null;
		} finally {
			lock.unlock();
		}
	}

	public Object getPayload() {
		return payload;
	}
}
