package hoare;

class Once {

	public static final String ERROR_MSG = "already performed";

	public interface Performable {
		public Object perform();
	}

	protected Object mutex;
	protected boolean performed;
	protected Error error;

	public Once() {
		mutex = new Object();
		performed = false;
	}

    /*public Error perform() {
      // optimium path
    	if (performed) {
    		return null, error();
    	}

      // slow path
      synchronized (mutex) {
        if (performed) {
        	return null, error();
        }
        performed = true;
      }

      return yield, null;
    }*/

	public Object perform(Performable performable) throws Error {
		// optimium path
		if (performed) {
			throw error();
		}

		// slow path
		synchronized (mutex) {
			if (performed) {
				throw error();
			}
			performed = true;
		}

		return performable.perform();
	}

	public boolean performed() {
		return performed;
	}

	protected Error error() {
		if (error == null) {
			error = new Error(ERROR_MSG);
		}
		return error;
	}
}
