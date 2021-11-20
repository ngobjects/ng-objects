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

		g._children.add( new NGHTMLBareString( "<p>Hello</p>" ) );

		final Map<String, NGAssociation> m = new HashMap<>();
		m.put( "filename", new NGConstantValueAssociation( "test-image-1.jpg" ) );
		g._children.add( new NGImage( "wat?", m, null ) );

		final Map<String, NGAssociation> m2 = new HashMap<>();
		m2.put( "filename", new NGKeyValueAssociation( "someMethodReturningImageFilename" ) );
		g._children.add( new NGImage( "wat?", m2, null ) );

		g._children.add( new NGHTMLBareString( "<p>Hohoho</p>" ) );

		return g;
	}
}