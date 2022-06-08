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

	public String someValueBoundInTheWodFile() {
		return "This is a string from a java method, bound in the wod file";
	}

	public String currentHref() {
		return "https://www.hugi.io/" + currentName;
	}

	public NGActionResults printCurrentName() {
		System.out.println( currentName );
		return null;
	}
}