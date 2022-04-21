package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import er.extensions.bettertemplates.NGHelperFunctionDeclarationFormatException;
import er.extensions.bettertemplates.NGHelperFunctionHTMLFormatException;
import er.extensions.bettertemplates.NGHelperFunctionParser;
import er.extensions.bettertemplates.NSArray;
import er.extensions.bettertemplates.NSDictionary;

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
		//		return NGComponentTemplateParser.parseTemplate( _name );
		try {
			return new NGHelperFunctionParser( NGComponentTemplateParser.loadTemplateString( _name ), "", new NSArray() ).parse();
		}
		catch( ClassNotFoundException | NGHelperFunctionDeclarationFormatException | NGHelperFunctionHTMLFormatException e ) {
			throw new RuntimeException( e );
		}
	}

	public NGComponentReference componentReferenceWithAssociations( NSDictionary nsdictionary, NGElement woelement ) {
		throw new RuntimeException( "Not implemented" );
	}
}