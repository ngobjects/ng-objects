package ng.appserver;

public class NGBindingAssociation extends NGAssociation {

	private final String _binding;

	public NGBindingAssociation( final String binding ) {
		_binding = binding;
	}

	@Override
	public Object valueInComponent( final NGComponent component ) {
		return component.valueForBinding( _binding );
	}

	@Override
	public void setValue( Object value, NGComponent component ) {
		component.setValueForBinding( value, _binding );
	}
}