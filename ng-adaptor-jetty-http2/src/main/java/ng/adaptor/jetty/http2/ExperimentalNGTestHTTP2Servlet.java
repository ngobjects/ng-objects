package ng.adaptor.jetty.http2;

import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jetty.server.Request;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.PushBuilder;

public class ExperimentalNGTestHTTP2Servlet extends HttpServlet {

	@Override
	protected void doGet( HttpServletRequest req, HttpServletResponse resp ) {

		PushBuilder pushBuilder = req.newPushBuilder();
		//		PushBuilder pushBuilder = Request.getBaseRequest( req ).newPushBuilder();

		pushBuilder
				.path( "images/kodedu-logo.png" )
				.addHeader( "content-type", "image/png" )
				.push();

		try( PrintWriter respWriter = resp.getWriter() ;) {
			respWriter.write( "<html>" +
					"<img src='images/kodedu-logo.png'>" +
					"</html>" );
		}
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}
}