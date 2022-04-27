package ng.appserver.templating;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGKeyValueAssociation;

public class NGAssociationFactory {

	public static NGAssociation associationWithValue( Object obj ) {
		return new NGConstantValueAssociation( obj );
	}

	public static NGAssociation associationWithKeyPath( String keyPath ) {

		if( keyPath.charAt( 0 ) == '^' ) {
			throw new RuntimeException( "Binding name associations are not supported" );
		}

		if( keyPathIsReadOnly( keyPath ) ) {
			throw new RuntimeException( "Read only keypath associations are not supported" );
		}

		return new NGKeyValueAssociation( keyPath );
	}

	private static boolean keyPathIsReadOnly( String keyPath ) {
		return keyPath.startsWith( "@" ) || keyPath.indexOf( ".@" ) > 0;
	}
}