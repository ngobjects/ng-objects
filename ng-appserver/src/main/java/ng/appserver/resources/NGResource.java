package ng.appserver.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;

/**
 * FIXME: We're currently missing caching of the resource's data // Hugi 2024-06-22
 */

public class NGResource {

	/**
	 * A method that will provide us with the actual inputStream
	 */
	private Callable<InputStream> _inputStreamSupplier;

	/**
	 * @return A resource that obtains it's data from the given supplier
	 */
	public static NGResource of( final Callable<InputStream> inputStreamSupplier ) {
		final NGResource resource = new NGResource();
		resource._inputStreamSupplier = inputStreamSupplier;
		return resource;
	}

	/**
	 * @return The resource's data as a byte array
	 */
	public byte[] bytes() {

		try( final InputStream is = inputStream()) {
			return is.readAllBytes();
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * @return The resource's data by opening a new inputStream provided by the InputStream supplier
	 */
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