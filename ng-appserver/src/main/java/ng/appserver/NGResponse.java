package ng.appserver;

import java.io.InputStream;
import java.util.List;

/**
 * FIXME: Need to decide what to do about responses of different types.
 * Is a string response the same type as a binary response or a streaming response, or should these have different implementations
 * Are responses even mutable? A mutable response should possibly have a different design or a builder
 * // Hugi 2022-06-05
 */

public interface NGResponse extends NGMessage, NGActionResults {

	public int status();

	public void setStatus( final int status );

	public List<NGCookie> cookies();

	public void addCookie( final NGCookie cookie );

	public void setContentInputStream( final InputStream inputStream, final long contentInputStreamLength );

	public InputStream contentInputStream();

	public long contentInputStreamLength();

	public void appendContentString( final String stringToAppend );

	public void setContentBytes( final byte[] contentBytes );

	public void setContentString( final String contentString );

	public long contentBytesLength();
}