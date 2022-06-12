package ng.appserver;

import java.util.Map;

public abstract class NGDynamicElement extends NGElement {

	/**
	 * FIXME: I'm not sure what to do with the [name] parameter, added it since it's a part of the WO APIs // Hugi 2022-04-18
	 */
	public NGDynamicElement( String name, Map<String, NGAssociation> associations, NGElement template ) {}

	/**
	 * @return The API description of this element
	 *
	 * FIXME: I don't want to return null by default // Hugi 2022-06-12
	 *
	 * The description class might perhaps be better provided by an interface that can be optionally implemented by any classes that extend WOElement (including components)
	 */
	public NGDynamicElementDescription dynamicElementDescription() {
		return null;
	}
}