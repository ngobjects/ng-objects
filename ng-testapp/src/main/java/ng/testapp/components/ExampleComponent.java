package ng.testapp.components;

import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;

public class ExampleComponent extends NGComponent {

	public String valueForTheTestTextField = "I am a field value";

	public int index;
	public String currentName;
	public List<String> names = List.of( "Hugi", "Atli", "Logi" );

	public ExampleComponent( NGContext context ) {
		super( context );
	}

	public String someValue() {
		return "This is a string from a java method";
	}

	public String someStuff() {
		return "Some string stuff";
	}

	public String currentHref() {
		return "https://www.hugi.io/" + currentName;
	}

	public NGActionResults printCurrentName() {
		System.out.println( currentName );
		return null;
	}
}