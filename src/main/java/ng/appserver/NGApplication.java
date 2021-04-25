package ng.appserver;

public class NGApplication {

	private static NGApplication _application;

	/**
	 * FIXME: Not sure if this method should actually be provided 
	 */
	public static void main( final String[] args ) {
		main( args, NGApplication.class );
	}

	public static void main( final String[] args, final Class<? extends NGApplication> applicationClass ) {
		try {
			_application = applicationClass.getDeclaredConstructor().newInstance();
			_application.run();
		}
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	private void run() {
		try {
			new NGJettyAdaptor().run();
		}
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static NGApplication application() {
		return _application;
	}

	public NGResponse dispatchRequest( final NGRequest request ) {
		final var response = new NGResponse( "Halló mamma" );
		return response;
	}
}