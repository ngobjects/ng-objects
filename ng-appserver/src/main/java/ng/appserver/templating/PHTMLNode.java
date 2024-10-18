package ng.appserver.templating;

public class PHTMLNode implements PNode {

	private final String _value;

	public PHTMLNode( final String value ) {
		_value = value;
	}

	public String value() {
		return _value;
	}
}