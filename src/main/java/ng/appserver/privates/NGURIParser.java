package ng.appserver.privates;

import java.util.Objects;

public class NGURIParser {

	final String _sourceUri;

	public NGURIParser( final String sourceUri ) {
		Objects.requireNonNull( sourceUri );
		_sourceUri = sourceUri;
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
	public String elementAt( final int i ) {

		if( i > elements().length ) {
			return null;
		}

		return elements()[i];
	}
}