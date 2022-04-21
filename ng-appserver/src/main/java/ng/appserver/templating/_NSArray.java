package ng.appserver.templating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

public class _NSArray extends ArrayList {

	public _NSArray() {}

	public _NSArray( int size ) {
		super( size );
	}

	public _NSArray( Collection collection ) {
		super( collection );
	}

	public Enumeration objectEnumerator() {
		return Collections.enumeration( this );
	}

	public static _NSArray componentsSeparatedByString( String trimmedDeclarationBody, String string ) {
		return new _NSArray( Arrays.asList( trimmedDeclarationBody.split( string ) ) );
	}
}