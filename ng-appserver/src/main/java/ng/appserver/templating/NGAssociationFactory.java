package ng.appserver.templating;

import ng.appserver.NGAssociation;
import ng.appserver.NGBindingAssociation;
import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGKeyValueAssociation;

public class NGAssociationFactory {

	public static NGAssociation associationWithValue( Object obj ) {
		return new NGConstantValueAssociation( obj );
	}

	public static NGAssociation associationWithKeyPath( String keyPath ) {

		if( keyPath.charAt( 0 ) == '^' ) {
			return new NGBindingAssociation( keyPath.substring( 1 ) );
		}

		if( keyPathIsReadOnly( keyPath ) ) {
			throw new RuntimeException( "Read only keypath associations are not supported" );
		}

		return new NGKeyValueAssociation( keyPath );
	}

	/**
	 * FIXME: This represents a keypath that contains an operator (@). We currently don't use that type of keypaths (I don't think they're a good idea) but I'm keeping the functionality around for a bit.
	 */
	private static boolean keyPathIsReadOnly( String keyPath ) {
		return keyPath.startsWith( "@" ) || keyPath.indexOf( ".@" ) > 0;
	}
}