package hoare;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hoare.errors.NegativeWaitGroupCountError;

public class WaitGroup {

	private int count;

	private Lock lock;
	private Condition cvar;

	public WaitGroup() {
		count = 0;
		lock = new ReentrantLock();
		cvar = lock.newCondition();
	}

	public void await() {
		lock.lock();
		try {
			while (count > 0) {
				cvar.await();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			lock.unlock();
		}
	}

	public void add(int delta) {
		lock.lock();
		try {
			modify_count(delta);
		} finally {
			lock.unlock();
		}
	}

	public void done() {
		lock.lock();
		try {
			modify_count(-1);
		} finally {
			lock.unlock();
		}
	}

	// Expects to be called while locked
	protected void modify_count(int delta) {
		count += delta;
		if (count < 0) {
			throw new NegativeWaitGroupCountError();
		}
		if (count == 0) {
			cvar.signal();
		}
	}

	public int getCount() {
		return count;
	}
}
