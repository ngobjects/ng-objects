package ng.appserver.templating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

public class NSArray extends ArrayList {

	public NSArray() {}

	public NSArray( int size ) {
		super( size );
	}

	public NSArray( Collection collection ) {
		super( collection );
	}

	public Enumeration objectEnumerator() {
		return Collections.enumeration( this );
	}

	public int count() {
		return size();
	}

	public static NSArray componentsSeparatedByString( String trimmedDeclarationBody, String string ) {
		return new NSArray( Arrays.asList( trimmedDeclarationBody.split( string ) ) );
	}
}