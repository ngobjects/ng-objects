package ng.appserver.resources;

public enum StandardNamespace {
	App( "app" );

	private String _identifier;

	StandardNamespace( final String identifier ) {
		_identifier = identifier;
	}

	public String identifier() {
		return _identifier;
	}
}