package ng.appserver;

public class NGForwardException extends RuntimeException {

	public NGForwardException( Exception e ) {
		super( e );
	}

	/**
	 * FIXME: Arded as an experiment. getCause() is not neccessarily the right way to go here.
	 */
	public static Throwable _originalThrowable( Throwable exception ) {
		Throwable result = exception;

		while( result.getCause() != null ) {
			result = result.getCause();
		}

		return result;
	}
}