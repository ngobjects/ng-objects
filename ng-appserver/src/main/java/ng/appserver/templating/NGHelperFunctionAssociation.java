package ng.appserver.templating;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;

public class NGHelperFunctionAssociation {

	private static boolean _keyPathIsReadOnly( String keyPath ) {
		return keyPath.startsWith( "@" ) || keyPath.indexOf( ".@" ) > 0;
	}

	public static NGAssociation associationWithValue( Object obj ) {
		return new NGConstantValueAssociation( obj );
	}

	public static NGAssociation associationWithKeyPath( String keyPath ) {

		if( keyPath.charAt( 0 ) == '^' ) {
			// return new NGHelperFunctionBindingNameAssociation( keyPath );
			throw new RuntimeException( "Binding name associations are not supported" );
		}

		if( _keyPathIsReadOnly( keyPath ) ) {
			// return new NGReadOnlyKeyValueAssociation( keyPath );
			throw new RuntimeException( "Read only keypath associations are not supported" );
		}

		return new NGHelperFunctionKeyValueAssociation( keyPath );
	}
}