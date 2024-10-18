package ng.appserver.templating;

public sealed interface PNode permits PBasicNode, PGroupNode, PHTMLNode, PCommentNode {}