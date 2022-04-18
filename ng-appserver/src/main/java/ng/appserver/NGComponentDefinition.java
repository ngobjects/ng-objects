package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.privates.NGUtils;

public class NGComponentDefinition {

	/**
	 * The cached name of this component definition. Corresponds to the component class's simpleName
	 */
	private String _name;

	public NGComponentDefinition( final Class<? extends NGComponent> componentClass ) {
		Objects.requireNonNull( componentClass );

		_name = componentClass.getSimpleName();
	}

	/**
	 * @return A new component of the given class in the given context
	 */
	public NGComponent componentInstanceInstanceInContext( final Class<? extends NGComponent> componentClass, final NGContext context ) {
		Objects.requireNonNull( componentClass );
		Objects.requireNonNull( context );

		try {
			return componentClass.getConstructor( NGContext.class ).newInstance( context );
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @return The parsed template for this component
	 */
	public NGElement template() {
		return parseTemplate( _name );
	}

	/**
	 * @return The parsed template for the named component
	 */
	public static NGElement parseTemplate( final String templateName ) {
		Objects.requireNonNull( templateName );

		final String htmlTemplateFilename = templateName + ".wo/" + templateName + ".html";

		final Optional<byte[]> templateBytes = NGUtils.readComponentResource( htmlTemplateFilename );

		if( templateBytes.isEmpty() ) {
			throw new RuntimeException( "Template not found" );
		}

		final String templateString = new String( templateBytes.get(), StandardCharsets.UTF_8 );
		return new NGHTMLBareString( templateString );
	}
}