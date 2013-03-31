package juggler;

public class Juggler {

	private Juggler() {
	}

	public static final Thread go(Runnable runnable) {
		Thread th = new Thread(runnable);
        th.run();
        return th;
    }


	public static Thread go(Class<? extends Runnable> class1, Object...args) {
        return null;
	}
}
