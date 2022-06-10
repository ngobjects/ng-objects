package ng.testapp.components;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;

public class WrapperComponent extends NGComponent {

	public WrapperComponent( NGContext context ) {
		super( context );
	}

	public String someString() {
		return "I am a string from that same wrapper component";
	}
}