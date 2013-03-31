package juggler;

public class Juggler {

	private Juggler() {
	}

    public interface Consumer<T> {
        public void run(T arg);
    }

    public interface BiConsumer<T, U> {
        public void run(T arg1, U arg2);
    }

    public interface TriConsumer<T, U, V> {
        public void run(T arg1, U arg2, V arg3);
    }

    public interface QuadConsumer<T, U, V, X> {
        public void run(T arg1, U arg2, V arg3, X arg4);
    }

	public static final Thread go(Runnable runnable) {
		Thread th = new Thread(runnable);
        th.run();
        return th;
    }


	public static Thread go(Class<? extends Runnable> class1, Object...args) {
        return null;
	}

    public static <T> Thread go(final Consumer<T> consumer, final T arg) {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                consumer.run(arg);
            }
        });
        th.run();
        return th;
    }

    public static <T, U> Thread go(final BiConsumer<T, U> consumer,
                                   final T arg1, final U arg2) {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                consumer.run(arg1, arg2);
            }
        });
        th.run();
        return th;
    }

    public static <T, U, V> Thread go(final TriConsumer<T, U, V> consumer,
                                      final T arg1, final U arg2, final V arg3) {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                consumer.run(arg1, arg2, arg3);
            }
        });
        th.run();
        return th;
    }

    public static <T, U, V, X> Thread go(final QuadConsumer<T, U, V, X> consumer,
                                      final T arg1, final U arg2, final V arg3, final X arg4) {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                consumer.run(arg1, arg2, arg3, arg4);
            }
        });
        th.run();
        return th;
    }
}
