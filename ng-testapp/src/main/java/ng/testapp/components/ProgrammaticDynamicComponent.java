package ng.testapp.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;
import ng.appserver.templating.assications.NGConstantValueAssociation;
import ng.appserver.templating.assications.NGKeyValueAssociation;
import ng.appserver.templating.elements.NGDynamicGroup;
import ng.appserver.templating.elements.NGHTMLBareString;
import ng.appserver.templating.elements.NGHyperlink;
import ng.appserver.templating.elements.NGImage;
import ng.appserver.templating.elements.NGString;
import ng.appserver.templating.elements.NGStylesheet;

public class ProgrammaticDynamicComponent extends NGComponent {

	public ProgrammaticDynamicComponent( NGContext context ) {
		super( context );
	}

	public String header() {
		return "<h2>This is a programmatically generated component template</h2>\n";
	}

	public String someMethodReturningImageFilename() {
		return "test-image-2.jpg";
	}

	public FilenameHolder someMethodReturningFilenameHolder() {
		return new FilenameHolder();
	}

	public static class FilenameHolder {

		public String filename() {
			return "test-image-3.jpg";
		}
	}

	@Override
	public NGElement template() {
		final NGDynamicGroup g = new NGDynamicGroup( "wat?", Collections.emptyMap(), new ArrayList<>() );

		g.children().add( new NGHTMLBareString( "<!doctype html>\n" ) );
		g.children().add( new NGHTMLBareString( "<html>\n" ) );
		g.children().add( new NGHTMLBareString( "<head>\n" ) );

		final Map<String, NGAssociation> stylesheetAss = new HashMap<>();
		stylesheetAss.put( "filename", new NGConstantValueAssociation( "main.css" ) );
		g.children().add( new NGStylesheet( "wat?", stylesheetAss, null ) );

		g.children().add( new NGHTMLBareString( "</head>\n" ) );
		g.children().add( new NGHTMLBareString( "<body>\n" ) );

		final Map<String, NGAssociation> stringAss = new HashMap<>();
		stringAss.put( "value", new NGKeyValueAssociation( "header" ) );
		g.children().add( new NGString( "wat?", stringAss, null ) );

		final Map<String, NGAssociation> image1Ass = new HashMap<>();
		image1Ass.put( "filename", new NGConstantValueAssociation( "test-image-1.jpg" ) );
		image1Ass.put( "width", new NGConstantValueAssociation( 300 ) );
		g.children().add( new NGImage( "wat?", image1Ass, null ) );

		g.children().add( new NGHTMLBareString( "<br><br>" ) );

		final Map<String, NGAssociation> image2Ass = new HashMap<>();
		image2Ass.put( "filename", new NGKeyValueAssociation( "someMethodReturningImageFilename" ) );
		image2Ass.put( "width", new NGConstantValueAssociation( 300 ) );
		g.children().add( new NGImage( "wat?", image2Ass, null ) );

		g.children().add( new NGHTMLBareString( "<br><br>" ) );

		final Map<String, NGAssociation> image3Ass = new HashMap<>();
		image3Ass.put( "filename", new NGKeyValueAssociation( "someMethodReturningFilenameHolder.filename" ) );
		image3Ass.put( "width", new NGConstantValueAssociation( 300 ) );
		g.children().add( new NGImage( "wat?", image3Ass, null ) );

		g.children().add( new NGHTMLBareString( "<br><br>" ) );

		final Map<String, NGAssociation> linkAss = new HashMap<>();
		linkAss.put( "href", new NGConstantValueAssociation( "https://www.hugi.io/" ) );

		final NGHyperlink hyperlink = new NGHyperlink( "wat?", linkAss, null );
		g.children().add( hyperlink );

		final Map<String, NGAssociation> image4Ass = new HashMap<>();
		System.out.println( "Hahaha" );
		image4Ass.put( "data", new NGConstantValueAssociation( application().resourceManager().obtainWebserverResource( "app", "test-image-4.jpg" ).get().bytes() ) );
		image4Ass.put( "mimeType", new NGConstantValueAssociation( "image/jpeg" ) );
		image4Ass.put( "width", new NGConstantValueAssociation( 300 ) );
		hyperlink.children().add( new NGImage( "wat?", image4Ass, null ) );

		g.children().add( new NGHTMLBareString( "</body>\n" ) );
		g.children().add( new NGHTMLBareString( "</html>\n" ) );

		return g;
	}
}