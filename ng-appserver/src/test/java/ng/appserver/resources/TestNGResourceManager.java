package ng.appserver.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ng.resources.NGResourceStamps;

public class TestNGResourceManager {

	@Test
	public void eachResourceTypeHasItsOwnStamp() {
		final TestResourceManager manager = new TestResourceManager( true );
		manager.put( StandardResourceType.WebServer, "site.css", "webserver" );
		manager.put( StandardResourceType.Public, "site.css", "public" );

		assertEquals( stamp( "webserver" ), manager.webserverResourceStamp( "app", "site.css" ) );
		assertEquals( stamp( "public" ), manager.publicResourceStamp( "app", "site.css" ) );
	}

	@Test
	public void aPrecedingSlashNamesTheSameResource() {
		final TestResourceManager manager = new TestResourceManager( true );
		manager.put( StandardResourceType.Public, "robots.txt", "User-agent: *" );

		assertEquals( stamp( "User-agent: *" ), manager.publicResourceStamp( "app", "/robots.txt" ) );
		assertEquals( stamp( "User-agent: *" ), manager.publicResourceStamp( "app", "robots.txt" ) );
	}

	@Test
	public void aMissingResourceHasNoStamp() {
		assertNull( new TestResourceManager( true ).publicResourceStamp( "app", "nothing.txt" ) );
		assertNull( new TestResourceManager( false ).webserverResourceStamp( "app", "nothing.txt" ) );
	}

	@Test
	public void stampsAreRememberedWhenCaching() {
		final TestResourceManager manager = new TestResourceManager( true );
		manager.put( StandardResourceType.Public, "robots.txt", "before" );
		manager.publicResourceStamp( "app", "robots.txt" );
		manager.put( StandardResourceType.Public, "robots.txt", "after" );

		assertEquals( stamp( "before" ), manager.publicResourceStamp( "app", "robots.txt" ) );
	}

	@Test
	public void stampsAreComputedEachTimeWhenNotCaching() {
		final TestResourceManager manager = new TestResourceManager( false );
		manager.put( StandardResourceType.Public, "robots.txt", "before" );
		final String before = manager.publicResourceStamp( "app", "robots.txt" );
		manager.put( StandardResourceType.Public, "robots.txt", "after" );

		assertNotEquals( before, manager.publicResourceStamp( "app", "robots.txt" ) );
		assertEquals( stamp( "after" ), manager.publicResourceStamp( "app", "robots.txt" ) );
	}

	private static String stamp( final String content ) {
		return NGResourceStamps.stamp( content.getBytes( StandardCharsets.UTF_8 ) );
	}

	/**
	 * A resource manager with in-memory resources in the 'app' namespace, caching or not without an application
	 */
	private static class TestResourceManager extends NGResourceManager {

		private final boolean _cachingEnabled;
		private final Map<ResourceType, Map<String, String>> _contents = new HashMap<>();

		TestResourceManager( final boolean cachingEnabled ) {
			_cachingEnabled = cachingEnabled;

			for( final StandardResourceType type : StandardResourceType.values() ) {
				final Map<String, String> contents = new HashMap<>();
				_contents.put( type, contents );
				resourceLoader().addResourceSource( "app", type, path -> Optional.ofNullable( contents.get( path ) ).map( content -> NGResource.of( () -> new ByteArrayInputStream( content.getBytes( StandardCharsets.UTF_8 ) ) ) ) );
			}
		}

		void put( final ResourceType type, final String path, final String content ) {
			_contents.get( type ).put( path, content );
		}

		@Override
		protected boolean cachingEnabled() {
			return _cachingEnabled;
		}
	}
}
