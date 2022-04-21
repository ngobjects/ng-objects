package er.extensions.bettertemplates;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

public class NSDictionary<K, V> extends HashMap<K, V> {

	public NSDictionary( V value, K key ) {
		super();
		put( key, value );
	}

	public NSDictionary() {}

	public Enumeration objectEnumerator() {
		return Collections.enumeration( this.values() );
	}

	public Enumeration keyEnumerator() {
		return Collections.enumeration( this.keySet() );
	}

	public Object objectForKey( String key ) {
		return get( key );
	}

	public void setObjectForKey( V object, K key ) {
		put( key, object );
	}

	public NSMutableDictionary<K, V> mutableClone() {
		return (NSMutableDictionary<K, V>)this.clone();
	}
}
