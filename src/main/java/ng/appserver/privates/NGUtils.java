package ng.appserver.privates;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import ng.appserver.NGResourceManager;

public class NGUtils {

	/**
	 * Reads the content of the given Java resource
	 */
	public static Optional<byte[]> readJavaResource( final String resourcePath ) {
		try( final InputStream resourceAsStream = NGResourceManager.class.getResourceAsStream( resourcePath )) {

			if( resourceAsStream == null ) {
				return Optional.empty();
			}

			return Optional.of( resourceAsStream.readAllBytes() );
		}
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}
}