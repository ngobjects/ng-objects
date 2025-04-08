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

			final HttpFields httpFields1 = HttpFields.build().add( new HttpField( "content-disposition", "form-data; name=\"smu\"" ) );
			final Source contentSource1 = Content.Source.from( new ByteArrayInputStream( "Þetta er fyrri strengurinn".getBytes() ) );

			final HttpFields httpFields2 = HttpFields.build().add( new HttpField( "content-disposition", "form-data; name=\"bork\"" ) );
			final Source contentSource2 = Content.Source.from( new ByteArrayInputStream( "Þetta er seinni strengurinn".getBytes() ) );

			final ContentSource cs = new MultiPartFormData.ContentSource( "12345" );
			cs.addPart( new MultiPart.ContentSourcePart( null, null, httpFields1, contentSource1 ) );
			cs.addPart( new MultiPart.ContentSourcePart( null, null, httpFields2, contentSource2 ) );
			cs.close();

			jettyResponse.getHeaders().add( "content-type", "multipart/form-data; boundary=12345" );
			jettyResponse.getHeaders().add( "Access-Control-Allow-Origin", "*" );

			Content.copy( cs, jettyResponse, callback );
		}
	}
}