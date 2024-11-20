package ng.appserver.templating.parser;

public sealed interface PNode permits PBasicNode, PGroupNode, PHTMLNode, PCommentNode {}