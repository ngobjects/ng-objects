package ng.appserver.templating.parser.model;

public sealed interface PNode permits PBasicNode, PRootNode, PHTMLNode, PRawNode, PCommentNode {

	/**
	 * @return The range in the source template that this node was parsed from
	 */
	SourceRange sourceRange();
}
