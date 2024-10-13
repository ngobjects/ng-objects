package ng.appserver;

public class NGAssociationFactory {

	public static NGAssociation constantValueAssociationWithValue( Object obj ) {
		return new NGConstantValueAssociation( obj );
	}

	public static NGAssociation associationWithKeyPath( String keyPath ) {

		if( keyPath.charAt( 0 ) == '^' ) {
			return new NGBindingAssociation( keyPath.substring( 1 ) );
		}

		return new NGKeyValueAssociation( keyPath );
	}
}