package ng.appserver;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGBindingAssociation extends NGAssociation {

	private static final Logger logger = LoggerFactory.getLogger( NGBindingAssociation.class );

	private final String _binding;

	public NGBindingAssociation( final String binding ) {
		_binding = binding;
	}

	@Override
	public Object valueInComponent( final NGComponent component ) {
		Objects.requireNonNull( component );
		Object value = component.valueForBinding( _binding );
		logger.error( "'{}':'{}'. This is where we should be retrieving the value for the component but aren't.", _binding, value );
		return value;
	}

	@Override
	public void setValue( Object value, NGComponent component ) {
		Objects.requireNonNull( component );
		logger.error( "setValue not yet implemented fpr binding associations" );
	}
}