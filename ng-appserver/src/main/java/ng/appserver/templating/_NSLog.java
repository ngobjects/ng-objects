package ng.appserver.templating;

public class _NSLog {

	public static Smu debug = new Smu();
	public static Smu err = new Smu();

	public static boolean debugLoggingAllowedForLevelAndGroups( int i, long l ) {
		// TODO Auto-generated method stub
		return false;
	}

	public static class Smu {
		public void appendln( Object object ) {
			System.out.println( object );
		}
	}
}
