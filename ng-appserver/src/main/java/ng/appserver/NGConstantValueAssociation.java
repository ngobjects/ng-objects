package ng.appserver;

import java.util.Objects;

public class NGConstantValueAssociation extends NGAssociation {

	private final Object _value;

	public NGConstantValueAssociation( final Object value ) {
		_value = value;
	}

	@Override
	public Object valueInComponent( NGComponent aComponent ) {
		return _value;
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + ":" + _value + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash( _value );
	}

	@Override
	public boolean equals( Object obj ) {
		if( this == obj ) {
			return true;
		}
		if( obj == null ) {
			return false;
		}
		if( getClass() != obj.getClass() ) {
			return false;
		}
		NGConstantValueAssociation other = (NGConstantValueAssociation)obj;
		return Objects.equals( _value, other._value );
	}
}