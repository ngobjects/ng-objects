package ng.appserver;

import java.util.List;

/**
 * FIXME: This class should probably not exist. Only for experimental AJAX functionality // Hugi 2024-03-16
 */

public class NGAjaxResponse extends NGResponse {

	private NGContext _context;

	public NGAjaxResponse( NGContext context ) {
		_context = context;
	}

	@Override
	public void appendContentString( String stringToAppend ) {
		//		if( shouldAppendToResponse( _context ) ) {
		super.appendContentString( stringToAppend );
		//		}
	}

	/**
	 * @return true if the context is currently working inside an updateContainer meant to be updated.
	 *
	 * FIXME: The best solution would be to not invoke appendToResponse() at all on components outside the rendering scope. Introduce conditional rendering in NGElement? // Hugi 2024-05-08
	 */
	public static boolean shouldAppendToResponse( NGContext context ) {
		final List<String> ucHeader = context.request().headers().get( "x-updatecontainerid" );

		if( ucHeader != null && !ucHeader.isEmpty() ) {
			if( !context.updateContainerIDs.contains( ucHeader.get( 0 ) ) ) {
				return false;
			}
		}

		return true;
	}
}