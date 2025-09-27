package ng.testapp.components;

import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

public class TAFormComponent extends NGComponent {

	public String textFieldValue;

	// Iteration variable forthe pop up menu
	public PopUpItem popUpItem;

	public TAFormComponent( NGContext context ) {
		super( context );
	}

	public record PopUpItem( String name, String value ) {}

	public List<PopUpItem> popUpItems() {
		return List.of(
				new PopUpItem( "WebObjects", "Classic" ),
				new PopUpItem( "Wonder", "Lovely" ),
				new PopUpItem( "ng", "Awesome!" ) );

	}

	public List<String> browserValues() {
		return List.of(
				"Hugi",
				"Álfrún",
				"Örlygur",
				"Örnólfur" );
	}

	public NGActionResults submit() {
		return null;
	}
}