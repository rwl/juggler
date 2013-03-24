package hoare;

import java.util.UUID;

public class Push implements Operation {

	private UUID uuid;
	private BlockingOnce blocking_once;
	private Notifier notifier;
	private Object object;

	private Object mutex;
	private ConditionVariable cvar;
	private boolean sent;
	private boolean closed;

	public Push(Object object, UUID uuid, BlockingOnce blocking_once, Notifier notifier) {
		this.object = Marshal.dump(object);
		this.uuid = uuid == null ? UUID.randomUUID() : uuid;
		this.blocking_once = blocking_once;
		this.notifier = notifier;
		this.mutex = new Object();
		this.cvar = new ConditionVariable();
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

    public void wait() {
      synchronized (mutex) {
        while (!(sent || closed)) {
          cvar.wait(mutex);
        }
        if (closed) {
        	throw new ChannelClosedError();
        }
      }
    }

    public void receive() {
      synchronized (mutex) {
        if (closed) {
        	throw new ChannelClosedError();
        }

        if (blocking_once != null) {
          _, error = blocking_once.perform() {
            yield object;
            sent = true;
            cvar.signal();
            if (notifier != null) {
            	notifier.notify(self);
            }
          }

          return error;
        } else {
          try {
            yield object;
            sent = true;
            cvar.signal();
            if (notifier != null) {
            	notifier.notify(self);
            }
          } catch (RollbackError e) {
          }
        }
      }
    }

	@Override
	public void close() {
		synchronized (mutex) {
			if (sent) {
				return;
			}
			closed = true;
			cvar.broadcast();
			if (notifier != null) {
				notifier.notify(this);
			}
		}
	}

}
