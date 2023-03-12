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
		// FIXME: Setting values through binding associations is not yet supported // Hugi 2023-03-12
		System.out.println( "FIXME: Setting values through binding associations is not yet supported" );
	}
}