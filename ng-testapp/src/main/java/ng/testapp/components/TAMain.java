package ng.testapp.components;

import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

public class TAMain extends NGComponent {

	public String valueForTheTestTextField = "I am a field value";

	public int index;
	public String currentName;
	public List<String> names = List.of( "Hugi", "Atli", "Logi" );

	public boolean showHiddenPart;

	public TAMain( NGContext context ) {
		super( context );
	}

	public String someValueBoundInTheWodFile() {
		return "This is a string from a java method, bound in the wod file";
	}

	public String currentHref() {
		return "https://www.hugi.io/" + currentName;
	}

	public NGActionResults printCurrentName() {
		System.out.println( "The clicked name is: " + currentName );
		return null;
	}

	public NGActionResults toggleShowHidden() {
		showHiddenPart = !showHiddenPart;
		return null;
	}

	public String showHideToggleString() {
		return showHiddenPart ? "Hide" : "Show";
	}

	public NGActionResults testAction() {
		System.out.println( "Field value is: " + valueForTheTestTextField );
		return null;
	}
}