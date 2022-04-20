package ng.testapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGUtils;
import ng.testapp.components.ExampleComponent;
import ng.testapp.components.ProgrammaticDynamicComponent;

public class Application extends NGApplication {

	private static Logger logger = LoggerFactory.getLogger( Application.class );

	public static void main( String[] args ) {
		NGApplication.run( args, Application.class );
	}

	public Application() {
		routeTable().map( "/response-plain", ( request ) -> {
			NGResponse response = new NGResponse( "Oh look, a response!", 200 );
			response.addCookie( new NGCookie( "nafn", "Hugi" ) );
			return response;
		} );

		routeTable().map( "/response-image", ( request ) -> {
			NGResponse response = new NGResponse();
			response.setContentBytes( NGUtils.readWebserverResource( "test-image-4.jpg" ).get() );
			response.setHeader( "content-type", "image/jpeg" );
			return response;
		} );

		routeTable().map( "/component-programmatic", ( request ) -> {
			return pageWithName( ProgrammaticDynamicComponent.class, request.context() );
		} );

		routeTable().map( "/component-plain", ( request ) -> {
			return pageWithName( ExampleComponent.class, request.context() );
		} );

		routeTable().map( "/print-routes", ( request ) -> {
			StringBuilder b = new StringBuilder();

			b.append( "<h2>These are the routes registered with the application</h2>" );
			b.append( "<style>body{ font-family: sans-serif}</style>" );

			routeTable().routes().forEach( route -> {
				b.append( String.format( "<a style=\"display:inline-block; width:200px\" href=\"%s\">%s</a>", route.pattern(), route.pattern() ) );
				b.append( " -> " );
				b.append( route.routeHandler().getClass().getName() );
				b.append( "<br>" );
			} );

			NGResponse response = new NGResponse();
			response.setContentString( b.toString() );
			response.setHeader( "content-type", "text/html" );
			return response;
		} );
	}

	@Override
	public NGResponse dispatchRequest( NGRequest request ) {
		logger.info( "uri: {} ", request.uri() );
		logger.info( "method: {} ", request.method() );
		logger.info( "headers: {} ", request.headers() );
		logger.info( "cookieValues: {} ", request.cookieValues() );
		logger.info( "formValues: {} ", request.formValues() );
		logger.info( "contentString: {}", request.contentString() );
		return super.dispatchRequest( request );
	}
}