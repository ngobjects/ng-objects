package ng.testapp.components;

import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

public class TAImagesPage extends NGComponent {

	public record Image( String filename, String title ) {}

	public Image currentImage;

	public TAImagesPage( NGContext context ) {
		super( context );
	}

	public List<Image> images() {
		return List.of(
				new Image( "test-image-1.jpg", "Image 1" ),
				new Image( "test-image-2.jpg", "Image 2" ),
				new Image( "test-image-3.jpg", "Image 3" ),
				new Image( "test-image-4.jpg", "Image 4" ) );
	}

	public byte[] testImage3Data() {
		return application().resourceManager().obtainAppResource( "app", "test-image-3.jpg" ).get().bytes();
	}

	public NGActionResults selectImage() {
		final TAImageDetailPage detailPage = pageWithName( TAImageDetailPage.class );
		detailPage.image = currentImage;
		return detailPage;
	}
}