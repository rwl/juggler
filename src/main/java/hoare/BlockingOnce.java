package hoare;

import hoare.errors.Rollback;

public class BlockingOnce extends Once {

	private Error rollback_error;

	/*public Error perform() {
	      synchronized (mutex) {
	    	  if (performed) {
	    		  return null, error();
	    	  }

	          try {
	            value      = yield;
	            performed = true;
	            return value, null;
	          } catch (Rollback e) {
	            return null, rollback_error();
	          }
	      }
	}*/

	@Override
	public Object perform(Performable performable) throws Error {
		synchronized (mutex) {
			if (performed) {
				throw error();
			}

			try {
				Object value = performable.perform();
				performed = true;
				return value;
			} catch (Rollback e) {
				throw rollback_error();
			}
		}
	}

	protected Error rollback_error() {
		if (rollback_error == null) {
			rollback_error = new Error("rolled back");
		}
		return rollback_error;
	}
}
