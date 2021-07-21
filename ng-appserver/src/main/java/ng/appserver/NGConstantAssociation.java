package ng.appserver;

public class NGConstantAssociation extends NGAssociation {

	private final Object _value;

	public NGConstantAssociation( final Object value ) {
		_value = value;
	}

	@Override
	public Object valueInComponent( NGComponent aComponent ) {
		return _value;
	}
}