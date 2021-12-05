package ng.adaptor.jetty.http2;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * FIXME: Further work on this is on hold until there's aarch_64 support for Conscrypt: https://github.com/google/conscrypt/issues/1034 // Hugi 2021-12-05
 *
 * Ripped from https://gist.github.com/ataylor284/7270580d3d46d89585f363f61b773536
 *
 * Create keystore with `keytool -keystore http2_keystore.jks -storepass password -noprompt -genkey -keyalg RSA -keypass password -alias jetty \
 *                          -dname CN=localhost,OU=dev,O=sonatype,L=home,ST=cloud,C=US -ext SAN=DNS:localhost,IP:127.0.0.1 -ext BC=ca:true`
 * Confirm http2 is being used with `curl -v --http2 --insecure https://localhost:8443`
 */

public class ExperimentalServer {

	public static void main( String[] args ) {
		Server server = new Server();

		ServletContextHandler context = new ServletContextHandler( server, "/", ServletContextHandler.SESSIONS );
		context.addServlet( new ServletHolder( new Servlet() ), "/" );
		server.setHandler( context );

		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme( "https" );
		httpConfig.setSecurePort( 8443 );

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath( "/Users/hugi/xxxxx-temp/http2_keystore.jks" );
		sslContextFactory.setKeyStorePassword( "password" );
		sslContextFactory.setKeyManagerPassword( "password" );
		sslContextFactory.setCipherComparator( HTTP2Cipher.COMPARATOR );
		sslContextFactory.setProvider( "Conscrypt" );

		HttpConfiguration httpsConfig = new HttpConfiguration( httpConfig );
		httpsConfig.addCustomizer( new SecureRequestCustomizer() );

		HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory( httpsConfig );

		// NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable()
		var alpn = new ALPNServerConnectionFactory();
		alpn.setDefaultProtocol( "h2" );
		SslConnectionFactory ssl = new SslConnectionFactory( sslContextFactory, alpn.getProtocol() );

		var http2Connector = new ServerConnector( server, ssl, alpn, h2, new HttpConnectionFactory( httpsConfig ) );
		http2Connector.setPort( 8443 );
		server.addConnector( http2Connector );

		// ALPN.debug=false // FIXME: Commented this out since there's no available ALPN class // Hugi 2021-12-05

		try {
			server.start();
			server.join();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}

	public static class Servlet extends HttpServlet {
		@Override
		public void doGet( HttpServletRequest req, HttpServletResponse resp ) {
			// resp.setcontentType = "text/plain";
			// resp.writer.write( "Hello, World!" );
		}
	}
}