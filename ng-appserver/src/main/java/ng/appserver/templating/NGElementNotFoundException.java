package ng.appserver.templating;

public class NGElementNotFoundException extends RuntimeException {

	private final String _elementName;

	public NGElementNotFoundException( final String message, final String elementName ) {
		super( message );
		_elementName = elementName;
	}

	public String elementName() {
		return _elementName;
	}
}