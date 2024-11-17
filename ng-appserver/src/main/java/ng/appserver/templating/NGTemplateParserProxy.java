package ng.appserver.templating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGApplication;
import ng.appserver.NGApplication.NGElementNotFoundException;
import ng.appserver.NGAssociation;
import ng.appserver.NGAssociationFactory;
import ng.appserver.NGElement;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.elements.NGHTMLCommentString;
import ng.appserver.templating.NGDeclaration.NGBindingValue;
import x.junk.NGElementNotFoundElement;

/**
 * Bridges the "new and old world" for template parsing
 */

public class NGTemplateParserProxy {

	private final String _htmlString;
	private final String _wodString;

	/**
	 * @param htmlString The HTML to parse
	 * @param wodString The associated wod/declarations
	 */
	public NGTemplateParserProxy( String htmlString, String wodString ) {
		Objects.requireNonNull( htmlString );
		Objects.requireNonNull( wodString );

		_htmlString = htmlString;
		_wodString = wodString;
	}

	/**
	 * @return A parsed element template
	 */
	public NGElement parse() throws NGDeclarationFormatException, NGHTMLFormatException {
		final PNode rootNode = new NGTemplateParser( _htmlString, _wodString ).parse();
		return toDynamicElement( rootNode );
	}

	private static NGElement toDynamicElement( final PNode node ) {
		return switch( node ) {
			case PBasicNode n -> toDynamicElement( n );
			case PGroupNode n -> toTemplate( n.children() );
			case PHTMLNode n -> new NGHTMLBareString( n.value() );
			case PCommentNode n -> new NGHTMLCommentString( n.value() );
		};
	}

	private static NGElement toDynamicElement( final PBasicNode node ) {

		final String type = node.type();
		final Map<String, NGAssociation> associations = associationsFromDeclaration( node.bindings(), node.isInline() );
		final NGElement childTemplate = toTemplate( node.children() );

		try {
			return NGApplication.dynamicElementWithName( type, associations, childTemplate, Collections.emptyList() );
		}
		catch( NGElementNotFoundException e ) {
			// FIXME: Experimental functionality, probably doesn't belong with the parser part of the framework.
			// But since it's definitely something we want, I'm keeping this here for reference until it finds it's final home. // Hugi 2024-10-19
			return new NGElementNotFoundElement( type );
		}
	}

	private static Map<String, NGAssociation> associationsFromDeclaration( final Map<String, NGBindingValue> bindings, final boolean isInline ) {
		final Map<String, NGAssociation> associations = new HashMap<>();

		for( Entry<String, NGBindingValue> entry : bindings.entrySet() ) {
			associations.put( entry.getKey(), NGAssociationFactory.toAssociation( entry.getValue(), isInline ) );
		}

		return associations;
	}

	/**
	 * @return An element/template from the given list of nodes.
	 */
	private static NGElement toTemplate( final List<PNode> nodes ) {

		// FIXME: Shouldn't really ever be null. Still hesitant to replace it with an empty list though, since in my mind that represents an empty container tag. Food for thought... // Hugi 2024-11-15
		if( nodes == null ) {
			return null;
		}

		final List<NGElement> elements = new ArrayList<>();

		for( final PNode pNode : nodes ) {
			elements.add( toDynamicElement( pNode ) );
		}

		if( elements.size() == 1 ) {
			return elements.getFirst();
		}

		return NGDynamicGroup.of( elements );
	}
}