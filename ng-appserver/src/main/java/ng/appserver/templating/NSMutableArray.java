package ng.appserver.templating;

public class NSMutableArray extends NSArray {

	public NSMutableArray() {}

	public NSMutableArray( int size ) {
		super( size );
	}

	public void addObject( Object object ) {
		add( object );
	}

	public Object objectAtIndex( int i ) {
		return get( i );
	}
}
