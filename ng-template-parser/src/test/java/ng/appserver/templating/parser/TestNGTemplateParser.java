package ng.appserver.templating.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;
import ng.appserver.templating.parser.model.PBasicNode;
import ng.appserver.templating.parser.model.PCommentNode;
import ng.appserver.templating.parser.model.PHTMLNode;
import ng.appserver.templating.parser.model.PNode;
import ng.appserver.templating.parser.model.PRawNode;
import ng.appserver.templating.parser.model.PRootNode;
import ng.appserver.templating.parser.model.SourceRange;

/**
 * Tests for the recursive descent parser (NGTemplateParser).
 *
 * Verifies that it produces identical PNode trees to the old parser,
 * and tests edge cases specific to the new implementation.
 */

public class TestNGTemplateParser {

	// ---- Plain HTML ----

	@Test
	public void plainHTML() throws Exception {
		final PRootNode root = parse( "<div>Hello</div>", "" );
		assertEquals( 1, root.children().size() );
		assertHTML( "<div>Hello</div>", root.children().getFirst() );
	}

	@Test
	public void emptyTemplate() throws Exception {
		final PRootNode root = parse( "", "" );
		assertEquals( 0, root.children().size() );
	}

	@Test
	public void htmlWithComment() throws Exception {
		final PRootNode root = parse( "<!-- a comment --><p>text</p>", "" );
		assertEquals( 1, root.children().size() );
		assertHTML( "<!-- a comment --><p>text</p>", root.children().getFirst() );
	}

	// ---- Inline namespaced elements ----

	@Test
	public void selfClosingInlineElement() throws Exception {
		final PRootNode root = parse( "<wo:String value=\"$name\" />", "" );
		assertEquals( 1, root.children().size() );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( "wo", node.namespace() );
		assertEquals( "String", node.type() );
		assertEquals( true, node.isInline() );
		assertTrue( node.selfClosing() );
		assertEquals( 0, node.children().size() );
		assertValueBinding( true, "$name", node.bindings().get( "value" ) );
	}

	@Test
	public void containerInlineElement() throws Exception {
		final PRootNode root = parse( "<wo:Conditional condition=\"$showIt\">Hello</wo:Conditional>", "" );
		assertEquals( 1, root.children().size() );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( "wo", node.namespace() );
		assertEquals( "Conditional", node.type() );
		assertFalse( node.selfClosing() );
		assertEquals( 1, node.children().size() );
		assertHTML( "Hello", node.children().getFirst() );
	}

	@Test
	public void selfClosingVsEmptyContainer() throws Exception {
		// Self-closing and empty container both have zero children, but differ in selfClosing flag
		final PRootNode selfClosed = parse( "<wo:Widget />", "" );
		final PBasicNode selfClosedNode = assertBasicNode( selfClosed.children().getFirst() );
		assertTrue( selfClosedNode.selfClosing() );
		assertEquals( 0, selfClosedNode.children().size() );

		final PRootNode emptyContainer = parse( "<wo:Widget></wo:Widget>", "" );
		final PBasicNode emptyContainerNode = assertBasicNode( emptyContainer.children().getFirst() );
		assertFalse( emptyContainerNode.selfClosing() );
		assertEquals( 0, emptyContainerNode.children().size() );
	}

	@Test
	public void nestedInlineElements() throws Exception {
		final PRootNode root = parse( "<wo:Conditional condition=\"$a\"><wo:String value=\"$b\" /></wo:Conditional>", "" );
		final PBasicNode outer = assertBasicNode( root.children().getFirst() );
		assertEquals( "Conditional", outer.type() );
		assertEquals( 1, outer.children().size() );
		final PBasicNode inner = assertBasicNode( outer.children().getFirst() );
		assertEquals( "String", inner.type() );
	}

	@Test
	public void customNamespace() throws Exception {
		final PRootNode root = parseWithNamespaces( "<ui:Button label=\"Click\" />", "", Set.of( "wo", "ui" ) );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( "ui", node.namespace() );
		assertEquals( "Button", node.type() );
	}

	@Test
	public void mixedHTMLAndDynamic() throws Exception {
		final PRootNode root = parse( "<p>Before</p><wo:String value=\"$x\" /><p>After</p>", "" );
		assertEquals( 3, root.children().size() );
		assertHTML( "<p>Before</p>", root.children().get( 0 ) );
		assertBasicNode( root.children().get( 1 ) );
		assertHTML( "<p>After</p>", root.children().get( 2 ) );
	}

	// ---- Legacy WOD-style elements ----

	@Test
	public void legacyWebobjectTag() throws Exception {
		final String wod = "MyString : String { value = \"hello\"; }";
		final PRootNode root = parse( "<webobject name=\"MyString\"></webobject>", wod );
		assertEquals( 1, root.children().size() );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( "wo", node.namespace() );
		assertEquals( "String", node.type() );
		assertEquals( false, node.isInline() );
	}

	@Test
	public void legacyWoTag() throws Exception {
		final String wod = "MyString : String { value = \"hello\"; }";
		final PRootNode root = parse( "<wo name=\"MyString\"></wo>", wod );
		assertEquals( 1, root.children().size() );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( "String", node.type() );
		assertEquals( false, node.isInline() );
	}

	@Test
	public void legacyMissingDeclaration() {
		assertThrows( NGDeclarationFormatException.class, () -> {
			parse( "<webobject name=\"Missing\"></webobject>", "" );
		} );
	}

	// ---- Legacy case insensitivity ----

	@Test
	public void legacyWebobjectTagUpperCase() throws Exception {
		final String wod = "MyString : String { value = \"hello\"; }";
		final PRootNode root = parse( "<WEBOBJECT NAME=\"MyString\"></WEBOBJECT>", wod );
		assertEquals( 1, root.children().size() );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( "String", node.type() );
	}

	@Test
	public void legacyWebobjectTagMixedCase() throws Exception {
		final String wod = "MyString : String { value = \"hello\"; }";
		final PRootNode root = parse( "<WebObject name=\"MyString\"></WebObject>", wod );
		assertEquals( 1, root.children().size() );
		assertBasicNode( root.children().getFirst() );
	}

	@Test
	public void legacyClosingTagCaseMismatch() throws Exception {
		// Opening tag lowercase, closing tag uppercase — should still work
		final String wod = "MyString : String { value = \"hello\"; }";
		final PRootNode root = parse( "<webobject name=\"MyString\"></WEBOBJECT>", wod );
		assertEquals( 1, root.children().size() );
		assertBasicNode( root.children().getFirst() );
	}

	@Test
	public void legacyWoTagCaseMismatch() throws Exception {
		// Opening <WO>, closing </wo> — should still work
		final String wod = "MyString : String { value = \"hello\"; }";
		final PRootNode root = parse( "<WO name=\"MyString\"></wo>", wod );
		assertEquals( 1, root.children().size() );
		assertBasicNode( root.children().getFirst() );
	}

	@Test
	public void legacyWebobjectTagWildCase() throws Exception {
		// Truly wild casing
		final String wod = "MyString : String { value = \"hello\"; }";
		final PRootNode root = parse( "<wEbObJeCt name=\"MyString\">Hello</WeBoBjEcT>", wod );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( "String", node.type() );
		assertEquals( 1, node.children().size() );
		assertHTML( "Hello", node.children().getFirst() );
	}

	// ---- Parser directives ----

	@Test
	public void rawDirective() throws Exception {
		final PRootNode root = parse( "<p:raw><wo:String value=\"$x\" /></p:raw>", "" );
		assertEquals( 1, root.children().size() );
		final PRawNode raw = assertInstanceOf( PRawNode.class, root.children().getFirst() );
		assertEquals( "<wo:String value=\"$x\" />", raw.value() );
	}

	@Test
	public void commentDirective() throws Exception {
		final PRootNode root = parse( "<p:comment>This is hidden</p:comment>", "" );
		assertEquals( 1, root.children().size() );
		assertInstanceOf( PCommentNode.class, root.children().getFirst() );
	}

	@Test
	public void nestedRawDirective() throws Exception {
		final PRootNode root = parse( "<p:raw>outer<p:raw>inner</p:raw>outer</p:raw>", "" );
		assertEquals( 1, root.children().size() );
		final PRawNode raw = assertInstanceOf( PRawNode.class, root.children().getFirst() );
		assertEquals( "outer<p:raw>inner</p:raw>outer", raw.value() );
	}

	@Test
	public void selfClosingRawDirective() throws Exception {
		final PRootNode root = parse( "<p:raw/>", "" );
		assertEquals( 1, root.children().size() );
		final PRawNode raw = assertInstanceOf( PRawNode.class, root.children().getFirst() );
		assertEquals( "", raw.value() );
	}

	// ---- Error cases ----

	@Test
	public void unclosedDynamicTag() {
		assertThrows( NGHTMLFormatException.class, () -> {
			parse( "<wo:Conditional condition=\"$a\">oops", "" );
		} );
	}

	@Test
	public void unclosedRawDirective() {
		assertThrows( NGHTMLFormatException.class, () -> {
			parse( "<p:raw>no closing tag", "" );
		} );
	}

	@Test
	public void unclosedQuotedBinding() {
		assertThrows( NGHTMLFormatException.class, () -> {
			parse( "<wo:String value=\"unclosed />", "" );
		} );
	}

	@Test
	public void spaceAfterColonInNamespacedTag() {
		assertThrows( NGHTMLFormatException.class, () -> {
			parse( "<wo: Repetition list=\"$items\" item=\"$item\">hello</wo: Repetition>", "" );
		} );
	}

	@Test
	public void mismatchedClosingTag() {
		final NGHTMLFormatException ex = assertThrows( NGHTMLFormatException.class, () -> {
			parse( "<wo:Conditional condition=\"$a\">hello</wo:Repetition>", "" );
		} );
		assert ex.getMessage().contains( "Unexpected closing tag" ) : "Expected mismatch error, got: " + ex.getMessage();
		assert ex.getMessage().contains( "wo:Conditional" ) : "Error should mention expected tag, got: " + ex.getMessage();
	}

	@Test
	public void spaceAfterSlashInClosingTag() {
		assertThrows( NGHTMLFormatException.class, () -> {
			parse( "<wo:Conditional condition=\"$a\">hello</ wo:Conditional>", "" );
		} );
	}

	// ---- Error position reporting ----

	@Test
	public void errorPositionForUnclosedDynamicTag() {
		final String html = "<wo:Conditional condition=\"$a\">oops";
		final NGHTMLFormatException ex = assertThrows( NGHTMLFormatException.class, () -> parse( html, "" ) );
		// Error should point at the opening tag, not at EOF
		assertEquals( 1, ex.line() );
		assertEquals( 1, ex.column() );
		assertEquals( 0, ex.position() );
	}

	@Test
	public void errorPositionForUnclosedDynamicTagOnLaterLine() {
		// "<div>\n\t<wo:Conditional condition="$a">\n\t\toops\n</div>"
		//  01234  5 6
		final String html = "<div>\n\t<wo:Conditional condition=\"$a\">\n\t\toops\n</div>";
		final NGHTMLFormatException ex = assertThrows( NGHTMLFormatException.class, () -> parse( html, "" ) );
		// Error should point at the opening <wo:Conditional> tag on line 2
		assertEquals( 2, ex.line() );
		assertEquals( 2, ex.column() ); // tab + '<' → column 2
		assertEquals( 7, ex.position() ); // "<div>" (0-4) + "\n" (5) + "\t" (6) + "<" at position 7
	}

	@Test
	public void errorPositionForUnclosedQuotedBinding() {
		final String html = "<wo:String value=\"unclosed />";
		final NGHTMLFormatException ex = assertThrows( NGHTMLFormatException.class, () -> parse( html, "" ) );
		// The error position should point at the opening quote of the unclosed value
		assertEquals( 1, ex.line() );
		assertEquals( 18, ex.column() ); // position of the opening " in value="unclosed
		assertEquals( 17, ex.position() );
	}

	@Test
	public void errorPositionForUnclosedRawDirective() {
		final String html = "<p:raw>no closing tag";
		final NGHTMLFormatException ex = assertThrows( NGHTMLFormatException.class, () -> parse( html, "" ) );
		assertEquals( 1, ex.line() );
		assertEquals( html.length() + 1, ex.column() );
	}

	@Test
	public void errorPositionOnMultilineTemplate() {
		// Position at "condition" start is right after the \n on line 2
		final String html = "<div>\n<wo:Conditional condition=\"$show>\nMore text";
		final NGHTMLFormatException ex = assertThrows( NGHTMLFormatException.class, () -> parse( html, "" ) );
		// The unclosed quoted value starts at the " before $show> — which is on line 2
		assertEquals( 2, ex.line() );
	}

	@Test
	public void errorMessageContainsLineAndColumn() {
		final String html = "<wo:String value=\"unclosed />";
		final NGHTMLFormatException ex = assertThrows( NGHTMLFormatException.class, () -> parse( html, "" ) );
		// The exception message should contain the line and column info
		final String message = ex.getMessage();
		assert message.contains( "line" ) : "Error message should contain 'line': " + message;
		assert message.contains( "column" ) : "Error message should contain 'column': " + message;
	}

	@Test
	public void errorPositionOnThirdLine() {
		final String html = "line one\nline two\n<wo:Bad";
		final NGHTMLFormatException ex = assertThrows( NGHTMLFormatException.class, () -> parse( html, "" ) );
		// The error should be on line 3
		assertEquals( 3, ex.line() );
	}

	@Test
	public void legacyExceptionHasNoPosition() {
		// The old-style constructor should produce -1 for position, line, and column
		final NGHTMLFormatException ex = new NGHTMLFormatException( "simple error" );
		assertEquals( -1, ex.position() );
		assertEquals( -1, ex.line() );
		assertEquals( -1, ex.column() );
	}

	// ---- Multiple bindings ----

	@Test
	public void multipleBindings() throws Exception {
		final PRootNode root = parse( "<wo:TextField value=\"$name\" size=\"$fieldSize\" />", "" );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( 2, node.bindings().size() );
		assertValueBinding( true, "$name", node.bindings().get( "value" ) );
		assertValueBinding( true, "$fieldSize", node.bindings().get( "size" ) );
	}

	// ---- Boolean attributes ----

	@Test
	public void booleanAttribute() throws Exception {
		final PRootNode root = parse( "<wo:Widget disabled />", "" );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( 1, node.bindings().size() );
		assertBooleanBinding( node.bindings().get( "disabled" ) );
	}

	@Test
	public void booleanAttributeWithValueBindings() throws Exception {
		final PRootNode root = parse( "<wo:TextField value=\"$name\" disabled size=\"$s\" />", "" );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( 3, node.bindings().size() );
		assertValueBinding( true, "$name", node.bindings().get( "value" ) );
		assertBooleanBinding( node.bindings().get( "disabled" ) );
		assertValueBinding( true, "$s", node.bindings().get( "size" ) );
	}

	@Test
	public void multipleBooleanAttributes() throws Exception {
		final PRootNode root = parse( "<wo:Widget disabled readonly hidden />", "" );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( 3, node.bindings().size() );
		assertBooleanBinding( node.bindings().get( "disabled" ) );
		assertBooleanBinding( node.bindings().get( "readonly" ) );
		assertBooleanBinding( node.bindings().get( "hidden" ) );
	}

	@Test
	public void booleanAttributeOnContainerElement() throws Exception {
		final PRootNode root = parse( "<wo:Conditional negate>Content</wo:Conditional>", "" );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertBooleanBinding( node.bindings().get( "negate" ) );
		assertEquals( 1, node.children().size() );
		assertHTML( "Content", node.children().getFirst() );
	}

	@Test
	public void booleanAttributeBeforeClosingSlash() throws Exception {
		final PRootNode root = parse( "<wo:Widget checked/>", "" );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( 1, node.bindings().size() );
		assertBooleanBinding( node.bindings().get( "checked" ) );
	}

	// ---- Namespace filtering ----

	@Test
	public void unregisteredNamespaceTreatedAsHTML() throws Exception {
		final PRootNode root = parse( "<svg:rect width=\"100\" height=\"50\" />", "" );
		assertEquals( 1, root.children().size() );
		// svg:rect should pass through as plain HTML since "svg" is not a registered namespace
		assertHTML( "<svg:rect width=\"100\" height=\"50\" />", root.children().getFirst() );
	}

	@Test
	public void unregisteredNamespaceWithChildren() throws Exception {
		final PRootNode root = parse( "<xsl:template match=\"/\"><p>Hello</p></xsl:template>", "" );
		assertEquals( 1, root.children().size() );
		assertHTML( "<xsl:template match=\"/\"><p>Hello</p></xsl:template>", root.children().getFirst() );
	}

	@Test
	public void registeredNamespaceStillWorks() throws Exception {
		final PRootNode root = parse( "<wo:String value=\"$name\" />", "" );
		assertEquals( 1, root.children().size() );
		assertBasicNode( root.children().getFirst() );
	}

	@Test
	public void mixedRegisteredAndUnregisteredNamespaces() throws Exception {
		final PRootNode root = parse( "<svg:rect /><wo:String value=\"$x\" /><svg:circle />", "" );
		assertEquals( 3, root.children().size() );
		assertHTML( "<svg:rect />", root.children().get( 0 ) );
		assertBasicNode( root.children().get( 1 ) );
		assertHTML( "<svg:circle />", root.children().get( 2 ) );
	}

	@Test
	public void customNamespaceRegistered() throws Exception {
		final PRootNode root = parseWithNamespaces( "<ui:Button label=\"Click\" /><svg:rect />", "", Set.of( "wo", "ui" ) );
		assertEquals( 2, root.children().size() );
		final PBasicNode button = assertBasicNode( root.children().get( 0 ) );
		assertEquals( "ui", button.namespace() );
		assertHTML( "<svg:rect />", root.children().get( 1 ) );
	}

	@Test
	public void malformedTagIgnoredForUnregisteredNamespace() throws Exception {
		// <svg: rect> should NOT trigger an error — svg is not a registered namespace
		final PRootNode root = parse( "<svg: rect>content</svg: rect>", "" );
		assertEquals( 1, root.children().size() );
		assertHTML( "<svg: rect>content</svg: rect>", root.children().getFirst() );
	}

	@Test
	public void malformedTagStillDetectedForRegisteredNamespace() {
		// <wo: String> should still trigger an error
		assertThrows( NGHTMLFormatException.class, () -> {
			parse( "<wo: String value=\"$x\" />", "" );
		} );
	}

	// ---- Comparison with old parser ----

	@Test
	public void matchesOldParserForSimpleInline() throws Exception {
		final String html = "<wo:String value=\"$name\" />";
		compareWithOldParser( html, "" );
	}

	@Test
	public void matchesOldParserForContainer() throws Exception {
		final String html = "<wo:Conditional condition=\"$show\"><p>Content</p></wo:Conditional>";
		compareWithOldParser( html, "" );
	}

	@Test
	public void matchesOldParserForMixedContent() throws Exception {
		final String html = "<h1>Title</h1><wo:String value=\"$body\" /><p>Footer</p>";
		compareWithOldParser( html, "" );
	}

	@Test
	public void matchesOldParserForLegacy() throws Exception {
		final String html = "<webobject name=\"Greeting\">Hello</webobject>";
		final String wod = "Greeting : String { value = \"hello\"; }";
		compareWithOldParser( html, wod );
	}

	// ---- Source range tracking ----

	@Test
	public void sourceRangeForRootNode() throws Exception {
		final String html = "<div>Hello</div>";
		final PRootNode root = parse( html, "" );
		assertEquals( new SourceRange( 0, html.length() ), root.sourceRange() );
	}

	@Test
	public void sourceRangeForHTMLNode() throws Exception {
		final PRootNode root = parse( "<p>text</p>", "" );
		final PHTMLNode html = assertInstanceOf( PHTMLNode.class, root.children().getFirst() );
		assertEquals( new SourceRange( 0, 11 ), html.sourceRange() );
	}

	@Test
	public void sourceRangeForSelfClosingElement() throws Exception {
		final String html = "<wo:String value=\"$x\" />";
		final PRootNode root = parse( html, "" );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( new SourceRange( 0, html.length() ), node.sourceRange() );
	}

	@Test
	public void sourceRangeForContainerElement() throws Exception {
		final String html = "<wo:Conditional condition=\"$a\">text</wo:Conditional>";
		final PRootNode root = parse( html, "" );
		final PBasicNode node = assertBasicNode( root.children().getFirst() );
		assertEquals( new SourceRange( 0, html.length() ), node.sourceRange() );
	}

	@Test
	public void sourceRangesForMixedContent() throws Exception {
		// Positions:  0         1         2         3         4         5
		//             0123456789012345678901234567890123456789012345678901234
		final String html = "<p>Before</p><wo:String value=\"$x\" /><p>After</p>";
		final PRootNode root = parse( html, "" );
		assertEquals( 3, root.children().size() );

		final PHTMLNode before = assertInstanceOf( PHTMLNode.class, root.children().get( 0 ) );
		assertEquals( new SourceRange( 0, 13 ), before.sourceRange() );

		final PBasicNode element = assertBasicNode( root.children().get( 1 ) );
		assertEquals( new SourceRange( 13, 37 ), element.sourceRange() );

		final PHTMLNode after = assertInstanceOf( PHTMLNode.class, root.children().get( 2 ) );
		assertEquals( new SourceRange( 37, html.length() ), after.sourceRange() );
	}

	@Test
	public void sourceRangeForRawDirective() throws Exception {
		final String html = "<p:raw>verbatim content</p:raw>";
		final PRootNode root = parse( html, "" );
		final PRawNode raw = assertInstanceOf( PRawNode.class, root.children().getFirst() );
		assertEquals( new SourceRange( 0, html.length() ), raw.sourceRange() );
	}

	@Test
	public void sourceRangeForCommentDirective() throws Exception {
		final String html = "<p:comment>hidden</p:comment>";
		final PRootNode root = parse( html, "" );
		final PCommentNode comment = assertInstanceOf( PCommentNode.class, root.children().getFirst() );
		assertEquals( new SourceRange( 0, html.length() ), comment.sourceRange() );
	}

	// ---- Helpers ----

	private static PRootNode parse( final String html, final String wod ) throws NGDeclarationFormatException, NGHTMLFormatException {
		final PNode result = new NGTemplateParser( html, wod ).parse();
		return assertInstanceOf( PRootNode.class, result );
	}

	private static PRootNode parseWithNamespaces( final String html, final String wod, final Set<String> namespaces ) throws NGDeclarationFormatException, NGHTMLFormatException {
		final PNode result = new NGTemplateParser( html, wod, namespaces ).parse();
		return assertInstanceOf( PRootNode.class, result );
	}

	private static void assertHTML( final String expected, final PNode node ) {
		final PHTMLNode html = assertInstanceOf( PHTMLNode.class, node );
		assertEquals( expected, html.value() );
	}

	private static PBasicNode assertBasicNode( final PNode node ) {
		return assertInstanceOf( PBasicNode.class, node );
	}

	private static void assertValueBinding( final boolean expectedIsQuoted, final String expectedValue, final NGBindingValue binding ) {
		final NGBindingValue.Value value = assertInstanceOf( NGBindingValue.Value.class, binding );
		assertEquals( expectedIsQuoted, value.isQuoted() );
		assertEquals( expectedValue, value.value() );
	}

	private static void assertBooleanBinding( final NGBindingValue binding ) {
		assertInstanceOf( NGBindingValue.BooleanPresence.class, binding );
	}

	/**
	 * Parses the same template with both old and new parsers and verifies structural equivalence
	 */
	private static void compareWithOldParser( final String html, final String wod ) throws Exception {
		final PRootNode newResult = (PRootNode)new NGTemplateParser( html, wod ).parse();
		final PRootNode oldResult = (PRootNode)new ng.appserver.templating.parser.legacy.NGTemplateParser( html, wod ).parse();
		assertNodesEqual( oldResult, newResult, "" );
	}

	private static void assertNodesEqual( final PNode expected, final PNode actual, final String path ) {
		assertEquals( expected.getClass(), actual.getClass(), "Node type mismatch at " + path );

		switch( expected ) {
			case PRootNode e -> {
				final PRootNode a = (PRootNode)actual;
				assertChildrenEqual( e.children(), a.children(), path + "/root" );
			}
			case PHTMLNode e -> {
				final PHTMLNode a = (PHTMLNode)actual;
				assertEquals( e.value(), a.value(), "HTML content mismatch at " + path );
			}
			case PBasicNode e -> {
				final PBasicNode a = (PBasicNode)actual;
				assertEquals( e.namespace(), a.namespace(), "Namespace mismatch at " + path );
				assertEquals( e.type(), a.type(), "Type mismatch at " + path );
				assertEquals( e.isInline(), a.isInline(), "isInline mismatch at " + path );
				assertBindingsEqual( e.bindings(), a.bindings(), path );
				assertChildrenEqual( e.children(), a.children(), path + "/" + e.type() );
			}
			case PRawNode e -> {
				final PRawNode a = (PRawNode)actual;
				assertEquals( e.value(), a.value(), "Raw content mismatch at " + path );
			}
			case PCommentNode e -> {
				final PCommentNode a = (PCommentNode)actual;
				assertEquals( e.value(), a.value(), "Comment content mismatch at " + path );
			}
		}
	}

	private static void assertChildrenEqual( final List<PNode> expected, final List<PNode> actual, final String path ) {
		assertEquals( expected.size(), actual.size(), "Children count mismatch at " + path );

		for( int i = 0; i < expected.size(); i++ ) {
			assertNodesEqual( expected.get( i ), actual.get( i ), path + "[" + i + "]" );
		}
	}

	/**
	 * Compares binding maps between old and new parser, accounting for the known difference
	 * in how inline quoted values are stored:
	 * - Old parser: Value(isQuoted=false, "\"$name\"") — raw with quotes
	 * - New parser: Value(isQuoted=true, "$name") — quotes stripped
	 */
	private static void assertBindingsEqual( final Map<String, NGBindingValue> expected, final Map<String, NGBindingValue> actual, final String path ) {
		assertEquals( expected.keySet(), actual.keySet(), "Binding keys mismatch at " + path );

		for( final String key : expected.keySet() ) {
			final NGBindingValue expectedValue = expected.get( key );
			final NGBindingValue actualValue = actual.get( key );

			if( expectedValue instanceof NGBindingValue.Value ev && actualValue instanceof NGBindingValue.Value av ) {
				// Normalize: if old parser stored raw quotes, strip them for comparison
				String expectedStr = ev.value();

				if( !ev.isQuoted() && expectedStr.startsWith( "\"" ) && expectedStr.endsWith( "\"" ) ) {
					expectedStr = expectedStr.substring( 1, expectedStr.length() - 1 );
					assertTrue( av.isQuoted(), "Expected isQuoted=true for binding '%s' at %s".formatted( key, path ) );
				}
				else {
					assertEquals( ev.isQuoted(), av.isQuoted(), "isQuoted mismatch for binding '%s' at %s".formatted( key, path ) );
				}

				assertEquals( expectedStr, av.value(), "Binding value mismatch for '%s' at %s".formatted( key, path ) );
			}
			else {
				assertEquals( expectedValue, actualValue, "Binding mismatch for '%s' at %s".formatted( key, path ) );
			}
		}
	}
}
