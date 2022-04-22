package ng.appserver.templating;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

public class _NSArray<E> extends ArrayList<E> {

	public _NSArray( Collection<E> collection ) {
		super( collection );
	}

	public Enumeration<E> objectEnumerator() {
		return Collections.enumeration( this );
	}
}