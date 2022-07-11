package ng;

/**
 * Exactly the same as RuntimeException, but provides an HTML version of an exception message as well
 */

public class NGRuntimeException extends RuntimeException {

	private String _htmlMessage;

	public NGRuntimeException( String message ) {
		super( message );
	}

	public NGRuntimeException( String message, String htmlMessage ) {
		this( message );
		_htmlMessage = htmlMessage;
	}

	public String htmlMessage() {

		if( _htmlMessage == null ) {
			return getMessage();
		}

		return _htmlMessage;
	}
}