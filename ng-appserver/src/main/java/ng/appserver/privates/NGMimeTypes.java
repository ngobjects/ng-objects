package ng.appserver.privates;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mime Type detection.
 *
 * FIXME: This map needs to be extensible // Hugi 2022-04-18
 */
public class NGMimeTypes {

	private static final Logger logger = LoggerFactory.getLogger( NGMimeTypes.class );

	public static final String mimeTypeForResourceName( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		final int lastPeriodIndex = resourceName.lastIndexOf( "." );

		if( lastPeriodIndex == -1 ) {
			throw new IllegalArgumentException( "Could not deduce mimeType from resource name " + resourceName );
		}

		final String extension = resourceName.substring( lastPeriodIndex + 1 );
		final String mimeType = mimeTypeForExtension( extension );

		if( mimeType == null ) {
			logger.warn( "Unknown file type %s, returning default mimeType".formatted( extension ) );
			return "application/octet-stream";
		}

		return mimeType;
	}

	public static final String mimeTypeForExtension( final String extension ) {
		Objects.requireNonNull( extension );
		return _mimeTypeMap.get( extension );
	}

	// FIXME: This map needs to be case insensitive // Hugi 2024-02-24
	private static final Map<String, String> _mimeTypeMap = populateMimeTypeMap();

	private static Map<String, String> populateMimeTypeMap() {
		final Map<String, String> map = new HashMap<>();
		map.put( "jpg", "image/jpeg" );
		map.put( "jpeg", "image/jpeg" );
		map.put( "js", "text/javascript" );
		map.put( "png", "image/png" );
		map.put( "css", "text/css" );
		map.put( "htm", "text/html" );
		map.put( "html", "text/html" );
		map.put( "svg", "image/svg+xml" );
		map.put( "ttf", "application/x-font-ttf" );
		map.put( "otf", "application/x-font-opentype" );
		map.put( "woff", "application/font-woff" );
		map.put( "woff2", "application/font-woff2" );
		map.put( "eot", "application/vnd.ms-fontobject" );
		map.put( "sfnt", "application/font-sfnt" );
		return map;
	}
}