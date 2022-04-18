package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

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
		return NGComponentTemplateParser.parseTemplate( _name );
	}
}