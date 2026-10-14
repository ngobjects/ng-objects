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
 * A library's published element contract, as declared in a {@value #FILENAME} file in its components folder: the
 * elements it publishes, by name. Everything else in the library is internal: it still renders, but tooling treats it
 * as not offered, and documentation leaves it out.
 *
 * <pre>
 * &lt;taglib&gt;
 * 	&lt;element name="AjaxUpdateLink"/&gt;
 * 	&lt;element name="AjaxUpdateContainer"/&gt;
 * &lt;/taglib&gt;
 * </pre>
 *
 * The file is optional. A library without one publishes every element it has: each {@code .apiext} in its components
 * folder, and its element classes. An element can be published without an {@code .apiext}: it's then undocumented.
 * What an element is (its class, its bindings and docs) is in its {@code .apiext}; what templates call it
 * is in tag alias files.
 */
public record NGTaglib( List<String> elementNames ) {

	public static final String FILENAME = "parsley.taglib";

	/**
	 * Parses a taglib.
	 *
	 * @param source Where the stream comes from, for error messages
	 * @throws IllegalArgumentException if the file isn't a valid taglib, saying why and where
	 */
	public static NGTaglib parse( final InputStream stream, final String source ) {
		Objects.requireNonNull( stream );

		final Document document;

		try {
			document = documentBuilder().parse( stream );
		}
		catch( SAXException | IOException e ) {
			throw new IllegalArgumentException( "%s: not a readable taglib: %s".formatted( source, e.getMessage() ), e );
		}

		final Element root = document.getDocumentElement();

		if( !"taglib".equals( root.getTagName() ) ) {
			throw new IllegalArgumentException( "%s: the root element must be <taglib>, not <%s>".formatted( source, root.getTagName() ) );
		}

		final List<String> elementNames = new ArrayList<>();
		final NodeList children = root.getChildNodes();

		for( int i = 0; i < children.getLength(); i++ ) {
			// Unknown elements are ignored, so a newer file still loads in an older runtime
			if( children.item( i ) instanceof Element child && "element".equals( child.getTagName() ) ) {
				final String name = attribute( child, "name" );

				if( name == null ) {
					throw new IllegalArgumentException( "%s: an <element> must have a name".formatted( source ) );
				}

				elementNames.add( name );
			}
		}

		return new NGTaglib( List.copyOf( elementNames ) );
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
	 * @return A parser that doesn't resolve external entities or DTDs (a taglib needs neither)
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
