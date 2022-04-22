package ng.appserver;

import java.util.Map;

public class NGComponentReference extends NGDynamicElement {

	private final String _name;
	private Map<String, NGAssociation> _associations;
	private final NGElement _template;

	public NGComponentReference( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_name = name;
		_associations = associations;
		_template = template;

	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		_template.appendToResponse( response, context );
	}
}