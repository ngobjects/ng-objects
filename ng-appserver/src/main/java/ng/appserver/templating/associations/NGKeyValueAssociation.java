package ng.appserver.templating.associations;

import java.util.Objects;

import ng.appserver.templating.NGComponent;
import ng.kvc.NGKeyValueCodingAdditions;

public class NGKeyValueAssociation extends NGAssociation {

	private final String _keyPath;

	public NGKeyValueAssociation( final String keyPath ) {
		validateKeyPath( keyPath );
		_keyPath = keyPath;
	}

	/**
	 * FIXME: We should be conducting a more thorough syntax check for every "disallowed" character // Hugi 2024-06-25
	 * FIXME: This check really belongs with KVC, not the association // Hugi 2024-07-16
	 * FIXME: KeyPath validation can really be dependent on the targeted object, so we might have to delegate this based on a keyPath element's capabilities (for example, a String key in a map could be whatever) // Hugi 2024-09-21
	 */
	private static void validateKeyPath( final String keyPath ) {
		if( keyPath == null ) {
			throw new NGAssociationConstructionException( "[keyPath] can't be null" );
		}

		if( keyPath.isEmpty() ) {
			throw new NGAssociationConstructionException( "[keyPath] can't be an empty string" );
		}

		if( keyPath.startsWith( "." ) ) {
			throw new NGAssociationConstructionException( "[keyPath] can't start with a period." );
		}

		if( keyPath.endsWith( "." ) ) {
			throw new NGAssociationConstructionException( "[keyPath] can't end with a period." );
		}

		if( keyPath.contains( ".@" ) ) {
			throw new NGAssociationConstructionException( "[keyPath] doesn't support operators (keys prefixed with '@')" );
		}

		if( keyPath.contains( ". " ) ) {
			throw new NGAssociationConstructionException( "[keyPath] has an element that starts with a space" );
		}

		if( keyPath.contains( " ." ) ) {
			throw new NGAssociationConstructionException( "[keyPath] has an element that ends with a space" );
		}

		if( keyPath.startsWith( " " ) ) {
			throw new NGAssociationConstructionException( "[keyPath] can't start with a space." );
		}

		if( keyPath.endsWith( " " ) ) {
			throw new NGAssociationConstructionException( "[keyPath] can't end with a space." );
		}

		if( keyPath.contains( ".." ) ) {
			throw new NGAssociationConstructionException( "[keyPath] can't contain two (or more) consecutive periods" );
		}
	}

	@Override
	public Object valueInComponent( final NGComponent component ) {
		Objects.requireNonNull( component );
		return NGKeyValueCodingAdditions.Utility.valueForKeyPath( component, keyPath() );
	}

	@Override
	public void setValue( final Object value, final NGComponent component ) {
		Objects.requireNonNull( component );
		NGKeyValueCodingAdditions.Utility.takeValueForKeyPath( component, value, _keyPath );
	}

	public String keyPath() {
		return _keyPath;
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + ":" + _keyPath + "]";
	}

	/**
	 * FIXME: This exception class serves as a temporary placeholder until we've designed proper error handling/reporting for the entire template parsing process // Hugi 2024-06-25
	 */
	public static class NGAssociationConstructionException extends RuntimeException {

		public NGAssociationConstructionException( String message ) {
			super( message );
		}
	}
}