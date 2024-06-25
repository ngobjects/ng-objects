package ng.appserver;

import java.util.Objects;

import ng.kvc.NGKeyValueCodingAdditions;

public class NGKeyValueAssociation extends NGAssociation {

	private final String _keyPath;

	public NGKeyValueAssociation( final String keyPath ) {
		validateKeyPath( keyPath );
		_keyPath = keyPath;
	}

	/**
	 * FIXME: We should be conducting a more thorough syntax check for every "disallowed" character // Hugi 2024-06-25
	 */
	private static void validateKeyPath( final String keyPath ) {
		if( keyPath == null ) {
			throw new NGAssociationConstructionException( "An association's keyPath can't be null" );
		}

		if( keyPath.isEmpty() ) {
			throw new NGAssociationConstructionException( "An association's keyPath can't be an empty string" );
		}

		if( keyPath.startsWith( "." ) ) {
			throw new NGAssociationConstructionException( "An association's keyPath can't start with a period." );
		}

		if( keyPath.endsWith( "." ) ) {
			throw new NGAssociationConstructionException( "An association's keyPath can't end with a period." );
		}

		if( keyPath.contains( "@" ) ) {
			throw new NGAssociationConstructionException( "Our keyPaths don't support keypath operators (keys prefixed with '@')" );
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