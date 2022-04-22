package ng.appserver.templating;

import java.util.Map;

import ng.appserver.NGAssociation;

/**
 * Represents a declaration of a dynamic tag
 */

public record NGDeclaration( String name, String type, Map<String, NGAssociation> associations ) {}