package hoare;

import static hoare.Hoare.go;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hoare.BlockingOnce;
import hoare.Channel;
import hoare.Error;
import hoare.Once;
import hoare.Once.Performable;
import hoare.errors.Rollback;
import junit.framework.TestCase;

public class BlockingOnceTest extends TestCase {

	BlockingOnce blocking_once;

	protected void setUp() throws Exception {
	}

	/**
	 * should execute the block passed to it
	 */
	public void testPerform() {
		final List<Integer> r = new ArrayList<Integer>();

		blocking_once.perform(new Performable() {
			@Override
			public Object perform() {
				r.add(1);
				return null;
			}
		});

		assertEquals(1, r.size());
		assertTrue(r.get(0) == 1);
	}

	/**
	 * should only execute the first block passed to it
	 */
	public void testPerform2() {
		final List<Integer> r = new ArrayList<Integer>();

		blocking_once.perform(new Performable() {
			@Override
			public Object perform() {
				r.add(1);
				return null;
			}
		});

		blocking_once.perform(new Performable() {
			@Override
			public Object perform() {
				r.add(2);
				return null;
			}
		});

		assertEquals(1, r.size());
		assertTrue(r.get(0) == 1);
	}

	/**
	 * should return the value returned from the block
	 */
	public void testPerformReturn() {
		Object value = blocking_once.perform(new Performable() {
			@Override
			public Object perform() {
				return 1;
			}
		});

		assertEquals(new Integer(1), value);
	}

	/**
	 * should return nil for value and an error if it has already been used
	 */
	public void testPerformError() {
		Object value = null;
		Error error = null;
		try {
			value = blocking_once.perform(new Performable() {
				@Override
				public Object perform() {
					return 1;
				}
			});
		} catch (Error e) {
			error = e;
		}

		assertEquals(new Integer(1), value);
		assertNull(error);

		try {
			value = blocking_once.perform(new Performable() {
				@Override
				public Object perform() {
					return 2;
				}
			});
		} catch (Error e) {
			error = e;
		}

		assertNull(value);
		assertNotNull(error);
		assertTrue(error.isMessage(Once.ERROR_MSG));
	}

	/**
	 * should roll back and allow the block to be executed again
	 */
	public void testRollback() {
		long s = System.currentTimeMillis();

		final Channel finished_channel = new Channel(Boolean.class, 2);

		go(new Runnable() {
			@Override
			public void run() {
				blocking_once.perform(new Performable() {
					@Override
					public Object perform() {
						Thread.sleep(100);
						finished_channel.send(true);
						throw new Rollback();
						return null;
					}
				});
			}
		});

		Thread.sleep(100); // make sure the first blocking_once calls #perform

		go(new Runnable() {
			@Override
			public void run() {
				blocking_once.perform(new Performable() {
					@Override
					public Object perform() {
						Thread.sleep(100);
						finished_channel.send(true);
						return null;
					}
				});
			}
		});

		finished_channel.receive();
		finished_channel.receive();

		finished_channel.close();

		// Three sleeps at 0.1 == 0.3, so if it's less than 0.3...
		assertTrue(System.currentTimeMillis() - s < 3);
	}

	/**
	 * should have minimal contention between threads when they contend for
	 * position
	 */
	public void testContention() {
		final List<Integer> r = new ArrayList<Integer>()
		long s = System.currentTimeMillis();

	    // Using condition variables to maximize potential contention
	    final Lock mutex = new ReentrantLock();
	    final Condition condition = mutex.newCondition();

	    final Channel waiting_channel  = new Channel(Boolean.class, 2);
	    final Channel finished_channel = new Channel(Boolean.class, 2);

	    go(new Runnable() {
			@Override
			public void run() {
				mutex.lock();
				try {
			      waiting_channel.send(true);
			      condition.await();
			      blocking_once.perform(new Performable() {
					@Override
					public Object perform() {
						Thread.sleep(100);
						r.add(1);
						return null;
					}
				});
			      finished_channel.send(true);
				} finally {
					mutex.unlock();
				}
			}
		});

	    go(new Runnable() {
			public void run() {
				mutex.lock();
				try {
					waiting_channel.send(true);
					condition.await();
					blocking_once.perform(new Performable() {
						@Override
						public Object perform() {
							Thread.sleep(100);
							r.add(1);
							return null;
						}
					});
					finished_channel.send(true);
				} finally {
					mutex.unlock();
				}
			}
		});

	    // wait for both the goroutines to be waiting
	    waiting_channel.receive();
	    waiting_channel.receive();

	    mutex.lock();
	    try {
	    	condition.signalAll();
	    } finally {
	    	mutex.unlock();
	    }

	    // wait for the finished channel to be completed
	    finished_channel.receive();
	    finished_channel.receive();

	    assertTrue(r.size() == 1);
	    // Only the first sleep should be performed, so things should quickly
	    long diff = System.currentTimeMillis() - s;
	    assertTrue(diff > 50);
	    assertTrue(diff < 150);

	    waiting_channel.close();
	    finished_channel.close();
	}
}
