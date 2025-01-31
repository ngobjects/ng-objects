package ng.appserver.resources;

public enum StandardNamespace {
	App( "app" ),
	NG( "ng" );

	private String _identifier;

	StandardNamespace( final String identifier ) {
		_identifier = identifier;
	}

	public String identifier() {
		return _identifier;
	}
}