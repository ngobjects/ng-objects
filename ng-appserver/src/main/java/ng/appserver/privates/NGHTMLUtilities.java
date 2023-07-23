package ng.appserver.privates;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;

public class NGHTMLUtilities {

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
}