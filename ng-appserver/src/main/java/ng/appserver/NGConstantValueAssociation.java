package ng.appserver;

public class NGConstantValueAssociation extends NGAssociation {

	private final Object _value;

	public NGConstantValueAssociation( final Object value ) {
		_value = value;
	}

	@Override
	public Object valueInComponent( NGComponent aComponent ) {
		return _value;
	}
}