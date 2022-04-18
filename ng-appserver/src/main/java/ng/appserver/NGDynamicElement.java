package ng.appserver;

import java.util.Map;

public abstract class NGDynamicElement extends NGElement {

	/**
	 * FIXME: Not sure what to do with the [name] parameter, only added it since it's a part of the WO APIs // Hugi 2022-04-18
	 */
	public NGDynamicElement( String name, Map<String, NGAssociation> associations, NGElement template ) {}
}