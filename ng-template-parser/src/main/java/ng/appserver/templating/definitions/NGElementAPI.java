package ng.appserver.templating.definitions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An element, as its {@code .apiext} file describes it: its class (none for a template-only component), what it does,
 * whether it wraps content, what happens to attributes it doesn't declare, and its bindings. Docs are Markdown, returned as written.
 *
 * It doesn't read cross-binding constraints ({@code <choose>}, {@code <requires>}) yet, and nothing here validates a
 * template.
 *
 * This is the format only, shared by the runtimes and tooling: where a runtime finds an element's file is its own
 * business.
 */

public record NGElementAPI( String className, String doc, String content, String unknownAttributes, String deprecated, List<Binding> bindings ) {

	/**
	 * A binding: its name, whether it must be bound, the types it pulls (reads) and pushes (writes back), its default,
	 * its docs, and its deprecation note (null when it isn't deprecated)
	 */
	public record Binding( String name, boolean required, List<String> pullTypes, List<String> pushTypes, String defaultValue, String doc, String deprecated ) {

		/**
		 * @return true if the element reads the binding's value
		 */
		public boolean isPulled() {
			return !pullTypes.isEmpty();
		}

		/**
		 * @return true if the element writes a value back to the binding
		 */
		public boolean isPushed() {
			return !pushTypes.isEmpty();
		}

		public boolean isDeprecated() {
			return deprecated != null;
		}
	}

	/**
	 * @return true if the element wraps content (a container element)
	 */
	public boolean wrapsContent() {
		return content != null && !"forbidden".equals( content );
	}

	public boolean isDeprecated() {
		return deprecated != null;
	}

	/**
	 * Parses an {@code .apiext} file
	 *
	 * @param source Where the stream comes from, for error messages
	 * @throws IllegalArgumentException if the file isn't readable as one
	 */
	public static NGElementAPI parse( final InputStream stream, final String source ) {
		final Document document;

		try {
			document = documentBuilder().parse( stream );
		}
		catch( SAXException | IOException e ) {
			throw new IllegalArgumentException( "%s: not a readable .apiext file: %s".formatted( source, e.getMessage() ), e );
		}

		final Element wo = child( document.getDocumentElement(), "wo" );

		if( wo == null ) {
			throw new IllegalArgumentException( "%s: no <wo> element".formatted( source ) );
		}

		final List<Binding> bindings = new ArrayList<>();

		for( final Element binding : children( wo, "binding" ) ) {
			// A <binding> with no children is a reference inside a constraint, not a declaration; those sit inside
			// <choose>/<requires>, so only direct children are read here
			bindings.add( new Binding(
					attribute( binding, "name" ),
					"true".equals( attribute( binding, "required" ) ),
					types( child( binding, "pull" ) ),
					types( child( binding, "push" ) ),
					text( child( binding, "default" ) ),
					text( child( binding, "doc" ) ),
					text( child( binding, "deprecated" ) ) ) );
		}

		return new NGElementAPI( className( attribute( wo, "class" ) ), text( child( wo, "doc" ) ), attribute( wo, "content" ), attribute( wo, "unknownAttributes" ), text( child( wo, "deprecated" ) ), List.copyOf( bindings ) );
	}

	/**
	 * @return The element's class, when the {@code class} attribute names one fully qualified. A simple name (how
	 * WebObjects' own element files name their elements) or no attribute (a template-only component) is no class.
	 */
	private static String className( final String classAttribute ) {
		return classAttribute != null && classAttribute.contains( "." ) ? classAttribute : null;
	}

	/**
	 * @return The class names in a {@code <pull>} or {@code <push>}
	 */
	private static List<String> types( final Element direction ) {
		if( direction == null ) {
			return List.of();
		}

		return children( direction, "type" ).stream().map( NGElementAPI::text ).filter( Objects::nonNull ).toList();
	}

	/**
	 * @return The element's first direct child with the given name, or null
	 */
	private static Element child( final Element parent, final String name ) {
		final List<Element> children = children( parent, name );
		return children.isEmpty() ? null : children.get( 0 );
	}

	/**
	 * @return The element's direct children with the given name
	 */
	private static List<Element> children( final Element parent, final String name ) {
		final List<Element> result = new ArrayList<>();
		final NodeList nodes = parent.getChildNodes();

		for( int i = 0; i < nodes.getLength(); i++ ) {
			if( nodes.item( i ) instanceof Element e && name.equals( e.getTagName() ) ) {
				result.add( e );
			}
		}

		return result;
	}

	/**
	 * @return The element's trimmed text (CDATA included), or null for a missing element or an empty one. A
	 * {@code <deprecated/>} with no note is deprecated nonetheless, so it answers an empty string rather than null.
	 */
	private static String text( final Element element ) {
		if( element == null ) {
			return null;
		}

		final String text = element.getTextContent().strip();

		if( text.isEmpty() ) {
			return "deprecated".equals( element.getTagName() ) ? "" : null;
		}

		return text;
	}

	/**
	 * @return The attribute's trimmed value, or null when it's absent or blank
	 */
	private static String attribute( final Element node, final String name ) {
		if( !node.hasAttribute( name ) ) {
			return null;
		}

		final String value = node.getAttribute( name ).strip();
		return value.isEmpty() ? null : value;
	}

	/**
	 * @return A parser that doesn't resolve external entities or DTDs
	 */
	private static DocumentBuilder documentBuilder() {
		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true );
			factory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
			factory.setExpandEntityReferences( false );
			return factory.newDocumentBuilder();
		}
		catch( ParserConfigurationException e ) {
			throw new IllegalStateException( e );
		}
	}
}
