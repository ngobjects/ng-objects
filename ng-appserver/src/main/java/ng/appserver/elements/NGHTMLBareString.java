package ng.appserver.elements;

import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGHTMLBareString extends NGElement {

	private final String _string;

	public NGHTMLBareString( String aString ) {
		this._string = aString;
	}

	public void appendToResponse( NGResponse aResponse, NGContext aContext ) {
		aResponse.appendContentString( this._string );
	}
}