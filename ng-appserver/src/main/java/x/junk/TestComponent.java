package x.junk;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;

public class TestComponent extends NGComponent {

	public TestComponent( NGContext context ) {
		super( context );
	}

	public String testString() {
		return "I am a string in a subcomponent in the ng-objects 'bundle'";
	}

	public String someString() {
		return (String)valueForBinding( "someString" );
	}

	@Override
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}
}