package ng.appserver.templating.parser.model;

public sealed interface PNode permits PBasicNode, PGroupNode, PHTMLNode, PCommentNode {}