package ng.appserver.templating;

import com.webobjects.appserver.WOElement;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.kvc.NGKeyValueCoding.UnknownKeyException;
import ng.xperimental.NGErrorMessageElement;

public interface NGElement extends WOElement {

	public default void takeValuesFromRequest( NGRequest request, NGContext context ) {}

	public default NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return null;
	}

	public default void appendToResponse( NGResponse response, NGContext context ) {}

	public default void appendOrTraverse( NGResponse response, NGContext context ) {
		if( context.shouldAppendToResponse() ) {
			try {
				appendToResponse( response, context );
			}
			catch( UnknownKeyException unknownKeyException ) {
				new NGErrorMessageElement( "VOFF! VOFF! Unknown key", getClass().getSimpleName(), unknownKeyException.getMessage() ).appendToResponse( response, context );
			}
		}
		else if( this instanceof NGStructuralElement se ) {
			se.appendStructureToResponse( response, context );
		}
	}
}