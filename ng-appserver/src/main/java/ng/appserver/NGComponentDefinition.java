package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.privates.NGUtils;

public class NGComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger( NGComponentDefinition.class );

	/**
	 * THe name of this component definition corresponds to the name of the component.
	 */
	private String _name;

	public NGComponentDefinition( final Class<? extends NGComponent> componentClass ) {
		_name = componentClass.getSimpleName();
	}

	public NGElement template() {
		return parseTemplate( _name );
	}

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
	
	public NGComponent componentInstanceInstanceInContext( final Class<? extends NGComponent> componentClass, NGContext context ) {
		try {
			return componentClass.getConstructor( NGContext.class ).newInstance( context );
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( e );
		}
	}
}