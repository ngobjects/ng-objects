package ng.xperimental;

import ng.appserver.NGContext;
import ng.appserver.http.NGResponse;
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

		// Lands in pages with unknown CSS, so it carries its own styles and resets what it would inherit
		response.appendContentString( """
				<span style="all: initial; position: relative; display: inline-flex; align-items: stretch; max-width: 36rem; margin: 20px 14px 6px 0; vertical-align: middle; box-sizing: border-box; border-radius: 6px; background: #c41e4a; border: 2px solid #121a17; box-shadow: 4px 4px 0 #121a17; font: 400 13px/1.45 -apple-system, 'Segoe UI', Helvetica, Arial, sans-serif; color: #fff; text-align: left;">
					<span style="flex: none; width: 10px; border-radius: 4px 0 0 4px; background: repeating-linear-gradient(-45deg, #ffb320 0 6px, #121a17 6px 12px);"></span>
					<span style="position: absolute; top: -24px; right: -14px; transform: rotate(8deg); filter: drop-shadow(2px 2px 0 #121a17);">%s</span>
					<span style="padding: 7px 34px 8px 12px;"><span style="font-weight: 700;">%s</span> <code style="all: initial; font: 700 12px/1.45 ui-monospace, Menlo, Consolas, monospace; color: #ffd27a;">%s</code><span style="display: block; color: #fff;">%s</span></span>
				</span>
				""".formatted( SNAPPED_GINGER, _type, _heading, _message ) );
	}

	/**
	 * The engifer root, snapped in two at its middle joint. 40px, drawn by logo-explorations/errors.py on the ng-website
	 */
	private static final String SNAPPED_GINGER = """
			<svg width="40" height="40" aria-hidden="true" style="display:block" xmlns="http://www.w3.org/2000/svg" viewBox="2 2 60 60"><defs><clipPath id="ng-snap-l"><path d="M0 0 L30.6 0 L30.6 33 L29.2 36.5 L30.8 39.5 L29.4 42.5 L30.9 45.5 L29.5 48.5 L30.5 52 L30 57 L0 64Z" /></clipPath><clipPath id="ng-snap-r"><path d="M64 0 L30.6 0 L30.6 33 L29.2 36.5 L30.8 39.5 L29.4 42.5 L30.9 45.5 L29.5 48.5 L30.5 52 L30 57 L64 64Z" /></clipPath></defs><g transform="translate(-1.6 0.5) rotate(-9 30 44)"><g clip-path="url(#ng-snap-l)"><g fill="#e5bd66"><path d="M9.8 39.0 L7.1 39.5 L4.7 41.1 L3.1 43.3 L2.5 46.0 L2.8 48.7 L4.1 51.2 L6.2 53.0 L8.9 53.9 L11.6 53.8 L31.2 49.3 L32.8 48.6 L34.2 47.5 L35.1 45.9 L35.5 44.2 L35.2 42.4 L34.4 40.8 L33.2 39.5 L31.6 38.7 L29.8 38.5Z" /><path d="M20.7 28.3 L20.0 26.2 L18.6 24.5 L16.7 23.4 L14.5 23.2 L12.3 23.8 L10.6 25.1 L9.5 27.1 L9.2 29.3 L9.7 31.4 L15.8 44.4 L16.4 45.3 L17.3 46.0 L18.3 46.4 L19.4 46.4 L20.4 46.1 L21.3 45.5 L22.0 44.7 L22.4 43.6 L22.4 42.5Z" /><circle cx="24.5" cy="47.5" r="2.6" /><circle cx="12.5" cy="34" r="2.2" /><circle cx="6" cy="44" r="2.4" /></g><g fill="none" stroke="#b98a3e" stroke-width="1.4" stroke-linecap="round"><path d="M21.4 48.9 Q19.6 45.2 20.5 41.3" /><path d="M14.3 36.0 Q16.5 34.4 19.2 34.6" /></g><ellipse cx="10.0" cy="46.5" rx="3.1" ry="6.9" transform="rotate(172.9 10.0 46.5)" fill="#f3e6a4" stroke="#d9c56e" stroke-width="1" /><path d="M30.6 37.5 L29.6 40 L30.8 42.5 L29.8 45 L30.9 47.5 L30.2 50.5" fill="none" stroke="#f3e6a4" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round" /></g><g stroke="#d9c56e" stroke-width="0.8" stroke-linecap="round"><path d="M30.4 40.5 l1.8 -0.4" /><path d="M30.6 44.2 l2.2 0.3" /><path d="M30.4 48 l1.6 0.5" /></g></g><g transform="translate(4 0.5) rotate(7 30 44)"><g clip-path="url(#ng-snap-r)"><g fill="#e5bd66"><path d="M49.7 47.4 L52.1 46.7 L54.2 45.1 L55.5 43.0 L56.0 40.5 L55.5 38.0 L54.2 35.8 L52.1 34.2 L49.7 33.5 L47.2 33.7 L28.5 38.6 L26.9 39.4 L25.6 40.6 L24.7 42.2 L24.5 44.0 L24.8 45.8 L25.7 47.4 L27.0 48.6 L28.7 49.3 L30.5 49.4Z" /><path d="M46.1 27.7 L46.7 25.3 L46.4 22.9 L45.2 20.8 L43.3 19.3 L41.0 18.7 L38.6 18.9 L36.4 20.1 L34.9 22.0 L34.2 24.3 L32.5 40.6 L32.5 41.7 L32.9 42.7 L33.6 43.5 L34.5 44.1 L35.5 44.4 L36.6 44.4 L37.6 44.0 L38.5 43.4 L39.1 42.5Z" /><path d="M49.5 53.2 L50.4 54.7 L51.8 55.8 L53.6 56.2 L55.4 56.0 L56.9 55.2 L58.1 53.8 L58.7 52.2 L58.6 50.4 L57.9 48.7 L51.8 40.0 L51.1 39.2 L50.1 38.6 L49.0 38.5 L47.8 38.6 L46.8 39.2 L46.1 40.0 L45.6 41.0 L45.5 42.1 L45.7 43.2Z" /><circle cx="43.5" cy="31.5" r="2.3" /></g><g fill="none" stroke="#b98a3e" stroke-width="1.4" stroke-linecap="round"><path d="M38.8 38.7 Q40.7 42.0 40.1 45.7" /><path d="M35.6 32.2 Q38.5 32.0 40.9 33.7" /></g><path d="M40.5 21 C40 16.5 41.5 13 44 10.5" fill="none" stroke="#4f8a4f" stroke-width="3.4" stroke-linecap="round" /><path d="M0 0 C5.2 -3.2 3.8 -9.7 0 -13.0 C-3.8 -9.7 -5.2 -3.2 0 0Z" fill="#4f8a4f" transform="translate(43.5 11) rotate(48)" /><path d="M30.6 37.5 L29.6 40 L30.8 42.5 L29.8 45 L30.9 47.5 L30.2 50.5" fill="none" stroke="#f3e6a4" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round" /></g><g stroke="#d9c56e" stroke-width="0.8" stroke-linecap="round"><path d="M30 42 l-1.8 -0.5" /><path d="M30.2 46.2 l-2 0.4" /></g></g></svg>""";

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