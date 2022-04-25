package ng.testapp.components;

import java.util.List;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;

public class RepetitionComponent extends NGComponent {

	public Image currentImage;

	public List<Image> images = List.of(
			new Image( "test-image-1.jpg", "Mynd 1" ),
			new Image( "test-image-2.jpg", "Mynd 2" ),
			new Image( "test-image-3.jpg", "Mynd 3" ),
			new Image( "test-image-4.jpg", "Mynd 4" ) );

	public RepetitionComponent( NGContext context ) {
		super( context );
	}

	public record Image( String filename, String title ) {}
}