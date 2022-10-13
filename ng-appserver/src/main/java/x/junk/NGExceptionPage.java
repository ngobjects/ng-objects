package x.junk;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;

/**
 * Exception page used when not in development mode
 */

public class NGExceptionPage extends NGComponent {

	/**
	 * The exception we're reporting.
	 */
	private Throwable _exception;

	public NGExceptionPage( NGContext context ) {
		super( context );
	}

	public Throwable exception() {
		return _exception;
	}

	public void setException( Throwable value ) {
		_exception = value;
	}
}