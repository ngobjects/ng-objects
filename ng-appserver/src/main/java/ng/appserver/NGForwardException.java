package ng.appserver;

public class NGForwardException extends RuntimeException {

	public NGForwardException( Exception e ) {
		super( e );
	}
}