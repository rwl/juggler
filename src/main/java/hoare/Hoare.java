package hoare;

public class Hoare {

	private Hoare() {
	}

	public static final void go(Runnable runnable) {
		new Thread(runnable).run();
	}
}
