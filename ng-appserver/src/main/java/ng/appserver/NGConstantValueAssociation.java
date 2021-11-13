package ng.appserver;

public class NGConstantValueAssociation extends NGAssociation {

	private final Object _value;

	public static final NGConstantValueAssociation TRUE = new NGConstantValueAssociation( Boolean.TRUE );
	public static final NGConstantValueAssociation FALSE = new NGConstantValueAssociation( Boolean.FALSE );

	public NGConstantValueAssociation( final Object value ) {
		_value = value;
	}

	@Override
	public Object valueInComponent( NGComponent aComponent ) {
		return _value;
	}
}