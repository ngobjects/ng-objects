package ng.appserver.templating.definitions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Reading an element's .apiext: its docs, content policy and bindings.
 */
public class TestNGElementAPI {

	private static NGElementAPI parse( final String xml ) {
		return NGElementAPI.parse( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ), "test" );
	}

	@Test
	public void elementAndBindings() {
		final NGElementAPI api = parse( """
				<wodefinitions>
					<wo class="NGRepetition" content="expected" unknownAttributes="forbidden">
						<doc><![CDATA[Renders its content once per item of `list`.]]></doc>
						<binding name="list" required="true">
							<pull><type>java.util.List</type><type>java.util.SequencedCollection</type></pull>
							<doc>The objects</doc>
						</binding>
						<binding name="item">
							<push><type>java.lang.Object</type></push>
						</binding>
						<binding name="method">
							<pull><type>java.lang.String</type></pull>
							<default>POST</default>
							<deprecated>Use verb</deprecated>
						</binding>
						<choose min="1" max="1">
							<binding name="list"/>
							<binding name="count"/>
						</choose>
					</wo>
				</wodefinitions>
				""" );

		assertEquals( "Renders its content once per item of `list`.", api.doc() );
		assertTrue( api.wrapsContent() );
		assertEquals( "forbidden", api.unknownAttributes() );
		assertEquals( List.of( "list", "item", "method" ), api.bindings().stream().map( NGElementAPI.Binding::name ).toList(), "constraint references aren't bindings" );

		final NGElementAPI.Binding list = api.bindings().get( 0 );
		assertTrue( list.required() );
		assertEquals( List.of( "java.util.List", "java.util.SequencedCollection" ), list.pullTypes() );
		assertFalse( list.isPushed() );
		assertEquals( "The objects", list.doc() );

		final NGElementAPI.Binding item = api.bindings().get( 1 );
		assertTrue( item.isPushed() );
		assertFalse( item.isPulled() );
		assertNull( item.doc() );

		final NGElementAPI.Binding method = api.bindings().get( 2 );
		assertEquals( "POST", method.defaultValue() );
		assertEquals( "Use verb", method.deprecated() );
	}

	@Test
	public void deprecatedWithoutANoteIsStillDeprecated() {
		assertTrue( parse( "<wodefinitions><wo class=\"X\"><deprecated/></wo></wodefinitions>" ).isDeprecated() );
		assertFalse( parse( "<wodefinitions><wo class=\"X\"/></wodefinitions>" ).isDeprecated() );
	}

	@Test
	public void contentPolicy() {
		assertTrue( parse( "<wodefinitions><wo class=\"X\" content=\"allowed\"/></wodefinitions>" ).wrapsContent() );
		assertFalse( parse( "<wodefinitions><wo class=\"X\" content=\"forbidden\"/></wodefinitions>" ).wrapsContent() );
		assertFalse( parse( "<wodefinitions><wo class=\"X\"/></wodefinitions>" ).wrapsContent() );
	}

	@Test
	public void aFullyQualifiedClass() {
		assertEquals( "ng.appserver.templating.elements.NGString", parse( "<wodefinitions><wo class=\"ng.appserver.templating.elements.NGString\"/></wodefinitions>" ).className() );
	}

	@Test
	public void aSimpleNameOrNoClassIsNoClass() {
		assertNull( parse( "<wodefinitions><wo class=\"WOString\"/></wodefinitions>" ).className(), "a simple name isn't a class reference" );
		assertNull( parse( "<wodefinitions><wo content=\"allowed\"/></wodefinitions>" ).className(), "a template-only component" );
	}

}
