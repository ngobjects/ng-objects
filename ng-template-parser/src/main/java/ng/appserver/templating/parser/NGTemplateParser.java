package ng.appserver.templating.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;
import ng.appserver.templating.parser.model.PBasicNode;
import ng.appserver.templating.parser.model.PCommentNode;
import ng.appserver.templating.parser.model.PHTMLNode;
import ng.appserver.templating.parser.model.PNode;
import ng.appserver.templating.parser.model.PRawNode;
import ng.appserver.templating.parser.model.PRootNode;
import ng.appserver.templating.parser.model.SourceRange;

/**
 * A single-pass recursive descent template parser.
 *
 * Replaces the old NGStringTokenizer → NGHTMLParser → callback → NGTemplateParser pipeline
 * with a direct character-by-character scan that produces a PNode tree.
 *
 * Source position tracking is built in from the ground up: every parse method
 * knows its start position, so SourceRange can be attached to nodes trivially.
 */

public class NGTemplateParser {

	/**
	 * Parser directive: raw/verbatim block (content not processed, included in output)
	 */
	private static final String DIRECTIVE_RAW = "raw";

	/**
	 * Parser directive: developer comment (content not processed, stripped from output)
	 */
	private static final String DIRECTIVE_COMMENT = "comment";

	/**
	 * The template source string
	 */
	private final String _source;

	/**
	 * The declarations parsed from the provided wod string
	 */
	private final Map<String, NGDeclaration> _declarations;

	/**
	 * Current position in the source string (the cursor)
	 */
	private int _pos;

	public NGTemplateParser( final String htmlString, final String declarationString ) throws NGDeclarationFormatException {
		Objects.requireNonNull( htmlString );
		Objects.requireNonNull( declarationString );

		_source = htmlString;
		_declarations = NGDeclarationParser.declarationsWithString( declarationString );
		_pos = 0;
	}

	/**
	 * @return The parsed template as a PNode tree
	 */
	public PNode parse() throws NGHTMLFormatException, NGDeclarationFormatException {
		final int startPos = _pos;
		final List<PNode> children = parseChildren( null, -1 );
		return new PRootNode( children, new SourceRange( startPos, _pos ) );
	}

	/**
	 * Parses a sequence of child nodes until we hit the expected closing tag or end of input.
	 *
	 * @param expectedClosingTag The tag name we expect to close this block (e.g. "wo:SomeComponent"), or null if we're at the root level
	 * @param openingTagPosition The source position where the opening tag started, used for error reporting if the closing tag is missing. -1 if at root level.
	 * @return The list of child PNodes
	 */
	private List<PNode> parseChildren( final String expectedClosingTag, final int openingTagPosition ) throws NGHTMLFormatException, NGDeclarationFormatException {
		final List<PNode> children = new ArrayList<>();
		final StringBuilder htmlBuffer = new StringBuilder();
		int htmlStart = _pos;

		while( _pos < _source.length() ) {

			// Check if we're at a '<' that might be something special
			if( current() == '<' ) {

				// Check for closing tag first
				if( lookingAt( "</" ) ) {
					if( expectedClosingTag != null && lookingAtClosingTag( expectedClosingTag ) ) {
						// Flush any accumulated HTML
						htmlStart = flushHTML( htmlBuffer, htmlStart, children );
						// Consume the closing tag
						consumeClosingTag( expectedClosingTag );
						return children;
					}
					// Catch typos like </ wo:Conditional> where a space follows </
					if( isAtMalformedClosingTag() ) {
						throw error( "Unexpected space after '</' in closing tag" );
					}
					// Catch mismatched namespaced closing tags like </wo:Repetition> inside a <wo:Conditional> block
					if( expectedClosingTag != null && isAtNamespacedClosingTag() ) {
						throw error( "Unexpected closing tag. Expected </%s>".formatted( expectedClosingTag ) );
					}
					// Not our closing tag — it's just HTML text (could be a regular HTML closing tag like </div>)
					htmlBuffer.append( current() );
					_pos++;
					continue;
				}

				// Check for parser directives
				if( lookingAtIgnoreCase( "<p:raw>" ) || lookingAtIgnoreCase( "<p:raw " ) || lookingAtIgnoreCase( "<p:raw/>" ) ) {
					htmlStart = flushHTML( htmlBuffer, htmlStart, children );
					children.add( parseRawDirective() );
					htmlStart = _pos;
					continue;
				}

				if( lookingAtIgnoreCase( "<p:comment>" ) || lookingAtIgnoreCase( "<p:comment " ) || lookingAtIgnoreCase( "<p:comment/>" ) ) {
					htmlStart = flushHTML( htmlBuffer, htmlStart, children );
					children.add( parseCommentDirective() );
					htmlStart = _pos;
					continue;
				}

				// Check for namespaced start tag (inline dynamic element)
				if( isAtNamespacedStartTag() ) {
					htmlStart = flushHTML( htmlBuffer, htmlStart, children );
					children.add( parseNamespacedElement() );
					htmlStart = _pos;
					continue;
				}

				// Catch typos like <wo: repetition> where a space follows the colon
				if( isAtMalformedNamespacedTag() ) {
					throw error( "Unexpected space after ':' in tag — did you mean to write a namespaced element?" );
				}

				// Check for legacy <webobject name="..."> or <wo name="...">
				if( lookingAtIgnoreCase( "<webobject " ) || lookingAtIgnoreCase( "<wo " ) ) {
					// Disambiguate: <wo name="..."> (legacy) vs <wo:Type> (inline namespace)
					// If we got here, it's NOT a namespaced tag (that was checked above), so it's legacy
					if( !isAtNamespacedStartTag() ) {
						htmlStart = flushHTML( htmlBuffer, htmlStart, children );
						children.add( parseLegacyElement() );
						htmlStart = _pos;
						continue;
					}
				}
			}

			// Regular character — accumulate as HTML
			htmlBuffer.append( current() );
			_pos++;
		}

		// End of input
		flushHTML( htmlBuffer, htmlStart, children );

		if( expectedClosingTag != null ) {
			throw error( "Unexpected end of template. Expected closing tag </%s>".formatted( expectedClosingTag ), openingTagPosition );
		}

		return children;
	}

	/**
	 * Parses a namespaced element: <ns:Type binding="value">children</ns:Type> or <ns:Type binding="value" />
	 */
	private PBasicNode parseNamespacedElement() throws NGHTMLFormatException, NGDeclarationFormatException {
		final int startPos = _pos;

		// Consume '<'
		expect( '<' );

		// Read namespace
		final String namespace = readIdentifier();

		if( namespace.isEmpty() ) {
			throw error( "Expected namespace identifier", startPos );
		}

		expect( ':' );

		// Read element type
		final String type = readIdentifier();

		if( type.isEmpty() ) {
			throw error( "Expected element type after '%s:'".formatted( namespace ) );
		}

		// Parse bindings
		final Map<String, NGBindingValue> bindings = parseBindings();

		skipWhitespace();

		// Self-closing?
		if( lookingAt( "/>" ) ) {
			_pos += 2;
			return new PBasicNode( namespace, type, bindings, List.of(), true, type, new SourceRange( startPos, _pos ) );
		}

		// Container tag — expect '>'
		expect( '>' );

		final String closingTagName = namespace + ":" + type;

		// Parse children recursively
		final List<PNode> children = parseChildren( closingTagName, startPos );

		return new PBasicNode( namespace, type, bindings, children, true, type, new SourceRange( startPos, _pos ) );
	}

	/**
	 * Parses a legacy element: <webobject name="Foo">children</webobject> or <wo name="Foo">children</wo>
	 */
	private PBasicNode parseLegacyElement() throws NGHTMLFormatException, NGDeclarationFormatException {
		final int startPos = _pos;

		// Consume '<'
		expect( '<' );

		// Read the tag keyword (either "webobject" or "wo")
		final String tagKeyword = readIdentifier();

		if( !"webobject".equalsIgnoreCase( tagKeyword ) && !"wo".equalsIgnoreCase( tagKeyword ) ) {
			throw error( "Expected 'webobject' or 'wo'", startPos );
		}

		skipWhitespace();

		// Parse the name attribute: name="declarationName" or name=declarationName
		final String declarationName = parseNameAttribute();

		skipWhitespace();

		// Look up the declaration
		final NGDeclaration declaration = _declarations.get( declarationName );

		if( declaration == null ) {
			throw new NGDeclarationFormatException( "No declaration for dynamic element (or component) named '%s'".formatted( declarationName ) );
		}

		// Self-closing?
		if( lookingAt( "/>" ) ) {
			_pos += 2;
			return new PBasicNode( declaration.namespace(), declaration.type(), declaration.bindings(), List.of(), false, declarationName, new SourceRange( startPos, _pos ) );
		}

		// Container tag — expect '>'
		expect( '>' );

		// Parse children recursively
		final List<PNode> children = parseChildren( tagKeyword, startPos );

		return new PBasicNode( declaration.namespace(), declaration.type(), declaration.bindings(), children, false, declarationName, new SourceRange( startPos, _pos ) );
	}

	/**
	 * Parses the name="value" attribute from a legacy webobject/wo tag
	 */
	private String parseNameAttribute() throws NGHTMLFormatException {
		// Expect "name"
		final String attrName = readIdentifier();

		if( !"name".equalsIgnoreCase( attrName ) ) {
			throw error( "Expected 'name' attribute but found '%s'".formatted( attrName ) );
		}

		skipWhitespace();
		expect( '=' );
		skipWhitespace();

		// Quoted or unquoted value
		if( _pos < _source.length() && current() == '"' ) {
			return readQuotedString();
		}
		else {
			return readAttributeValue();
		}
	}

	/**
	 * Parses a <p:raw>...</p:raw> directive.
	 * Content is captured verbatim (no template processing), but included in output.
	 * Handles nested <p:raw> tags correctly.
	 */
	private PRawNode parseRawDirective() throws NGHTMLFormatException {
		final int startPos = _pos;

		// Consume the opening tag
		consumeOpeningTagFully();

		// Check for self-closing: the tag we just consumed might have been <p:raw/>
		// In that case consumeOpeningTagFully already consumed up to and including '>'
		// We need to check what was consumed
		final String consumed = _source.substring( startPos, _pos );

		if( consumed.endsWith( "/>" ) ) {
			return new PRawNode( "", new SourceRange( startPos, _pos ) );
		}

		// Scan for closing </p:raw> with nesting
		final String content = scanDirectiveContent( DIRECTIVE_RAW );

		return new PRawNode( content, new SourceRange( startPos, _pos ) );
	}

	/**
	 * Parses a <p:comment>...</p:comment> directive.
	 * Content is captured verbatim and stripped from output entirely.
	 */
	private PCommentNode parseCommentDirective() throws NGHTMLFormatException {
		final int startPos = _pos;

		// Consume the opening tag
		consumeOpeningTagFully();

		final String consumed = _source.substring( startPos, _pos );

		if( consumed.endsWith( "/>" ) ) {
			return new PCommentNode( "", new SourceRange( startPos, _pos ) );
		}

		final String content = scanDirectiveContent( DIRECTIVE_COMMENT );

		return new PCommentNode( content, new SourceRange( startPos, _pos ) );
	}

	/**
	 * Scans forward for the closing tag of a parser directive, handling nested occurrences.
	 *
	 * @param directiveName "raw" or "comment"
	 * @return The content between the opening and closing tags
	 */
	private String scanDirectiveContent( final String directiveName ) throws NGHTMLFormatException {
		final int contentStart = _pos;
		int nestingDepth = 0;

		final String openingPattern = "<p:" + directiveName;
		final String closingPattern = "</p:" + directiveName;

		while( _pos < _source.length() ) {

			if( current() == '<' ) {
				// Check for nested opening tag
				if( lookingAtIgnoreCase( openingPattern ) ) {
					// Verify it's actually a tag (followed by '>', ' ', or '/')
					final int afterName = _pos + openingPattern.length();

					if( afterName < _source.length() ) {
						final char next = _source.charAt( afterName );

						if( next == '>' || next == ' ' || next == '/' || next == '\t' || next == '\n' || next == '\r' ) {
							nestingDepth++;
						}
					}
				}
				// Check for closing tag
				else if( lookingAtIgnoreCase( closingPattern ) ) {
					final int afterName = _pos + closingPattern.length();
					boolean isClosingTag = false;

					if( afterName < _source.length() ) {
						final char next = _source.charAt( afterName );

						if( next == '>' || next == ' ' || next == '\t' || next == '\n' || next == '\r' ) {
							isClosingTag = true;
						}
					}
					else if( afterName == _source.length() ) {
						// At the end — malformed, but we'll catch that
						isClosingTag = false;
					}

					if( isClosingTag ) {
						if( nestingDepth > 0 ) {
							nestingDepth--;
						}
						else {
							// This is our closing tag
							final String content = _source.substring( contentStart, _pos );
							// Consume </p:directiveName>
							_pos += closingPattern.length();
							skipUntilAndConsume( '>' );
							return content;
						}
					}
				}
			}

			_pos++;
		}

		throw error( "Unclosed parser directive <p:%s>".formatted( directiveName ) );
	}

	// ---- Binding parsing ----

	/**
	 * Parses inline binding key=value pairs and boolean (valueless) attributes from
	 * the current position until '>' or '/>'.
	 *
	 * IMPORTANT: Inline binding values are stored raw, including their surrounding quotes.
	 * For example, value="$hello" is stored as NGBindingValue.Value(false, "\"$hello\"").
	 * The downstream NGAssociationFactory expects this format and handles quote stripping,
	 * escape processing, and '$' detection itself.
	 *
	 * Boolean attributes (e.g. "disabled" in {@code <my:Widget disabled />}) are stored
	 * as NGBindingValue.BooleanPresence instances — their meaning is solely their presence.
	 */
	private Map<String, NGBindingValue> parseBindings() throws NGHTMLFormatException {
		final Map<String, NGBindingValue> bindings = new HashMap<>();

		while( _pos < _source.length() ) {
			skipWhitespace();

			if( _pos >= _source.length() ) {
				break;
			}

			final char ch = current();

			// End of tag
			if( ch == '>' || ch == '/' ) {
				break;
			}

			// Read binding key
			final String key = readIdentifier();

			if( key.isEmpty() ) {
				throw error( "Expected binding key, found '%c'".formatted( current() ) );
			}

			skipWhitespace();

			// Check if this is a boolean attribute (no '=' follows) or a key=value binding
			if( _pos < _source.length() && current() == '=' ) {
				_pos++; // consume '='
				skipWhitespace();

				// Read binding value — stored raw (with quotes if present) for inline binding processing
				final String value = readInlineBindingValue();
				bindings.put( key, new NGBindingValue.Value( false, value ) );
			}
			else {
				// Boolean attribute — present with no value
				bindings.put( key, new NGBindingValue.BooleanPresence() );
			}
		}

		return bindings;
	}

	/**
	 * Reads an inline binding value, preserving quotes if present.
	 *
	 * For quoted values: reads from opening '"' to closing '"', handling escape sequences (\")
	 * to find the correct end, and returns the entire string including the surrounding quotes.
	 *
	 * For unquoted values: reads until whitespace, '>', or '/'.
	 */
	private String readInlineBindingValue() throws NGHTMLFormatException {

		if( _pos < _source.length() && current() == '"' ) {
			// Quoted value — scan to the matching closing quote, respecting \" escapes
			final int start = _pos;
			_pos++; // skip opening quote

			while( _pos < _source.length() ) {
				final char ch = current();

				if( ch == '\\' ) {
					_pos += 2; // skip escape sequence
				}
				else if( ch == '"' ) {
					_pos++; // skip closing quote
					return _source.substring( start, _pos );
				}
				else {
					_pos++;
				}
			}

			throw error( "Unclosed quoted binding value", start );
		}
		else {
			// Unquoted value
			final int start = _pos;

			while( _pos < _source.length() ) {
				final char ch = current();

				if( ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '>' || ch == '/' ) {
					break;
				}

				_pos++;
			}

			if( _pos == start ) {
				throw error( "Expected binding value", start );
			}

			return _source.substring( start, _pos );
		}
	}

	// ---- Low-level scanning helpers ----

	/**
	 * @return The character at the current position
	 */
	private char current() {
		return _source.charAt( _pos );
	}

	/**
	 * Asserts the current character is the expected one and advances past it.
	 */
	private void expect( final char expected ) throws NGHTMLFormatException {
		if( _pos >= _source.length() ) {
			throw error( "Unexpected end of template, expected '%c'".formatted( expected ) );
		}

		if( current() != expected ) {
			throw error( "Expected '%c' but found '%c'".formatted( expected, current() ) );
		}

		_pos++;
	}

	/**
	 * Skips whitespace characters at the current position.
	 */
	private void skipWhitespace() {
		while( _pos < _source.length() ) {
			final char ch = current();

			if( ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r' ) {
				break;
			}

			_pos++;
		}
	}

	/**
	 * Reads an identifier (letters, digits, underscores, hyphens) starting at the current position.
	 */
	private String readIdentifier() {
		final int start = _pos;

		while( _pos < _source.length() ) {
			final char ch = current();

			if( Character.isLetterOrDigit( ch ) || ch == '_' || ch == '-' ) {
				_pos++;
			}
			else {
				break;
			}
		}

		return _source.substring( start, _pos );
	}

	/**
	 * Reads a double-quoted string value (no escape handling, for name attributes)
	 */
	private String readQuotedString() throws NGHTMLFormatException {
		expect( '"' );

		final int start = _pos;

		while( _pos < _source.length() && current() != '"' ) {
			_pos++;
		}

		if( _pos >= _source.length() ) {
			throw error( "Unclosed quoted string", start - 1 );
		}

		final String value = _source.substring( start, _pos );
		_pos++; // skip closing quote
		return value;
	}

	/**
	 * Reads an unquoted attribute value (up to whitespace, '>', or '/')
	 */
	private String readAttributeValue() {
		final int start = _pos;

		while( _pos < _source.length() ) {
			final char ch = current();

			if( ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '>' || ch == '/' ) {
				break;
			}

			_pos++;
		}

		return _source.substring( start, _pos );
	}

	/**
	 * @return true if the source at the current position starts with the given string
	 */
	private boolean lookingAt( final String s ) {
		return _source.startsWith( s, _pos );
	}

	/**
	 * @return true if the source at the current position starts with the given string (case-insensitive)
	 */
	private boolean lookingAtIgnoreCase( final String s ) {
		if( _pos + s.length() > _source.length() ) {
			return false;
		}

		for( int i = 0; i < s.length(); i++ ) {
			if( Character.toLowerCase( _source.charAt( _pos + i ) ) != Character.toLowerCase( s.charAt( i ) ) ) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @return true if the current position is at a closing tag matching the given name (case-sensitive).
	 *
	 * Matches </name> or </name followed by whitespace then >
	 */
	private boolean lookingAtClosingTag( final String tagName ) {
		if( !lookingAt( "</" + tagName ) ) {
			return false;
		}

		final int afterName = _pos + 2 + tagName.length();

		if( afterName >= _source.length() ) {
			return false;
		}

		final char next = _source.charAt( afterName );
		return next == '>' || next == ' ' || next == '\t' || next == '\n' || next == '\r';
	}

	/**
	 * Consumes a closing tag: </tagName> (allowing whitespace before >)
	 */
	private void consumeClosingTag( final String tagName ) throws NGHTMLFormatException {
		_pos += 2; // "</"

		// Skip the tag name (case-sensitive)
		final String actual = _source.substring( _pos, Math.min( _pos + tagName.length(), _source.length() ) );

		if( !actual.equals( tagName ) ) {
			throw error( "Expected closing tag </%s> but found </%s>".formatted( tagName, actual ) );
		}

		_pos += tagName.length();
		skipWhitespace();
		expect( '>' );
	}

	/**
	 * Consumes a complete opening tag from '<' up to and including '>' or '/>'
	 * Used for parser directives where we don't need to parse the individual attributes.
	 */
	private void consumeOpeningTagFully() throws NGHTMLFormatException {
		if( _pos >= _source.length() || current() != '<' ) {
			throw error( "Expected '<'" );
		}

		while( _pos < _source.length() ) {
			if( current() == '>' ) {
				_pos++;
				return;
			}

			_pos++;
		}

		throw error( "Unclosed tag" );
	}

	/**
	 * Advances past characters until the given character is found, then consumes it too.
	 */
	private void skipUntilAndConsume( final char target ) throws NGHTMLFormatException {
		while( _pos < _source.length() ) {
			if( current() == target ) {
				_pos++;
				return;
			}

			_pos++;
		}

		throw error( "Expected '%c' but reached end of template".formatted( target ) );
	}

	/**
	 * @return true if the current position is at the start of a namespaced opening tag (<ns:type)
	 *
	 * Pattern: '<' followed by one or more letters, ':', then one or more letters/digits
	 */
	private boolean isAtNamespacedStartTag() {
		if( _pos >= _source.length() || current() != '<' ) {
			return false;
		}

		final int start = _pos + 1; // skip '<'

		// Check for '/' (that would be a closing tag)
		if( start < _source.length() && _source.charAt( start ) == '/' ) {
			return false;
		}

		// Read potential namespace: one or more letters
		int i = start;

		while( i < _source.length() && Character.isLetter( _source.charAt( i ) ) ) {
			i++;
		}

		// Need at least one letter before ':'
		if( i == start ) {
			return false;
		}

		// Expect ':'
		if( i >= _source.length() || _source.charAt( i ) != ':' ) {
			return false;
		}

		i++; // skip ':'

		// Need at least one letter/digit after ':'
		if( i >= _source.length() || !Character.isLetterOrDigit( _source.charAt( i ) ) ) {
			return false;
		}

		return true;
	}

	/**
	 * @return true if the current position looks like a malformed namespaced tag with a space after the colon.
	 *
	 * Matches the pattern: '<' letters ':' space — e.g. {@code <wo: Repetition>}
	 */
	private boolean isAtMalformedNamespacedTag() {
		if( _pos >= _source.length() || current() != '<' ) {
			return false;
		}

		int i = _pos + 1;

		// Need at least one letter before ':'
		while( i < _source.length() && Character.isLetter( _source.charAt( i ) ) ) {
			i++;
		}

		if( i == _pos + 1 ) {
			return false;
		}

		// Expect ':'
		if( i >= _source.length() || _source.charAt( i ) != ':' ) {
			return false;
		}

		i++; // skip ':'

		// The tell: a space right after the colon
		return i < _source.length() && _source.charAt( i ) == ' ';
	}

	/**
	 * @return true if the current position looks like a malformed closing tag with a space after {@code </}.
	 *
	 * Matches the pattern: {@code </ letters:} — e.g. {@code </ wo:Conditional>}
	 * Only matches namespaced tags (with a colon) to avoid false positives on regular HTML like {@code </ div>}.
	 */
	private boolean isAtMalformedClosingTag() {
		if( !lookingAt( "</ " ) ) {
			return false;
		}

		int i = _pos + 3; // skip "</ "

		// Skip any extra whitespace
		while( i < _source.length() && _source.charAt( i ) == ' ' ) {
			i++;
		}

		// Need at least one letter
		final int nameStart = i;

		while( i < _source.length() && Character.isLetter( _source.charAt( i ) ) ) {
			i++;
		}

		if( i == nameStart ) {
			return false;
		}

		// Only flag it if there's a colon (namespaced tag) — </ div> is probably not our concern
		return i < _source.length() && _source.charAt( i ) == ':';
	}

	/**
	 * @return true if the current position is at a namespaced closing tag ({@code </letters:letters}).
	 *
	 * Used to detect mismatched closing tags — e.g. {@code </wo:Repetition>} inside a {@code <wo:Conditional>} block.
	 */
	private boolean isAtNamespacedClosingTag() {
		if( !lookingAt( "</" ) ) {
			return false;
		}

		int i = _pos + 2; // skip "</"

		// Need at least one letter before ':'
		while( i < _source.length() && Character.isLetter( _source.charAt( i ) ) ) {
			i++;
		}

		if( i == _pos + 2 ) {
			return false;
		}

		// Expect ':'
		if( i >= _source.length() || _source.charAt( i ) != ':' ) {
			return false;
		}

		i++; // skip ':'

		// Need at least one letter/digit after ':'
		return i < _source.length() && Character.isLetterOrDigit( _source.charAt( i ) );
	}

	/**
	 * Flushes accumulated HTML text into the children list as a PHTMLNode (if non-empty),
	 * then resets the buffer.
	 *
	 * @return The current position, to be used as the start of the next HTML buffer
	 */
	private int flushHTML( final StringBuilder buffer, final int htmlStart, final List<PNode> children ) {
		if( buffer.length() > 0 ) {
			children.add( new PHTMLNode( buffer.toString(), new SourceRange( htmlStart, _pos ) ) );
			buffer.setLength( 0 );
		}

		return _pos;
	}

	// ---- Error reporting ----

	/**
	 * Creates an NGHTMLFormatException with the current parser position and source context.
	 */
	private NGHTMLFormatException error( final String message ) {
		return new NGHTMLFormatException( message, _pos, _source );
	}

	/**
	 * Creates an NGHTMLFormatException with an explicit position and source context.
	 */
	private NGHTMLFormatException error( final String message, final int position ) {
		return new NGHTMLFormatException( message, position, _source );
	}
}
