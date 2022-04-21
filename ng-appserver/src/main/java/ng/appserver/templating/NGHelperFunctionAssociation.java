package ng.appserver.templating;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;

/**
 * Factory methods for creating WOAssociations (ripped from WOAssociation)
 *
 * @author mschrag
 */
public class NGHelperFunctionAssociation {

	private static boolean _keyPathIsReadOnly( String keyPath ) {
		return keyPath.startsWith( "@" ) || keyPath.indexOf( ".@" ) > 0;
	}

	public static NGAssociation associationWithValue( Object obj ) {
		return new NGConstantValueAssociation( obj );
	}

	public static NGAssociation associationWithKeyPath( String keyPath ) {
		NGAssociation association;

		if( keyPath.charAt( 0 ) == '^' ) {
			//			association = new NGHelperFunctionBindingNameAssociation( keyPath );
			throw new RuntimeException( "Binding name associations are not supported" );
		}
		else if( _keyPathIsReadOnly( keyPath ) ) {
			throw new RuntimeException( "Read only keypath associations are not supported" );
			/*
			association = new NGReadOnlyKeyValueAssociation( keyPath );
			*/
		}
		else {
			association = new NGHelperFunctionKeyValueAssociation( keyPath );
		}

		return association;
	}
}
