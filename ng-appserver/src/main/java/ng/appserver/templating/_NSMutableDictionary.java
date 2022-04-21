package ng.appserver.templating;

public class _NSMutableDictionary<K, V> extends _NSDictionary<K, V> {

	public int count() {
		return size();
	}

	public Object valueForKey( String bindingName ) {
		return objectForKey( bindingName );
	}
}
