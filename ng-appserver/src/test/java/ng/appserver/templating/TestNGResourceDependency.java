package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.NGComponentRequestTestHarness.TestContext;
import ng.appserver.NGComponentRequestTestHarness.TestPage;
import ng.appserver.NGComponentRequestTestHarness.TestRequest;
import ng.appserver.NGContext;
import ng.appserver.NGSession;
import ng.appserver.http.NGResponse;
import ng.appserver.http.NGStandardResponse;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.associations.NGConstantValueAssociation;
import ng.appserver.templating.elements.NGJavaScript;

public class TestNGResourceDependency {

	private static final NGResourceDependency SCRIPT = NGResourceDependency.script( "app", "js/app.js" );
	private static final NGResourceDependency STYLESHEET = NGResourceDependency.stylesheet( "app", "css/app.css" );

	@Test
	public void declaredDependenciesAreAddedToTheHeadOnce() {
		final String html = render( Map.of(), new Declaring( SCRIPT, STYLESHEET ), new Declaring( SCRIPT ) );

		assertEquals( """
				<html><head><title>t</title><script src="/nr/app/js/app.js"></script>
				<link rel="stylesheet" type="text/css" href="/nr/app/css/app.css" />
				</head><body>xx</body></html>""", html );
	}

	@Test
	public void resourcesThePageIncludesItselfAreNotAddedAgain() {
		final Map<String, NGAssociation> associations = Map.of(
				"filename", new NGConstantValueAssociation( "js/app.js" ),
				"namespace", new NGConstantValueAssociation( "app" ) );

		final String html = render( Map.of(), new NGJavaScript( null, associations, null ), new Declaring( SCRIPT ) );

		assertEquals( """
				<html><head><title>t</title></head><body><script src="/nr/app/js/app.js"></script>x</body></html>""", html );
	}

	@Test
	public void partialPageResponsesAreLeftAlone() {
		final String html = render( Map.of( "ng-container-id", List.of( "container" ) ), new Declaring( SCRIPT ) );

		assertEquals( "", html );
	}

	@Test
	public void insertsBeforeTheHeadEndInAnyCase() {
		assertNull( NGComponent.insertBeforeHeadEnd( "<p>no head</p>", "x" ) );
		assertEquals( "<HEAD>x</HEAD>", NGComponent.insertBeforeHeadEnd( "<HEAD></HEAD>", "x" ) );
	}

	/**
	 * An element that declares the given dependencies, and renders as "x"
	 */
	private static class Declaring implements NGElement {

		private final List<NGResourceDependency> _dependencies;

		Declaring( final NGResourceDependency... dependencies ) {
			_dependencies = List.of( dependencies );
		}

		@Override
		public List<NGResourceDependency> resourceDependencies() {
			return _dependencies;
		}

		@Override
		public void appendToResponse( NGResponse response, NGContext context ) {
			response.appendContentString( "x" );
		}
	}

	/**
	 * @return The page generated with the given elements rendered in its body, for a request with the given form values
	 */
	private static String render( final Map<String, List<String>> formValues, final NGElement... bodyElements ) {
		final NGContext context = new TestContext( new TestRequest( "/wo/1.0", formValues ), new NGSession() );

		final TestPage page = new TestPage( context, "" ) {
			@Override
			public void appendOrTraverse( NGResponse response, NGContext context ) {
				if( context.shouldAppendToResponse() ) {
					response.appendContentString( "<html><head><title>t</title></head><body>" );
				}

				for( final NGElement element : bodyElements ) {
					element.appendOrTraverse( response, context );
				}

				if( context.shouldAppendToResponse() ) {
					response.appendContentString( "</body></html>" );
				}
			}
		};

		return ((NGStandardResponse)page.generateResponse()).contentString();
	}
}
