package ng.appserver.templating;

public class PCommentNode implements PNode {

	private final String _value;

	public PCommentNode( final String value ) {
		_value = value;
	}

	public String value() {
		return _value;
	}
}