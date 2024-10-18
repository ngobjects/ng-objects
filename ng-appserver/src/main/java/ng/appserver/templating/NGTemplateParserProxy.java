package ng.appserver.templating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGAssociationFactory;
import ng.appserver.NGComponentReference;
import ng.appserver.NGElement;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.elements.NGHTMLCommentString;
import ng.appserver.templating.NGDeclaration.NGBindingValue;

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

	private static NGElement toDynamicElement( PNode node ) throws NGDeclarationFormatException {
		NGElement element = switch( node ) {
			case PBasicNode n -> toDynamicElement( n.tag(), n.declaration() );
			case PGroupNode n -> new NGDynamicGroup( null, null, template( n.tag() ) );
			case PHTMLNode n -> new NGHTMLBareString( n.value() );
			case PCommentNode n -> new NGHTMLCommentString( n.value() );
			/*
			default -> {
				throw new IllegalStateException( "Unhandled node type: " + node );
			}*/
		};

		return element;
	}

	private static NGElement toDynamicElement( NGDynamicHTMLTag tag, NGDeclaration declaration ) throws NGDeclarationFormatException {
		return NGApplication.dynamicElementWithName( declaration.type(), toAssociations( declaration ), template( tag ), Collections.emptyList() );
	}

	public static Map<String, NGAssociation> toAssociations( NGDeclaration declaration ) {
		final Map<String, NGAssociation> associations = new HashMap<>();

		for( Entry<String, NGBindingValue> entry : declaration.bindings().entrySet() ) {
			associations.put( entry.getKey(), toAssociation( declaration, entry.getValue() ) );
		}

		return associations;
	}

	private static NGAssociation toAssociation( NGDeclaration declaration, NGBindingValue bindingValue ) {

		if( declaration.isInline() ) {
			try {
				return bindingValueForInlineBindingString( bindingValue.value() );
			}
			catch( NGHTMLFormatException e ) {
				throw new RuntimeException( e );
			}
		}
		else {
			return NGAssociationFactory.associationWithValue( bindingValue.value(), bindingValue.isQuoted() );
		}
	}

	public static NGAssociation bindingValueForInlineBindingString( String value ) throws NGHTMLFormatException {
		Objects.requireNonNull( value );

		if( value.startsWith( "\"" ) ) {
			value = value.substring( 1 );

			if( value.endsWith( "\"" ) ) {
				value = value.substring( 0, value.length() - 1 );
			}
			else {
				throw new NGHTMLFormatException( value + " starts with quote but does not end with one." );
			}

			if( value.startsWith( "$" ) ) {
				value = value.substring( 1 );

				if( value.endsWith( "VALID" ) ) {
					value = value.replaceFirst( "\\s*//\\s*VALID", "" );
				}

				return NGAssociationFactory.associationWithValue( value, false );
			}
			else {
				value = value.replaceAll( "\\\\\\$", "\\$" );
				value = value.replaceAll( "\\\"", "\"" );
				return NGAssociationFactory.associationWithValue( value, true );
			}
		}

		return NGAssociationFactory.associationWithValue( value, false );
	}

	/**
	 * @return The tag's template
	 * @throws NGDeclarationFormatException
	 */
	public static NGElement template( NGDynamicHTMLTag tag ) throws NGDeclarationFormatException {

		if( tag.children() == null ) {
			return null;
		}

		final List<PNode> childNodes = combineAndWrapBareStrings( tag.children() );
		final List<NGElement> childElements = new ArrayList<>();

		for( PNode pNode : childNodes ) {
			childElements.add( toDynamicElement( pNode ) );
		}

		if( childElements.size() == 1 ) {
			final NGElement onlyElement = childElements.get( 0 );

			if( onlyElement instanceof NGComponentReference ) {
				return new NGDynamicGroup( tag.declarationName(), null, onlyElement );
			}

			return onlyElement;
		}

		return new NGDynamicGroup( tag.declarationName(), null, childElements );
	}

	/**
	 * Iterates through the list, combining adjacent strings and wrapping them in NGHTMLBareStrings
	 * Other elements get added directly to the element list.
	 */
	private static List<PNode> combineAndWrapBareStrings( List<Object> children ) {
		final List<PNode> childElements = new ArrayList<>( children.size() );

		final StringBuilder sb = new StringBuilder( 128 );

		for( final Object currentChild : children ) {

			if( currentChild instanceof String ) {
				// If we encounter a string, we append it to the StringBuilder
				sb.append( (String)currentChild );
			}
			else {
				// If we encounter any other element and we still have unwrapped strings in our builder,
				// we take the string data we've collected, wrap it up and add it to the element list.
				if( sb.length() > 0 ) {
					final PHTMLNode bareString = new PHTMLNode( sb.toString() );
					childElements.add( bareString );
					sb.setLength( 0 );
				}

				// ... and then add the element itself
				childElements.add( (PNode)currentChild );
			}
		}

		// If the last element happened to be a string, the StringBuilder will still have data so we wrap it here
		if( sb.length() > 0 ) {
			final PHTMLNode bareString = new PHTMLNode( sb.toString() );
			childElements.add( bareString );
			sb.setLength( 0 );
		}

		return childElements;
	}
}