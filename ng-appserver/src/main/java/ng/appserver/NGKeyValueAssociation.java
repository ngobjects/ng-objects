package ng.appserver;

import ng.kvc.NGKeyValueCoding;

public class NGKeyValueAssociation extends NGAssociation {

	private final String _keyPath;

	public NGKeyValueAssociation( final String keyPath ) {
		_keyPath = keyPath;
	}

	@Override
	public Object valueInComponent( final NGComponent aComponent ) {
		return NGKeyValueCoding.Utility.valueForKeyPath( aComponent, _keyPath );
	}
}