package ng.appserver.privates;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;
import ng.appserver.templating.associations.NGAssociation;

public class NGHTMLUtilities {

	private static final Logger logger = LoggerFactory.getLogger( NGHTMLUtilities.class );

	public static String createElementStringWithAttributes( final String elementName, final Map<String, String> attributes, boolean close ) {
		Objects.requireNonNull( elementName );
		Objects.requireNonNull( attributes );

		StringBuilder b = new StringBuilder();

		b.append( "<" );
		b.append( elementName );

		attributes.forEach( ( name, value ) -> {
			if( value != null ) {
				b.append( " " );
				b.append( name );
				b.append( "=" );
				b.append( "\"" + value + "\"" );
			}
		} );

		if( close ) {
			b.append( "/" );
		}

		b.append( ">" );

		return b.toString();
	}

	/**
	 * Goes through each key in [associations], read their value in [component] and add the resulting value to [attributes]
	 *
	 * CHECKME: This thing sucks (in-place mutation of an existing map, not really the right utility class etc) but at least it beats coding each case // Hugi 2023-04-15
	 */
	public static void addAssociationValuesToAttributes( final Map<String, String> attributes, final Map<String, NGAssociation> associations, final NGComponent component ) {
		Objects.requireNonNull( attributes );
		Objects.requireNonNull( associations );
		Objects.requireNonNull( component );

		for( Entry<String, NGAssociation> entry : associations.entrySet() ) {
			final Object value = entry.getValue().valueInComponent( component );

			if( value != null ) {
				attributes.put( entry.getKey(), value.toString() );
			}
		}
	}

	/**
	 * @return The string with HTML values escaped
	 */
	public static String escapeHTML( final String string ) {
		Objects.requireNonNull( string );

		return string
				.replace( "&", "&amp;" )
				.replace( "<", "&lt;" )
				.replace( ">", "&gt;" )
				.replace( "\"", "&quot;" )
				.replace( "'", "&#39;" );
	}

	/**
	 * @return The namespace association from the given association map.
	 *
	 * FIXME:
	 * Temp method for checking for the old style "framework" binding that will inevitably be present while old templates are being ported from WO.
	 * Represents a missing feature. We need a better way to map and document element API structure in source to allow for binding deprecation of this kind.
	 * Oh, and allow for multiple binding names that map to the same association. And allow for binding deprecation levels ("warn", "fail" etc.)
	 * Hugi 2024-10-11
	 */
	public static NGAssociation namespaceAssociation( final Map<String, NGAssociation> associations, boolean removeAssociation ) {
		Objects.requireNonNull( associations );

		NGAssociation a = removeAssociation ? associations.remove( "namespace" ) : associations.get( "namespace" );

		if( a != null ) {
			return a;
		}

		a = removeAssociation ? associations.remove( "framework" ) : associations.get( "framework" );

		if( a != null ) {
			logger.warn( "Found [framework] binding. You should be using [namespace]" );
		}

		return null;
	}

	/**
	 * @return The value of the namespace association in the given context.
	 *
	 * FIXME: Yet another temp fix that represents a couple of missing features:
	 *
	 * - Default association values
	 * - Reusable/Global/Shared associations, i.e. associations that behave the same in different elements. Many elements have the same or similar attributes or attribute combinations, an example being elements that reference resources (using [filename], [namespace], [data] etc.)
	 *
	 *  Hugi 20214-10-11
	 */
	public static String namespaceInContext( final NGContext context, final NGAssociation namespaceAssociation ) {

		// [namespace] is not bound. Use default namespace
		if( namespaceAssociation == null ) {
			return "app";
		}

		return (String)namespaceAssociation.valueInComponent( context.component() );
	}
}