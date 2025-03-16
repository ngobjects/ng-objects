package ng.appserver.elements;

import java.text.Format;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGCheckedExceptionWrapper;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;
import ng.appserver.templating.assications.NGAssociationUtils;

public class NGTextField extends NGDynamicElement {

	/**
	 * 'name' attribute of the text field. If not specified, will be populated using the elementID
	 */
	private final NGAssociation _nameAssociation;

	/**
	 * The value for the field. This is a bidirectional binding that will also pass the value upstrem.
	 */
	private final NGAssociation _valueAssociation;

	/**
	 * Indicates that the text field is disabled.
	 *
	 * Both actually disables the text fields, and prevents values from it from being read.
	 */
	private final NGAssociation _disabledAssociation;

	/**
	 * Allows you to pass in a java.text.Formatter instance to perform formatting on the entered value
	 *
	 * FIXME:
	 * We should probably seriously consider passing in our own formatter class here, since the java formatters su... uh,
	 * aren't that nice (thread safety, null problems, date formatters don't handle java.time etc.)
	 *
	 * See comment on the corresponding binding on NGString
	 * // Hugi 2023-04-15
	 */
	private final NGAssociation _formatterAssociation;

	/**
	 * Pass-through attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGTextField( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_additionalAssociations = new HashMap<>( associations );
		_nameAssociation = _additionalAssociations.remove( "name" );
		_valueAssociation = _additionalAssociations.remove( "value" );
		_disabledAssociation = _additionalAssociations.remove( "disabled" );
		_formatterAssociation = _additionalAssociations.remove( "formatter" );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		if( !disabled( context ) ) {
			final String name = name( context );
			final List<String> valuesFromRequest = request.formValuesForKey( name );

			if( !valuesFromRequest.isEmpty() ) {

				// If multiple form values are present for the same field name, the potential for an error condition is probably high enough to just go ahead and fail.
				if( valuesFromRequest.size() > 1 ) {
					throw new IllegalStateException( "The request contains %s form values named '%s'. I can only handle one at a time. The values you sent me are (%s).".formatted( valuesFromRequest.size(), name, valuesFromRequest ) );
				}

				Object value = null; // We're sticking with null as the default for an empty string to align with WO behaviour. This might have to be revisited in the future.

				final String stringValueFromRequest = valuesFromRequest.get( 0 );

				if( !stringValueFromRequest.isEmpty() ) {
					if( _formatterAssociation != null ) {
						// If a formatter is present, we make a formatting attempt here
						final Format formatter = (Format)_formatterAssociation.valueInComponent( context.component() );

						try {
							value = formatter.parseObject( stringValueFromRequest );
						}
						catch( ParseException e ) {
							throw new NGCheckedExceptionWrapper( e );
						}
					}
					else {
						value = stringValueFromRequest;
					}
				}

				_valueAssociation.setValue( value, context.component() );
			}
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		final Map<String, String> attributes = new HashMap<>();

		attributes.put( "type", "text" );
		attributes.put( "name", name( context ) );

		Object objectValue = _valueAssociation.valueInComponent( context.component() );

		final String stringValue;

		if( _formatterAssociation != null ) {
			final Format formatter = (Format)_formatterAssociation.valueInComponent( context.component() );
			stringValue = formatter.format( objectValue );
		}
		else {
			if( objectValue != null ) {
				stringValue = objectValue.toString();
			}
			else {
				stringValue = null;
			}
		}

		if( stringValue != null ) {
			attributes.put( "value", stringValue );
		}

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		if( disabled( context ) ) {
			// CHECKME: 'disabled' is a "boolean attribute" and doesn't really need a value. We need a nice way to generate those // Hugi 2023-03-11
			attributes.put( "disabled", "" );
		}

		final String tagString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( tagString );
	}

	/**
	 * @return True if the field is disabled
	 */
	private boolean disabled( final NGContext context ) {
		if( _disabledAssociation != null ) {
			return NGAssociationUtils.isTruthy( _disabledAssociation.valueInComponent( context.component() ) );
		}

		return false;
	}

	/**
	 * @return The name of the field (to use in the HTML code)
	 */
	private String name( final NGContext context ) {

		if( _nameAssociation != null ) {
			return (String)_nameAssociation.valueInComponent( context.component() );
		}

		return context.elementID().toString();
	}
}