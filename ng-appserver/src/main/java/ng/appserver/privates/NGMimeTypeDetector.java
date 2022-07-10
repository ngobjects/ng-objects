package ng.appserver.privates;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mime Type detection.
 *
 * FIXME: This map needs to be extensible // Hugi 2022-04-18
 */
public class NGMimeTypeDetector {

	public static final String mimeTypeForResourceName( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		final int lastPeriodIndex = resourceName.lastIndexOf( "." );

		if( lastPeriodIndex == -1 ) {
			throw new IllegalArgumentException( "Could not deduce mimeType from resource name " + resourceName );
		}

		final String extension = resourceName.substring( lastPeriodIndex + 1 );
		final String mimeType = mimeTypeForExtension( extension );

		if( mimeType == null ) {
			throw new IllegalArgumentException( "For some reason, our advanced algorithm has not managed to identify your resource type" );
		}

		return mimeType;
	}

	public static final String mimeTypeForExtension( final String extension ) {
		Objects.requireNonNull( extension );
		return _mimeTypeMap.get( extension );
	}

	private static final Map<String, String> _mimeTypeMap = populateMimeTypeMap();

	private static Map<String, String> populateMimeTypeMap() {
		final Map<String, String> map = new HashMap<>();
		map.put( "jpg", "image/jpeg" );
		map.put( "jpeg", "image/jpeg" );
		map.put( "js", "text/javascript" );
		map.put( "png", "image/png" );
		map.put( "css", "text/css" );
		return map;
	}
}