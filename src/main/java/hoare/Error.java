package hoare;

public class Error extends java.lang.Error {

	private static final long serialVersionUID = 7443775691909110183L;

	private final String message;

	public Error(String message) {
		this.message = message;
	}

	public String toString() {
		return message;
	}

	public boolean isMessage(String message) {
		return this.message.equals(message);
	}

}
