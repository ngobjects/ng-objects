package ng.appserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.privates.NGParsedURI;

/**
 * FIXME: Ideally I'd like this to work with streams, not byte arrays. In that case it just becomes the responsibility of this code to link up the file/socket streams
 */

public class NGResourceRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( final NGRequest request ) {
		final Optional<String> resourceName = NGParsedURI.of( request.uri() ).getStringOptional( 1 );

		if( resourceName.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final Optional<byte[]> resourceBytes = NGApplication.application().resourceManager().bytesForWebserverResourceNamed( resourceName.get() );

		// FIXME: How to handle this properly? User configurable? Just always a 404
		if( resourceBytes.isEmpty() ) {
			return new NGResponse( "Resource '" + resourceName.get() + "' does not exist", 404 );
		}

		final String mimeType = NGMimeTypeDetector.mimeTypeForResourceName( resourceName.get() );

		// FIXME: Detect and set the correct response headers
		final NGResponse response = new NGResponse( resourceBytes.get(), 200 );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceName.get() ) );
		response.setHeader( "Content-Type", mimeType );
		return response;
	}

	/**
	 * Mime Type detection.
	 *
	 * FIXME: This map needs to be extensible // Hugi 2022-04-18
	 */
	public static class NGMimeTypeDetector {

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
			map.put( "png", "image/png" );
			map.put( "css", "text/css" );
			return map;
		}
	}
}