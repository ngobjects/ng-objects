package ng.kvc;

public interface NGKeyValueCodingAdditions {

	public Object valueForKeyPath( String keyPath );

	public void takeValueForKeyPath( Object value, String keyPath );
}