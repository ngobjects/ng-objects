package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;
import ng.appserver.templating._NGUtilities;

/**
 * FIXME: Work in progress
 */

public class NGConditional extends NGDynamicGroup {

	/**
	 * The condition this conditional evaluates
	 */
	private NGAssociation _conditionAssociation;

	/**
	 * If set to true, will reverse the condition in the "condition" binding. Defaults to false (…of course)
	 */
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
		final Object condition = _conditionAssociation.valueInComponent( context.component() );
		Boolean conditionAsBoolean = _NGUtilities.isTruthy( condition );

		if( _negateAssociation != null ) {
			final Boolean negate = (Boolean)_negateAssociation.valueInComponent( context.component() );

			if( negate == true ) {
				conditionAsBoolean = !conditionAsBoolean;
			}
		}

		if( conditionAsBoolean ) {
			appendChildrenToResponse( response, context );
		}
	}
}