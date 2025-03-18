package ng.xperimental;

import ng.appserver.NGContext;
import ng.appserver.NGSessionRestorationException;
import ng.appserver.templating.NGComponent;

public class NGSessionTimeoutPage extends NGComponent {

	// FIXME: This variable, while set, is never actually used. Not sure we should keep it around // Hugi 2024-03-28
	private NGSessionRestorationException _exception;

	public NGSessionTimeoutPage( NGContext context ) {
		super( context );
	}

	public void setException( NGSessionRestorationException exception ) {
		_exception = exception;
	}
}