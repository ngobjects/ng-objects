package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * URL rewrite patterns are applied to a request's URI before the request is created
 */
public class TestNGApplicationURLRewrite {

	/**
	 * The pattern NGWOIntegrationPlugin adds, for URLs that come through the WO adaptor
	 */
	private static final List<Pattern> WO_ADAPTOR = List.of( Pattern.compile( "^/(cgi-bin|Apps)/WebObjects/(.*).woa(/[0-9])?" ) );

	@Test
	public void theWOAdaptorPathIsRemoved() {
		assertEquals( "/wo/1.0", NGApplication.rewrittenURI( "/Apps/WebObjects/App.woa/wo/1.0", WO_ADAPTOR ) );
		assertEquals( "/wo/1.0", NGApplication.rewrittenURI( "/cgi-bin/WebObjects/App.woa/wo/1.0", WO_ADAPTOR ) );
	}

	@Test
	public void theInstanceNumberIsRemovedWithIt() {
		assertEquals( "/wo/1.0", NGApplication.rewrittenURI( "/Apps/WebObjects/App.woa/1/wo/1.0", WO_ADAPTOR ) );
	}

	@Test
	public void theAppsOwnURLIsTheRoot() {
		assertEquals( "/", NGApplication.rewrittenURI( "/Apps/WebObjects/App.woa", WO_ADAPTOR ) );
	}

	@Test
	public void aURIThatDoesntMatchIsLeftAlone() {
		assertEquals( "/wo/1.0", NGApplication.rewrittenURI( "/wo/1.0", WO_ADAPTOR ) );
		assertEquals( "/", NGApplication.rewrittenURI( "/", List.of() ) );
	}
}
