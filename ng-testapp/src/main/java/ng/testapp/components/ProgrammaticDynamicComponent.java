package ng.testapp.components;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGKeyValueAssociation;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGStylesheet;
import ng.appserver.privates.NGUtils;

public class ProgrammaticDynamicComponent extends NGComponent {

	public ProgrammaticDynamicComponent( NGContext context ) {
		super( context );
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
		final NGDynamicGroup g = new NGDynamicGroup( "wat?", Collections.emptyMap(), null );

		g.children().add( new NGHTMLBareString( "<!doctype html>\n" ) );
		g.children().add( new NGHTMLBareString( "<html>\n" ) );
		g.children().add( new NGHTMLBareString( "<head>\n" ) );

		final Map<String, NGAssociation> stylesheet = new HashMap<>();
		stylesheet.put( "filename", new NGConstantValueAssociation( "main.css" ) );
		g.children().add( new NGStylesheet( "wat?", stylesheet, null ) );

		g.children().add( new NGHTMLBareString( "</head>\n" ) );
		g.children().add( new NGHTMLBareString( "<body>\n" ) );

		g.children().add( new NGHTMLBareString( "<h2>This is a programmatically generated component template</h2>\n" ) );

		final Map<String, NGAssociation> image1 = new HashMap<>();
		image1.put( "filename", new NGConstantValueAssociation( "test-image-1.jpg" ) );
		image1.put( "width", new NGConstantValueAssociation( 300 ) );
		g.children().add( new NGImage( "wat?", image1, null ) );

		g.children().add( new NGHTMLBareString( "<br><br>" ) );

		final Map<String, NGAssociation> image2 = new HashMap<>();
		image2.put( "filename", new NGKeyValueAssociation( "someMethodReturningImageFilename" ) );
		image2.put( "width", new NGConstantValueAssociation( 300 ) );
		g.children().add( new NGImage( "wat?", image2, null ) );

		g.children().add( new NGHTMLBareString( "<br><br>" ) );

		final Map<String, NGAssociation> image3 = new HashMap<>();
		image3.put( "filename", new NGKeyValueAssociation( "someMethodReturningFilenameHolder.filename" ) );
		image3.put( "width", new NGConstantValueAssociation( 300 ) );
		g.children().add( new NGImage( "wat?", image3, null ) );

		g.children().add( new NGHTMLBareString( "<br><br>" ) );

		final Map<String, NGAssociation> image4 = new HashMap<>();
		image4.put( "data", new NGConstantValueAssociation( NGUtils.readWebserverResource( "test-image-4.jpg" ).get() ) );
		image4.put( "width", new NGConstantValueAssociation( 300 ) );
		g.children().add( new NGImage( "wat?", image4, null ) );

		g.children().add( new NGHTMLBareString( "</body>\n" ) );
		g.children().add( new NGHTMLBareString( "</html>\n" ) );

		return g;
	}
}