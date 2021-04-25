package ng.appserver.privates;

import java.util.Objects;
import java.util.Optional;

public class NGParsedURI {

	final String _sourceUri;

	private NGParsedURI( final String sourceUri ) {
		_sourceUri = sourceUri;
	}

	public static NGParsedURI of( final String sourceUri ) {
		Objects.requireNonNull( sourceUri );
		return new NGParsedURI( sourceUri );
	}

	public String[] elements() {
		String uri = _sourceUri;

		if( uri.startsWith( "/" ) ) {
			uri = uri.substring( 1 );
		}

		if( uri.length() == 0 ) {
			return new String[] {};
		}

		return uri.substring( 0 ).split( "/" );
	}

	/**
	 * FIXME: This should probably return an Optional at some point.
	 */
	public Optional<String> elementAt( final int i ) {

		if( i > elements().length ) {
			return Optional.empty();
		}

		return Optional.of( elements()[i] );
	}
}