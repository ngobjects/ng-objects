package ng.appserver;

public abstract class NGAdaptor {

	public abstract void start();

	/**
	 * FIXME: Added as a placeholder until I decide the best way to pass the request on to the actual request handling mechanism
	 */
	public NGResponse dispatchRequest( final NGRequest request ) {
		return NGApplication.application().dispatchRequest( request );
	}
}