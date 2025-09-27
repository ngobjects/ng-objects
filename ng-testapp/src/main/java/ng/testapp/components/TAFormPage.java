package ng.testapp.components;

import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

public class TAFormPage extends NGComponent {

	public record PopUpItem( String name, String value ) {}

	public record SubmitResult( String textFieldValue, PopUpItem popUpValue ) {}

	public String textFieldValue;

	// Iteration variable forthe pop up menu
	public PopUpItem currentPopUpItem;
	public PopUpItem selectedPopUpItem;

	public SubmitResult submitResult;

	public TAFormPage( NGContext context ) {
		super( context );
	}

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
		submitResult = new SubmitResult( textFieldValue, selectedPopUpItem );
		return null;
	}
}