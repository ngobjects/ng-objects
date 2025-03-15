package ng.appserver.templating;

import com.webobjects.appserver.WOElement;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public interface NGElement extends WOElement {

	public default void takeValuesFromRequest( NGRequest request, NGContext context ) {}

	public default NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return null;
	}

	public default void appendToResponse( NGResponse response, NGContext context ) {}
}