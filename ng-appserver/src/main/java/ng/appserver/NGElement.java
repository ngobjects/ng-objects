package ng.appserver;

import com.webobjects.appserver.WOElement;

public interface NGElement extends WOElement {

	public default void takeValuesFromRequest( NGRequest request, NGContext context ) {}

	public default NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return null;
	}

	public default void appendToResponse( NGResponse response, NGContext context ) {}
}