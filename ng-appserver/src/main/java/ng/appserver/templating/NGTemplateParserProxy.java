package ng.appserver.templating;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGApplication;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.elements.NGHTMLCommentString;
import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;
import ng.appserver.templating.assications.NGAssociation;
import ng.appserver.templating.assications.NGAssociationFactory;
import ng.appserver.templating.parser.NGDeclarationFormatException;
import ng.appserver.templating.parser.NGHTMLFormatException;
import ng.appserver.templating.parser.NGTemplateParser;
import ng.appserver.templating.parser.model.PBasicNode;
import ng.appserver.templating.parser.model.PCommentNode;
import ng.appserver.templating.parser.model.PGroupNode;
import ng.appserver.templating.parser.model.PHTMLNode;
import ng.appserver.templating.parser.model.PNode;
import ng.xperimental.NGElementNotFoundElement;
import ng.xperimental.NGErrorMessageElement;

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
	public NGTemplateParserProxy( final String htmlString, final String wodString ) {
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
		final Map<String, NGAssociation> associations = toAssociations( node.bindings(), node.isInline() );
		final NGElement childTemplate = toTemplate( node.children() );

		try {
			return NGApplication.application().elementManager().dynamicElementWithName( NGElementManager.GLOBAL_UNNAMESPACED_NAMESPACE, type, associations, childTemplate );
		}
		catch( NGElementNotFoundException e ) {
			// FIXME: Experimental functionality, probably doesn't belong with the parser part of the framework.
			// But since it's definitely something we want, I'm keeping this here for reference until it finds it's final home. // Hugi 2024-10-19
			return new NGElementNotFoundElement( type );
		}
		catch( RuntimeException e ) {
			// FIXME: Digging this deep to get to the actual exception is crazy. We need to fix this up // Hugi 2024-11-19
			if( e.getCause() instanceof InvocationTargetException ite ) {
				if( ite.getCause() instanceof NGBindingConfigurationException bce ) {
					return new NGErrorMessageElement( "Binding configuration error", "&lt;wo:" + type + "&gt;", bce.getMessage() );
				}
			}

			throw e;
		}
	}

	private static Map<String, NGAssociation> toAssociations( final Map<String, NGBindingValue> bindings, final boolean isInline ) {
		final Map<String, NGAssociation> associations = new HashMap<>();

		for( Entry<String, NGBindingValue> entry : bindings.entrySet() ) {
			final String bindingName = entry.getKey();
			final NGBindingValue bindingValue = entry.getValue();
			final NGAssociation association = NGAssociationFactory.associationForBindingValue( bindingValue, isInline );
			associations.put( bindingName, association );
		}

		return associations;
	}

	/**
	 * @return An element/template from the given list of nodes.
	 */
	private static NGElement toTemplate( final List<PNode> nodes ) {

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