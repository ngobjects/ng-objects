package ng.testapp;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.directactions.NGDirectActionRequestHandler;
import ng.plugins.Elements;
import ng.plugins.Routes;
import ng.testapp.components.TAMain;
import ng.testapp.components.TAProgrammaticDynamicComponent;
import ng.testapp.components.TASingleFileComponent;
import ng.testapp.directaction.JSONAction;

public class Application extends NGApplication {

	public static void main( String[] args ) {
		NGApplication.run( args, Application.class );
	}

	/**
	 * Registration of direct action classes is very primitive, since DAs have received little love through ng's development process.
	 *
	 * DAs are still included, mostly because I believe they might be important for migrating older projects (which often contain plenty of DAs).
	 *
	 * But I think actual routes really mostly replace DAs (they give you the static URLs, but in a nicer way)
	 */
	public Application() {
		// FIXME: Just a reminder that this sucks // Hugi 2025-06-19
		NGDirectActionRequestHandler.registerDirectActionClass( DirectAction.class );
		NGDirectActionRequestHandler.registerDirectActionClass( JSONAction.class );
	}

	/**
	 * Demonstrates how you can map URLs to actions in your application (our version of routing, as seen in most web frameworks).
	 *
	 * As seen, we generate our route table by invoking super.routes() and then adding routes to it by invoking map().
	 *
	 * Route patterns are very simple in this current incarnation.
	 * You can either map an exact URL, or end the URL pattern with "*" to map everything starting with the given string (as in the /print route shown below).
	 * Note that unlike most other frameworks, we don't differentiate between methods when defining a route. .map()-ing a route will handle the URL, regardless of the request method.
	 * Like most other things, that may change in the future, if we feel it's useful.
	 *
	 * You can .map() a URL to
	 *
	 * 1) A component instance (will just construct an instance of the component and generate a response from it)
	 * 2) A function accepting the request as a parameter and returning NGActionResults (Function<NGRequest,NGActionResults)
	 * 3) A method accepting no parameters and returning NGActionResults (Supplier<NGActionResults)
	 * 4) An instance of an NGRequestHandler. In this case you can think of .map like  a fancy version of WO's .registerRequestHandler
	 */
	@Override
	public Routes routes() {
		return super.routes()
				.map( "/", TAMain.class )

				// A little Hello World demo for demonstration of a wildcard patterned URL
				.map( "/print/*", request -> {
					final String responseContent = "Hello " + request.parsedURI().getString( 1 );

					// Note that NGResponse's constructors are deprecated.
					// This is because NGResponse and NGRequest are seriously being considered for conversion to interfaces,
					// so we can have separate response/request types based on the content they serve (string, byte[], stream, multipart etc.).
					// In that case, generic response construction would possible be performed using factory methods instead (NGResponse.of() or something like that)
					return new NGResponse( responseContent, 200 );
				} )

				// Demonstrates how we can map a URL to a response-generating method accepting the request as a parameter
				.map( "/response-image", this::imageReponse )

				// Test adding cookies to the response. Mainly to demonstrate
				.map( "/response-with-cookie", () -> {
					final NGResponse response = new NGResponse( "Oh look, a response!", 200 );
					response.addCookie( new NGCookie( "nafn", "Hugi" ) );
					return response;
				} )

				// A component with a single-file-template
				.map( "/single-file", TASingleFileComponent.class )

				// A classless component with a single-file-template
				.map( "/single-file-classless", request -> pageWithName( "TASingleFileClasslessComponent", request.context() ) )

				// Testing of programmatic component generation
				// FIXME: Doesn't really belong in the demo project, find a new home // Hugi 2025-09-27
				.map( "/component-programmatic", TAProgrammaticDynamicComponent.class );
	}

	@Override
	public Elements elements() {
		return super.elements();
	}

	private NGActionResults imageReponse( NGRequest request ) {
		final byte[] bytes = application().resourceManager().obtainWebserverResource( "app", "test-image-4.jpg" ).get().bytes();

		final NGResponse response = new NGResponse();
		response.setContentBytes( bytes );
		response.setHeader( "content-type", "image/jpeg" );
		return response;
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