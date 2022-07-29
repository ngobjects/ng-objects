package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGBindingConfigurationException;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGString extends NGDynamicElement {

	/**
	 * The string's value
	 */
	private NGAssociation _valueAss;

	/**
	 * Indicates if we want to escape HTML values. Defaults to true
	 *
	 * FIXME: Keeping the WO defaults. Should we perhaps default to false? PRobably not. Keeping the speculation around though // Hugi 2022-07-29
	 */
	private NGAssociation _escapeHTMLAss;

	/**
	 * A value to display if the string is null or empty (length==0)
	 *
	 * FIXME: I think this binding could be better served as two separate bindings, "valueWhenNull" and "valueWhenBlank" or "valueWhenEmpty". These are separate situations // Hugi 2022-07-09
	 */
	private NGAssociation _valueWhenEmptyAss;

	public NGString( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( null, null, null );
		_valueAss = associations.get( "value" );
		_valueWhenEmptyAss = associations.get( "valueWhenEmpty" );
		_escapeHTMLAss = associations.get( "escapeHTML" );

		if( _valueAss == null ) {
			throw new NGBindingConfigurationException( "[value] binding is required" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		Object value = _valueAss.valueInComponent( context.component() );

		if( value == null ) {
			value = "";
		}

		if( _valueWhenEmptyAss != null ) {
			if( value == null || (value instanceof String s && s.isEmpty()) ) {
				value = _valueWhenEmptyAss.valueInComponent( context.component() );
			}
		}

		String result = value.toString();

		boolean escapeHTML = true;

		if( _escapeHTMLAss != null ) {
			escapeHTML = (boolean)_escapeHTMLAss.valueInComponent( context.component() );
		}

		if( escapeHTML == true ) {
			result = escapeHTML( result );
		}

		response.appendContentString( result );
	}

	/**
	 * @return The string with HTML values escaped
	 *
	 * FIXME: Go over this and add the missing values such as ampersands, quotes etc.
	 */
	private static String escapeHTML( final String string ) {
		return string.replace( "<", "&lt;" ).replace( ">", "&gt;" );
	}
}