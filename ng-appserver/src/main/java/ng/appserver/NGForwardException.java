package ng.appserver;

/**
 * FIXME: Would it be a good idea to allow the user to specify a status code along with the exception thrown or even a component name/custom response?
 */

public class NGForwardException extends RuntimeException {

	public NGForwardException( Exception e ) {
		super( e );
	}
}