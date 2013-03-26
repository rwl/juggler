package juggler;

import java.util.UUID;

interface Operation<T> {

	boolean isClosed();

	void close();

	BlockingOnce getBlockingOnce();

	UUID getUUID();
}
