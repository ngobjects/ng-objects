package ng.appserver;

import ng.kvc.NGKeyValueCodingAdditions;

public class NGKeyValueAssociation extends NGAssociation {

	private final String _keyPath;

	public NGKeyValueAssociation( final String keyPath ) {
		_keyPath = keyPath;
	}

	@Override
	public Object valueInComponent( final NGComponent aComponent ) {
		return NGKeyValueCodingAdditions.Utility.valueForKeyPath( aComponent, keyPath() );
	}

	public String keyPath() {
		return _keyPath;
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + ":" + _keyPath + "]";
	}
}