package ng.xperimental;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGElement;
import ng.dev.NGRuntimeProblems;

public class NGErrorMessageElement implements NGElement {

	private final String _type;
	private final String _heading;
	private final String _message;

	public NGErrorMessageElement( final String type, final String heading, final String message ) {
		_type = type;
		_heading = heading;
		_message = message;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		// Record the problem so /ng/dev/problems can serve it back — a tool notices binding errors
		// without scraping the rendered page. Both error paths (parse-time binding-configuration
		// errors and render-time unknown-key errors) construct this element, so this one call site
		// captures them all. The heading carries the element/component involved.
		//
		// The heading is pre-HTML-escaped for safe rendering into the page (e.g. "&lt;wo:link&gt;");
		// the recorded value is DATA, not markup, so un-escape it — otherwise a JSON consumer sees
		// the double-encoded "&lt;wo:link&gt;" instead of "<wo:link>".
		NGRuntimeProblems.record( _type, unescapeHtml( _heading ), _message );

		response.appendContentString( """
				<span style="display: inline-block; margin: 10px; padding: 10px; color:white; background-color: rgba(255,100,100,0.5); border: 1px solid red; border-radius: 5px; box-shadow: 5px 5px 0px rgba(0,0,200,0.8); font-size: 12px;">
					<span style="font-size: 20px">%s</span> %s<br>
					<span style="font-size: 16px"><strong>%s</strong></span><br>
					%s
				</span>
				""".formatted( u( "DOG FACE" ), _type, _heading, _message ) );
	}

	/**
	 * @return The unicode character corresponding to the given character name
	 */
	private static String u( final String unicodeCharacterName ) {
		final int codePoint = Character.codePointOf( unicodeCharacterName );
		return Character.toString( codePoint ).toString();
	}

	/**
	 * Reverses the small set of HTML entities the error headings use, so the RECORDED (data) form is
	 * the semantic string rather than escaped markup. Only the entities actually emitted here.
	 */
	private static String unescapeHtml( final String value ) {
		if( value == null || value.indexOf( '&' ) < 0 ) {
			return value;
		}
		return value
				.replace( "&lt;", "<" )
				.replace( "&gt;", ">" )
				.replace( "&quot;", "\"" )
				.replace( "&#39;", "'" )
				.replace( "&amp;", "&" ); // last, so "&amp;lt;" -> "&lt;" not "<"
	}
}