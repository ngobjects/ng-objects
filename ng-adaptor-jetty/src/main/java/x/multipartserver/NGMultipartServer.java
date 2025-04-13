package x.multipartserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.http.MultiPartFormData;
import org.eclipse.jetty.http.MultiPartFormData.ContentSource;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.Content.Source;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;

public class NGMultipartServer {

	public static void main( String[] args ) {
		final Server server = new Server();

		final HttpConfiguration http = new HttpConfiguration();
		final HttpConnectionFactory http11 = new HttpConnectionFactory( http );

		final ServerConnector connector = new ServerConnector( server, http11 );
		connector.setPort( 1201 );
		server.addConnector( connector );
		server.setHandler( new MPHandler() );

		try {
			server.start();
		}
		catch( final Exception e ) {
			e.printStackTrace();
		}
	}

	public static class MPHandler extends Handler.Abstract {

		@Override
		public boolean handle( Request request, Response response, Callback callback ) throws Exception {
			doRequest( request, response, callback );
			return true;
		}

		private void doRequest( final Request jettyRequest, final Response jettyResponse, Callback callback ) throws IOException {
			final ContentSource cs = new MultiPartFormData.ContentSource( "12345" );
			cs.addPart( stringPart( "number1", "<h1>Hvað er að frétta!</h1>" ) );
			cs.addPart( stringPart( "number2", "Hér er annar partur" ) );
			cs.addPart( stringPart( "number3", "Hér er þriðji partur" ) );
			cs.close();

			jettyResponse.getHeaders().add( "content-type", "multipart/form-data; boundary=12345" );
			jettyResponse.getHeaders().add( "Access-Control-Allow-Origin", "*" );

			Content.copy( cs, jettyResponse, callback );
		}
	}

	public static MultiPart.ContentSourcePart stringPart( final String name, final String content ) {
		final HttpFields httpFields = HttpFields.build().add( new HttpField( "content-disposition", "form-data; name=\"%s\"".formatted( name ) ) );
		final Source contentSource = Content.Source.from( new ByteArrayInputStream( content.getBytes() ) );
		return new MultiPart.ContentSourcePart( name, null, httpFields, contentSource );
	}
}