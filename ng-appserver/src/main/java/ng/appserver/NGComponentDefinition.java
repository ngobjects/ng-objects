package ng.appserver;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.privates.NGUtils;

public class NGComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger( NGComponentDefinition.class );

	public static NGElement parseTemplate( final String templateName ) {
		final String htmlTemplateFilename = templateName + ".wo/" + templateName + ".html";
		final String htmlTemplatePath = NGUtils.resourcePath( "components", htmlTemplateFilename );
		logger.debug( "Locating component at: " + htmlTemplatePath );

		final Optional<byte[]> templateBytes = NGUtils.readJavaResource( htmlTemplatePath );

		if( templateBytes.isEmpty() ) {
			throw new RuntimeException( "Template not found" );
		}

		final String templateString = new String( templateBytes.get(), StandardCharsets.UTF_8 );
		return new NGHTMLBareString( templateString );
	}
}