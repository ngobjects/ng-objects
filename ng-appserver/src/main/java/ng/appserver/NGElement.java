package ng.appserver;

public interface NGElement {

	public default void takeValuesFromRequest( NGRequest request, NGContext context ) {}

	public default NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return null;
	}

	public default void appendToResponse( NGResponse response, NGContext context ) {}
}