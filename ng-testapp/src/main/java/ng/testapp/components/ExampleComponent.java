package ng.testapp.components;

import java.util.List;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;

public class ExampleComponent extends NGComponent {

	public String item;
	public List<String> names = List.of( "Hugi", "Atli", "Logi" );

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