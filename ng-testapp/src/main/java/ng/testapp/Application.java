package ng.testapp;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.testapp.components.ExampleComponent;
import ng.testapp.components.FormComponent;
import ng.testapp.components.ProgrammaticDynamicComponent;
import ng.testapp.components.RepetitionComponent;
import ng.testapp.components.SingleFileComponent;

public class Application extends NGApplication {

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
			final byte[] bytes = application().resourceManager().obtainWebserverResource( "app", "test-image-4.jpg" ).get().bytes();

			NGResponse response = new NGResponse();
			response.setContentBytes( bytes );
			response.setHeader( "content-type", "image/jpeg" );
			return response;
		} );

		routeTable().map( "/component-programmatic", ( request ) -> {
			return pageWithName( ProgrammaticDynamicComponent.class, request.context() );
		} );

		routeTable().map( "/component-plain", ( request ) -> {
			return pageWithName( ExampleComponent.class, request.context() );
		} );

		routeTable().map( "/component-repetition", ( request ) -> {
			return pageWithName( RepetitionComponent.class, request.context() );
		} );

		routeTable().map( "/component-form", ( request ) -> {
			return pageWithName( FormComponent.class, request.context() );
		} );

		routeTable().map( "/single-file-classless", ( request ) -> {
			return pageWithName( "SingleFileClasslessComponent", request.context() );
		} );

		routeTable().map( "/single-file", ( request ) -> {
			return pageWithName( SingleFileComponent.class, request.context() );
		} );

		routeTable().map( "/form-handler", ( request ) -> {
			System.out.println( request.contentString() );
			System.out.println( request.formValues() );
			return new NGResponse();
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
	public NGActionResults defaultResponse( NGRequest request ) {
		return pageWithName( ExampleComponent.class, request.context() );
	}

	//	@Override
	//	public NGResponse dispatchRequest( NGRequest request ) {
	//		logger.info( "uri: {} ", request.uri() );
	//		logger.info( "method: {} ", request.method() );
	//		logger.info( "headers: {} ", request.headers() );
	//		logger.info( "cookieValues: {} ", request.cookieValues() );
	//		logger.info( "formValues: {} ", request.formValues() );
	//		logger.info( "contentString: {}", request.contentString() );
	//		return super.dispatchRequest( request );
	//	}
}