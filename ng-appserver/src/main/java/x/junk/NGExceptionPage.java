package x.junk;

import ng.appserver.NGContext;

/**
 * Exception page used when not in development mode
 */

public class NGExceptionPage extends NGExceptionPageDevelopment {

	public NGExceptionPage( NGContext context ) {
		super( context );
	}
}