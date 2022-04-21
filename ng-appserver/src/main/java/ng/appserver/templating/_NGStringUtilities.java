package ng.appserver.templating;

public class _NGStringUtilities {

	public static String stringFromBuffer( StringBuffer buffer ) {
		int len = buffer.length();
		return len == 0 ? "" : buffer.toString();
	}

	public static String replaceAllInstancesOfString( String s1, String s2, String s3 ) {
		return s1.replace( s2, s3 );
	}

	public static boolean isNumber( String string ) {
		int length = string.length();
		if( length == 0 ) {
			return false;
		}

		boolean dot = false;
		int i = 0;
		char character = string.charAt( 0 );
		if( (character == '-') || (character == '+') ) {
			i = 1;
		}
		else if( character == '.' ) {
			i = 1;
			dot = true;
		}

		while( i < length ) {
			character = string.charAt( i++ );
			if( character == '.' ) {
				if( dot ) {
					return false;
				}
				dot = true;
			}
			else if( !(Character.isDigit( character )) ) {
				return false;
			}
		}
		return true;
	}
}