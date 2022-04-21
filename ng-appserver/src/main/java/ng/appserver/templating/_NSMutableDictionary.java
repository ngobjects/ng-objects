package ng.appserver.templating;

public class _NSMutableDictionary<K, V> extends _NSDictionary<K, V> {

	public void setObjectForKey( V object, K key ) {
		put( key, object );
	}
}