package ng.testapp.components;

import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

public class TAWrapperComponent extends NGComponent {

	public TAWrapperComponent( NGContext context ) {
		super( context );
	}

	public String someString() {
		return "I am a string from that same wrapper component";
	}
}