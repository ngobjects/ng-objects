package ng.testapp.components;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;

public class ExampleComponent extends NGComponent {

	public ExampleComponent( NGContext context ) {
		super( context );
	}

	public String smu() {
		return "Þetta er hreint ekki svo slæmt";
	}

	public String someStuff() {
		return "Jóhóhó";
	}

	public String someValue() {
		return "Hehehe";
	}
}