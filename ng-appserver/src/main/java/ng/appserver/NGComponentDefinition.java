package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ng.appserver.templating.NGHelperFunctionDeclarationFormatException;
import ng.appserver.templating.NGHelperFunctionHTMLFormatException;
import ng.appserver.templating.NGHelperFunctionParser;
import ng.appserver.templating._NSDictionary;

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
		try {
			final String htmlTemplateString = NGComponentTemplateParser.loadTemplateString( _name );
			final String wodString = "";
			final List<String> languages = Collections.emptyList();
			return new NGHelperFunctionParser( htmlTemplateString, wodString, languages ).parse();
		}
		catch( ClassNotFoundException | NGHelperFunctionDeclarationFormatException | NGHelperFunctionHTMLFormatException e ) {
			throw new RuntimeException( e );
		}
	}

	public NGComponentReference componentReferenceWithAssociations( _NSDictionary<String, NGAssociation> nsdictionary, NGElement woelement ) {
		throw new RuntimeException( "Not implemented" );
	}
}