package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

/**
 * FIXME: Work in progress
 */

public class NGConditional extends NGDynamicGroup {

	private NGAssociation _conditionAssociation;
	private NGAssociation _negateAssociation;

	public NGConditional( final String name, final Map<String, NGAssociation> associations, final NGElement content ) {
		super( name, associations, content );
		_conditionAssociation = associations.get( "condition" );
		_negateAssociation = associations.get( "negate" );

		if( _conditionAssociation == null ) {
			// FIXME: We should probably have an exception class for missing bindings, IllegalArgumentException isn't really nice // Hugi 2022-06-05
			throw new IllegalArgumentException( "The binding [condition] is required" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		Boolean condition = (Boolean)_conditionAssociation.valueInComponent( context.component() );

		if( _negateAssociation != null ) {
			final Boolean negate = (Boolean)_negateAssociation.valueInComponent( context.component() );

			if( negate == true ) {
				condition = !condition;
			}
		}

		if( condition ) {
			appendChildrenToResponse( response, context );
		}
	}
}