package ng.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Content stamps for resource URLs. A stamped resource name carries a stamp of the resource's content before its
 * extension, marked by an {@code @} ({@code css/site.css} as {@code css/site@3f9c1e07ab.css}), so the URL changes
 * whenever the content does and can be cached for good. The resource URL generator stamps the URLs it generates, and the
 * resource request handler serves a stamped name as the resource it names without the stamp; the unstamped name keeps
 * working.
 *
 * The stamp goes in the file name rather than in a folder or the query string: a relative reference in a stamped
 * stylesheet ({@code url(../img/logo.png)}) then resolves to the plain URL of what it references, and caches that ignore
 * query strings still tell stamps apart. The {@code @} sets the stamp apart from the name: only an {@code @} followed by
 * exactly {@value #LENGTH} hex digits is a stamp, so a density variant such as {@code logo@2x.png} stays a name (and is
 * stamped as {@code logo@2x@3f9c1e07ab.png}).
 *
 * Parsing also accepts {@code %40}, the marker as some tools encode it in a path.
 *
 * Shared by ng and wonder-slim, so both stamp and parse resource URLs the same way.
 */
public final class NGResourceStamps {

	/**
	 * The number of hex digits of the content's SHA-256 a stamp uses
	 */
	static final int LENGTH = 10;

	/**
	 * Marks the stamp in a stamped file name
	 */
	static final String MARKER = "@";

	/**
	 * A stamped file name: the name, the stamp after the marker (or the marker encoded, {@code %40}), and the extension if
	 * there is one
	 */
	private static final Pattern STAMPED_FILE_NAME = Pattern.compile( "^(.+)(?:@|%40)([0-9a-f]{" + LENGTH + "})(\\.[^.]+)?$" );

	/**
	 * A stamped resource path, and the path it names without the stamp
	 */
	public record Stamped( String unstampedPath, String stamp ) {}

	private NGResourceStamps() {}

	/**
	 * @return The stamp of the given content: the first {@value #LENGTH} hex digits of its SHA-256
	 */
	public static String stamp( final byte[] content ) {
		return stampFromDigest( sha256().digest( content ) );
	}

	/**
	 * @return The stamp of the content read from the given stream, read through in chunks rather than held in memory
	 */
	public static String stamp( final InputStream content ) throws IOException {
		final MessageDigest digest = sha256();
		content.transferTo( new DigestOutputStream( OutputStream.nullOutputStream(), digest ) );
		return stampFromDigest( digest.digest() );
	}

	/**
	 * @return The stamp for a SHA-256 digest: its first {@value #LENGTH} hex digits
	 */
	private static String stampFromDigest( final byte[] digest ) {
		return HexFormat.of().formatHex( digest ).substring( 0, LENGTH );
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance( "SHA-256" );
		}
		catch( NoSuchAlgorithmException e ) {
			throw new IllegalStateException( "Every Java platform has SHA-256", e );
		}
	}

	/**
	 * @return The given resource path with the stamp in its file name, before the extension ({@code css/site.css} to
	 *         {@code css/site@<stamp>.css}), or after the name if it has none
	 */
	public static String stampedPath( final String path, final String stamp ) {
		final int lastSlash = path.lastIndexOf( '/' );
		final String folder = path.substring( 0, lastSlash + 1 );
		final String fileName = path.substring( lastSlash + 1 );
		final int extensionStart = fileName.lastIndexOf( '.' );

		// No extension, or a dot file (".htaccess"), whose dot isn't one
		if( extensionStart <= 0 ) {
			return folder + fileName + MARKER + stamp;
		}

		return folder + fileName.substring( 0, extensionStart ) + MARKER + stamp + fileName.substring( extensionStart );
	}

	/**
	 * @return The given resource path's stamp and the path without it, null if the path's file name carries no stamp
	 */
	public static Stamped parse( final String path ) {
		final int lastSlash = path.lastIndexOf( '/' );
		final String fileName = path.substring( lastSlash + 1 );
		final Matcher matcher = STAMPED_FILE_NAME.matcher( fileName );

		if( !matcher.matches() ) {
			return null;
		}

		final String extension = matcher.group( 3 ) == null ? "" : matcher.group( 3 );
		return new Stamped( path.substring( 0, lastSlash + 1 ) + matcher.group( 1 ) + extension, matcher.group( 2 ) );
	}
}
