package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates._NGUtilities;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.assications.NGAssociation;

/**
 * Container element that will only render it's contained content if the binding [condition] evaluates to true.
 */

public class NGConditional extends NGDynamicGroup {

	/**
	 * The condition this conditional evaluates
	 */
	private final NGAssociation _conditionAssociation;

	/**
	 * If set to true, will reverse the condition in the "condition" binding. Defaults to false (…of course)
	 */
	private final NGAssociation _negateAssociation;

	public NGConditional( final String name, final Map<String, NGAssociation> associations, final NGElement content ) {
		super( name, associations, content );
		_conditionAssociation = associations.get( "condition" );
		_negateAssociation = associations.get( "negate" );

		if( _conditionAssociation == null ) {
			throw new NGBindingConfigurationException( "The binding [condition] is required" );
		}
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		if( conditionInContext( context ) ) {
			super.takeValuesFromRequest( request, context );
		}
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		if( conditionInContext( context ) ) {
			return super.invokeAction( request, context );
		}

		return null;
	}

	@Override
	public void appendChildrenToResponse( NGResponse response, NGContext context ) {
		if( conditionInContext( context ) ) {
			super.appendChildrenToResponse( response, context );
		}
	}

	/**
	 * @return The value our condition evaluates to
	 */
	private Boolean conditionInContext( NGContext context ) {
		final Object condition = _conditionAssociation.valueInComponent( context.component() );
		Boolean conditionAsBoolean = _NGUtilities.isTruthy( condition );

		if( _negateAssociation != null ) {
			final Boolean negate = _NGUtilities.isTruthy( _negateAssociation.valueInComponent( context.component() ) );

			if( negate ) {
				conditionAsBoolean = !conditionAsBoolean;
			}
		}

		return conditionAsBoolean;
	}
}