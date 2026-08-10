package ng.dev;

/**
 * Minimal JSON string escaping for the development endpoints ( /eval, /problems and friends ).
 *
 * Deliberately not a JSON library: the dev endpoints emit tiny, fixed-shape documents, and both
 * frameworks that share this code (ng-objects and wonder-slim via Parsley) keep their runtime
 * dependency lists deliberately short — hand-building the JSON with a correct escaper costs a few
 * lines and no dependency. The Parslips dev server takes the same stance on the tooling side.
 */
public class NGDevJson {

	private NGDevJson() {}

	/**
	 * @return the JSON string-escaped form of the given value ( without surrounding quotes )
	 */
	public static String escape( final String value ) {

		if( value == null ) {
			return "";
		}

		final StringBuilder b = new StringBuilder( value.length() + 16 );

		for( int i = 0; i < value.length(); i++ ) {
			final char c = value.charAt( i );

			switch( c ) {
				case '"' -> b.append( "\\\"" );
				case '\\' -> b.append( "\\\\" );
				case '\n' -> b.append( "\\n" );
				case '\r' -> b.append( "\\r" );
				case '\t' -> b.append( "\\t" );
				case '\b' -> b.append( "\\b" );
				case '\f' -> b.append( "\\f" );
				default -> {
					if( c < 0x20 ) {
						b.append( String.format( "\\u%04x", (int)c ) );
					}
					else {
						b.append( c );
					}
				}
			}
		}

		return b.toString();
	}

	/**
	 * @return the value as a JSON string literal ( quoted and escaped ), or the literal {@code null} for a null value
	 */
	public static String str( final String value ) {

		if( value == null ) {
			return "null";
		}

		return "\"" + escape( value ) + "\"";
	}
}
