package ng.appserver.templating;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

public class _NSDictionary<K, V> extends HashMap<K, V> {

	public _NSDictionary() {}

	public _NSDictionary( V value, K key ) {
		put( key, value );
	}

	public Enumeration<V> objectEnumerator() {
		return Collections.enumeration( this.values() );
	}

	public Enumeration<K> keyEnumerator() {
		return Collections.enumeration( this.keySet() );
	}
}