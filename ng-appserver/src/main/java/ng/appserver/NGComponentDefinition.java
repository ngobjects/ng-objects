package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.privates.NGUtils;
import ng.appserver.templating.NGDeclarationFormatException;
import ng.appserver.templating.NGHTMLFormatException;
import ng.appserver.templating.NGTemplateParser;

public class NGComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger( NGComponentDefinition.class );

	/**
	 * The referenced component's class
	 */
	private final Class<? extends NGComponent> _componentClass;

	/**
	 * Construct a new component definition based on the given class.
	 *
	 * FIXME: We need to decide what parts of the component name/class name we're going to keep around // Hugi 2022-04-22
	 */
	public NGComponentDefinition( final Class<? extends NGComponent> componentClass ) {
		Objects.requireNonNull( componentClass );
		_componentClass = componentClass;
	}

	/**
	 * The cached name of this component definition. Corresponds to the component class's simpleName
	 */
	private String name() {
		return _componentClass.getSimpleName();
	};

	/**
	 * The fully qualified class name of this component definition.
	 */
	private String className() {
		return _componentClass.getName();
	}

	/**
	 * @return A new component of the given class in the given context
	 */
	public NGComponent componentInstanceInContext( final NGContext context ) {
		Objects.requireNonNull( context );

		try {
			final NGComponent newComponentInstance = _componentClass.getConstructor( NGContext.class ).newInstance( context );
			newComponentInstance._componentDefinition = this; // FIXME: Feel like this is ugly as all hell, the _componentDefinition variable should not be exposed
			return newComponentInstance;
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @return A new component reference to insert into a template being rendered
	 */
	public NGComponentReference componentReferenceWithAssociations( final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		return new NGComponentReference( className(), associations, contentTemplate );
	}

	/**
	 * @return The parsed template for this component
	 */
	public NGElement template() {
		try {
			// Let's try first for the traditional template
			String htmlTemplateString = loadHTMLTemplateString( name() );
			String wodString = loadWODTemplateString( name() );

			// If that fails, let's go for the single file html template
			// FIXME: this is not a good way to check for this. Check for existence of files and determine heuristics from there
			if( htmlTemplateString.isEmpty() && wodString.isEmpty() ) {
				final Optional<byte[]> htmlTemplate = NGUtils.readComponentResource( name() + ".html" );

				if( htmlTemplate.isPresent() ) {
					htmlTemplateString = new String( htmlTemplate.get(), StandardCharsets.UTF_8 );
				}
				else {
					htmlTemplateString = "";
				}
			}

			return NGTemplateParser.parse( htmlTemplateString, wodString, Collections.emptyList() );
		}
		catch( ClassNotFoundException | NGDeclarationFormatException | NGHTMLFormatException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @return The HTML template for the named component
	 */
	private static String loadHTMLTemplateString( final String templateName ) {
		Objects.requireNonNull( templateName );
		return loadTemplateFile( templateName, "html" );
	}

	/**
	 * @return The WOD template for the named component
	 */
	private static String loadWODTemplateString( final String templateName ) {
		Objects.requireNonNull( templateName );
		return loadTemplateFile( templateName, "wod" );
	}

	/**
	 * @return The string template for the named component
	 */
	private static String loadTemplateFile( final String templateName, final String extension ) {
		Objects.requireNonNull( templateName );
		Objects.requireNonNull( extension );

		final String htmlTemplateFilename = templateName + ".wo/" + templateName + "." + extension;

		final Optional<byte[]> templateBytes = NGUtils.readComponentResource( htmlTemplateFilename );

		if( templateBytes.isEmpty() ) {
			logger.warn( String.format( "Template file '%s.%s' not found", templateName, extension ) );
			return "";
		}

		return new String( templateBytes.get(), StandardCharsets.UTF_8 );
	}
}