package ng.appserver;

public abstract class NGAdaptor {

	/**
	 * Start an instance of the adaptor in the given application
	 */
	public abstract void start( NGApplication application );

	/**
	 * FIXME: Added as a placeholder until I decide the best way to pass the request on to the actual request handling mechanism
	 */
	public NGResponse dispatchRequest( final NGRequest request ) {
		return NGApplication.application().dispatchRequest( request );
	}
}