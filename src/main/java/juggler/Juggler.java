package juggler;

public class Juggler {

	private Juggler() {
	}

	public static final void go(Runnable runnable) {
		new Thread(runnable).run();
	}


	public static void go(Class<? extends Runnable> class1, Channel<?> clientRequests) {

	}
}
