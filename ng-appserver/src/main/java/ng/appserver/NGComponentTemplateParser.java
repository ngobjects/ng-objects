package ng.appserver;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.privates.NGUtils;

public class NGComponentTemplateParser {

	/**
	 * @return The string template for the named component
	 */
	public static String loadTemplateString( final String templateName ) {
		Objects.requireNonNull( templateName );

		final String htmlTemplateFilename = templateName + ".wo/" + templateName + ".html";

		final Optional<byte[]> templateBytes = NGUtils.readComponentResource( htmlTemplateFilename );

		if( templateBytes.isEmpty() ) {
			throw new RuntimeException( "Template not found" );
		}

		return new String( templateBytes.get(), StandardCharsets.UTF_8 );
	}

	/**
	 * @return The parsed template for the named component
	 */
	public static NGElement parseTemplate( final String templateName ) {
		Objects.requireNonNull( templateName );

		return new NGHTMLBareString( loadTemplateString( templateName ) );
	}
}
