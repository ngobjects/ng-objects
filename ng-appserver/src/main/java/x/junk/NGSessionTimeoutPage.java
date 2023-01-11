package x.junk;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGSessionRestorationException;

public class NGSessionTimeoutPage extends NGComponent {

	private NGSessionRestorationException _exception;

	public NGSessionTimeoutPage( NGContext context ) {
		super( context );
	}

	public void setException( NGSessionRestorationException exception ) {
		_exception = exception;
	}
}