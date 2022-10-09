package ng.appserver;

import java.util.Objects;

import ng.kvc.NGKeyValueCodingAdditions;

public class NGKeyValueAssociation extends NGAssociation {

	private final String _keyPath;

	public NGKeyValueAssociation( final String keyPath ) {
		_keyPath = keyPath;
	}

	@Override
	public Object valueInComponent( final NGComponent component ) {
		Objects.requireNonNull( component );
		return NGKeyValueCodingAdditions.Utility.valueForKeyPath( component, keyPath() );
	}

	@Override
	public void setValue( Object value, NGComponent component ) {
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
}