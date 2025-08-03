package ng.appserver.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * FIXME: We're currently missing caching of the resource's data // Hugi 2024-06-22
 * FIXME: We need to accommodate JAR resources, filesystem resources, dynamic resources etc. etc. // Hugi 2025-08-03
 * FIXME: A resource should know it's name, length and possibly content type (although the default implementation may derive that from it's name)
 */

public interface NGResource {

	/**
	 * @return A stream providing the resource data. The stream is the consumer's responsibility to close.
	 */
	public InputStream inputStream();

	/**
	 * @return The resource's data as a byte array
	 */
	public default byte[] bytes() {

		try( final InputStream is = inputStream()) {
			return is.readAllBytes();
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * @return A resource that obtains it's data from the given supplier
	 */
	public static NGResource of( final Callable<InputStream> inputStreamSupplier ) {
		return new NGInputStreamSupplierResource( inputStreamSupplier );
	}

	/**
	 * @return A resource that obtains it's data from the given supplier
	 */
	public static NGResource of( final URL url ) {
		return new NGClasspathResource( url );
	}

	public static class NGClasspathResource implements NGResource {

		private URL _url;

		/**
		 * Constructs a new classpath resource from the URL provided
		 */
		public NGClasspathResource( URL url ) {
			_url = url;
		}

		@Override
		public InputStream inputStream() {
			try {
				return _url.openStream();
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}
		}
	}

	/**
	 * A resource obtained from the java class path
	 */
	public static class NGInputStreamSupplierResource implements NGResource {

		/**
		 * A method that will provide us with the actual inputStream
		 */
		private Callable<InputStream> _inputStreamSupplier;

		public NGInputStreamSupplierResource( Callable<InputStream> inputStreamSupplier ) {
			_inputStreamSupplier = inputStreamSupplier;
		}

		/**
		 * @return The resource's data by opening a new inputStream provided by the InputStream supplier
		 */
		@Override
		public InputStream inputStream() {
			final Callable<InputStream> provider = _inputStreamSupplier;

			try {
				return provider.call();
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}
			catch( Exception e ) {
				throw new RuntimeException( e );
			}
		}
	}
}