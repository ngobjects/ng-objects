package ng.testapp.components;

import java.util.List;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;

public class RepetitionComponent extends NGComponent {

	public String currentImageName;

	public List<String> images = List.of(
			"test-image-1.jpg",
			"test-image-2.jpg",
			"test-image-3.jpg",
			"test-image-4.jpg" );

	public RepetitionComponent( NGContext context ) {
		super( context );
	}

}