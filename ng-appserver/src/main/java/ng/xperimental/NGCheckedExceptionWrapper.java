package ng.xperimental;

/**
 * Primarily intended for wrapping checked exceptions thrown during the rendering process.
 * We need to be able to allow to throw that exception so it can pass up the stack for display in the UI.
 */

public class NGCheckedExceptionWrapper extends RuntimeException {

	public NGCheckedExceptionWrapper( Throwable e ) {
		super( e );
	}
}