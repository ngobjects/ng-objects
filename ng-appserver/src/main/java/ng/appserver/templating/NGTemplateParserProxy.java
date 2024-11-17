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
 * Serves as a bridge between the "new and old world" for template parsing
 */

public class NGTemplateParserProxy {

	private final String _htmlString;
	private final String _wodString;

	public NGTemplateParserProxy( String htmlString, String wodString ) {
		Objects.requireNonNull( htmlString );
		Objects.requireNonNull( wodString );

		_htmlString = htmlString;
		_wodString = wodString;
	}

	public NGElement parse() throws NGDeclarationFormatException, NGHTMLFormatException {
		final PNode pNode = new NGTemplateParser( _htmlString, _wodString ).parse();
		return toDynamicElement( pNode );
	}

	private static NGElement toDynamicElement( final PNode node ) {
		return switch( node ) {
			case PBasicNode n -> toDynamicElement( n.tag() );
			case PGroupNode n -> childrenToTemplate( n.children() );
			case PHTMLNode n -> new NGHTMLBareString( n.value() );
			case PCommentNode n -> new NGHTMLCommentString( n.value() );
		};
	}

	private static NGElement toDynamicElement( final NGDynamicHTMLTag tag ) {
		try {
			return NGApplication.dynamicElementWithName( tag.declaration().type(), associationsFromDeclaration( tag.declaration() ), childrenToTemplate( tag.childrenWithStringsProcessedAndCombined() ), Collections.emptyList() );
		}
		catch( NGElementNotFoundException e ) {
			// FIXME:
			// Very experimental functionality at the moment and definitely does not belong with the parser part of the framework.
			// But since it's definitely something we want to add ad some point, I'm keeping it for reference
			// Hugi 2024-10-19
			return new NGElementNotFoundElement( tag.declaration().type() );
		}
	}

	private static Map<String, NGAssociation> associationsFromDeclaration( final NGDeclaration declaration ) {
		final Map<String, NGAssociation> associations = new HashMap<>();

		for( Entry<String, NGBindingValue> entry : declaration.bindings().entrySet() ) {
			associations.put( entry.getKey(), NGAssociationFactory.toAssociation( entry.getValue(), declaration.isInline() ) );
		}

		return associations;
	}

	/**
	 * @return The tag's template
	 */
	private static NGElement childrenToTemplate( final List<PNode> children ) {

		// FIXME: Children should never really be null. I'm still hesitant to replace it with an empty list though, since in my mind that represents an empty container tag. Food for thought... // Hugi 2024-11-15
		if( children == null ) {
			return null;
		}

		final List<NGElement> childElements = new ArrayList<>();

		for( final PNode pNode : children ) {
			childElements.add( toDynamicElement( pNode ) );
		}

		if( childElements.size() == 1 ) {
			return childElements.getFirst();

			// FIXME: OK, I can  kind of understand why we unwrap the single element. But why on earth wrap a component reference in a dynamic group? (legacy from WOOgnl). Disabled // Hugi 2024-11-15
			//			final NGElement onlyElement = childElements.get( 0 );
			//
			//			if( onlyElement instanceof NGComponentReference ) {
			//				return new NGDynamicGroup( null, null, onlyElement );
			//			}
			//
			//			return onlyElement;
		}

		return NGDynamicGroup.of( childElements );
	}
}