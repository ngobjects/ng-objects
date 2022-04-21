package er.extensions.bettertemplates;

public class NSMutableDictionary<K, V> extends NSDictionary<K, V> {

	public int count() {
		return size();
	}

	public Object valueForKey( String bindingName ) {
		return objectForKey( bindingName );
	}
}
