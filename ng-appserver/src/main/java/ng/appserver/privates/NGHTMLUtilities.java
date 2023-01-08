package ng.appserver.privates;

import java.util.Map;
import java.util.Objects;

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
	 * @return The string with HTML values escaped
	 */
	public static String escapeHTML( final String string ) {
		Objects.requireNonNull( string );

		return string
				.replace( "<", "&lt;" )
				.replace( ">", "&gt;" )
				.replace( "&", "&amp;" )
				.replace( "\"", "&quot;" )
				.replace( "'", "&#39;" );
	}
}