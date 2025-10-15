package ng.appserver.templating.parser.model;

public sealed interface PNode permits PBasicNode, PRootNode, PHTMLNode, PCommentNode {}