package ng.appserver.templating.elements;

import java.text.Format;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;

public class NGString extends NGDynamicElement {

	/**
	 * The string's value
	 */
	private final NGAssociation _valueAssociation;

	/**
	 * A value to display if the string is null or empty (length==0)
	 */
	private final NGAssociation _valueWhenEmptyAssociation;

	/**
	 * A java formatter used to format the displayed value
	 *
	 * FIXME:
	 * Instead of accepting a java.text.Format here we need to handle this in a way that accepts other formatter types,
	 * java.time.format.DateTimeFormatter being the most obvious example.
	 *
	 * Our best course of action is probably to introduce our own formatter object that can wrap other formatters,
	 * along with a "formatter wrapping shop" object that looks up and wraps different formatter types.
	 *
	 * It should probably also be the role of that "wrapper" factory to cache formatters for reuse, in case we decide to create formatters ourselves based on
	 * date/number format strings, like WOString does. Whether or not to do that is a different question (although it's probably sensible WRT porting older templates).
	 * // Hugi-2024-08-14
	 *
	 */
	private final NGAssociation _formatterAssociation;

	/**
	 * Indicates if we want to escape HTML values. Defaults to true
	 */
	private final NGAssociation _escapeHTMLAssociation;

	public NGString( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( null, null, null );
		_valueAssociation = associations.get( "value" );
		_valueWhenEmptyAssociation = associations.get( "valueWhenEmpty" );
		_escapeHTMLAssociation = associations.get( "escapeHTML" );
		_formatterAssociation = associations.get( "formatter" );

		if( _valueAssociation == null ) {
			throw new NGBindingConfigurationException( "[value] binding is required" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		Object objectValue = _valueAssociation.valueInComponent( context.component() );

		// Once we add support for formatters, this might be worth revisiting.
		// I.e. an object might not be null, but it's formatted representation might be an empty string.
		if( _valueWhenEmptyAssociation != null ) {
			if( objectValue == null || (objectValue instanceof String s && s.isEmpty()) ) {
				objectValue = _valueWhenEmptyAssociation.valueInComponent( context.component() );
			}
		}

		// If objectValue is null, we don't do anything at all. Otherwise, we proceed to do some rendering.
		if( objectValue != null ) {
			boolean escapeHTML = true;

			if( _escapeHTMLAssociation != null ) {
				escapeHTML = (boolean)_escapeHTMLAssociation.valueInComponent( context.component() );
			}

			String string;

			if( _formatterAssociation != null ) {
				final Format formatter = (Format)_formatterAssociation.valueInComponent( context.component() );
				string = formatter.format( objectValue );
			}
			else {
				string = objectValue.toString();
			}

			if( escapeHTML ) {
				string = NGHTMLUtilities.escapeHTML( string );
			}

			response.appendContentString( string );
		}
	}

	@Override
	public String toString() {
		return "NGString [_valueAss=" + _valueAssociation + ", _escapeHTMLAss=" + _escapeHTMLAssociation + ", _valueWhenEmptyAss=" + _valueWhenEmptyAssociation + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash( _escapeHTMLAssociation, _valueAssociation, _valueWhenEmptyAssociation );
	}

	@Override
	public boolean equals( Object obj ) {
		if( this == obj ) {
			return true;
		}
		if( obj == null ) {
			return false;
		}
		if( getClass() != obj.getClass() ) {
			return false;
		}
		NGString other = (NGString)obj;
		return Objects.equals( _escapeHTMLAssociation, other._escapeHTMLAssociation ) && Objects.equals( _valueAssociation, other._valueAssociation ) && Objects.equals( _valueWhenEmptyAssociation, other._valueWhenEmptyAssociation );
	}
}