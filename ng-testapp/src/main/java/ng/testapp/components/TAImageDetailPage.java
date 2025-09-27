package ng.testapp.components;

import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;
import ng.testapp.components.TAImagesPage.Image;

public class TAImageDetailPage extends NGComponent {

	public Image image;

	public TAImageDetailPage( NGContext context ) {
		super( context );
	}
}