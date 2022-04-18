package ng.testapp.components;

import java.util.ArrayList;
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

public class ProgrammaticDynamicComponent extends NGComponent {

	public ProgrammaticDynamicComponent( NGContext context ) {
		super( context );
	}

	public String someMethodReturningImageFilename() {
		return "test-image-2.jpg";
	}

	@Override
	public NGElement template() {
		final NGDynamicGroup g = new NGDynamicGroup( "wat?", Collections.emptyMap(), null );
		g._children = new ArrayList<>();

		g._children.add( new NGHTMLBareString( "<!doctype html>\n" ) );
		g._children.add( new NGHTMLBareString( "<html>\n" ) );
		g._children.add( new NGHTMLBareString( "<head>\n" ) );

		final Map<String, NGAssociation> style = new HashMap<>();
		style.put( "filename", new NGConstantValueAssociation( "main.css" ) );
		g._children.add( new NGStylesheet( "wat?", style, null ) );

		g._children.add( new NGHTMLBareString( "</head>\n" ) );
		g._children.add( new NGHTMLBareString( "<body>\n" ) );
		g._children.add( new NGHTMLBareString( "<p>Hello</p>\n" ) );

		final Map<String, NGAssociation> m = new HashMap<>();
		m.put( "filename", new NGConstantValueAssociation( "test-image-1.jpg" ) );
		g._children.add( new NGImage( "wat?", m, null ) );

		final Map<String, NGAssociation> m2 = new HashMap<>();
		m2.put( "filename", new NGKeyValueAssociation( "someMethodReturningImageFilename" ) );
		g._children.add( new NGImage( "wat?", m2, null ) );

		g._children.add( new NGHTMLBareString( "</body>\n" ) );
		g._children.add( new NGHTMLBareString( "</html>\n" ) );

		return g;
	}
}