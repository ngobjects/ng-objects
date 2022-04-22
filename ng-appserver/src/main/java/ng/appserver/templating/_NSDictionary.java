package ng.appserver.templating;

import java.util.HashMap;

public class _NSDictionary<K, V> extends HashMap<K, V> {

	public _NSDictionary() {}

	public _NSDictionary( V value, K key ) {
		put( key, value );
	}
}