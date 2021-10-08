package ng.appserver;

import ng.kvc.NGKeyValueCoding;

public class NGKeyValueAssociation extends NGAssociation {

	private final String _keyPath;

	public NGKeyValueAssociation( final String keyPath ) {
		_keyPath = keyPath;
	}

	@Override
	public Object valueInComponent( final NGComponent aComponent ) {
		// FIXME: We're probably going to want to use valueForKeyPath here // Hugi 2021-10-08 
		return NGKeyValueCoding.Utility.valueForKey( aComponent, _keyPath );
	}
}