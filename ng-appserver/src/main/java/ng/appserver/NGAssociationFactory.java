package ng.appserver;

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
	 * This represents a keypath that contains an operator (@).
	 * Our KVC implementation currently does not support operators but I'm keeping the functionality around for a bit while I mull this over
	 */
	private static boolean keyPathIsReadOnly( String keyPath ) {
		return keyPath.startsWith( "@" ) || keyPath.indexOf( ".@" ) > 0;
	}
}