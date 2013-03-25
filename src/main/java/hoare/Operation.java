package hoare;

import java.util.UUID;

interface Operation {

	boolean isClosed();

	void close();

	BlockingOnce getBlockingOnce();

	UUID getUUID();
}
