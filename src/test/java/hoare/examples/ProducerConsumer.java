package hoare.examples;

import static hoare.Hoare.go;
import hoare.Channel;

public class ProducerConsumer {

	public static void main(String[] args) {
		final Channel c = new Channel(Integer.class);

		go(new Runnable() {
			@Override
			public void run() {
				int i = 0;
				while (true) {
					c.send(i += 1);
				}
			}
		});

		System.out.println(c.receive());
		System.out.println(c.receive());

		c.close();
	}

}
