package ng.appserver;

import java.util.Map;

public class NGComponentReference extends NGDynamicElement {

	public NGComponentReference( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
	}
}