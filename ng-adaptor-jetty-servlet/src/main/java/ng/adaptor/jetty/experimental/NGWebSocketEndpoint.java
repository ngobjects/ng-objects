package ng.adaptor.jetty.experimental;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;

public class NGWebSocketEndpoint extends Endpoint implements MessageHandler.Whole<String> {

	public static List<NGWebSocketEndpoint> allEndpoints = new ArrayList<>();

	private static final Logger logger = LoggerFactory.getLogger( NGWebSocketEndpoint.class );
	public Session _session;
	public RemoteEndpoint.Async _remoteEndpoint;

	@Override
	public void onOpen( Session session, EndpointConfig config ) {
		_session = session;
		_remoteEndpoint = session.getAsyncRemote();
		logger.info( "Opened websocket: {}", session );
		session.addMessageHandler( this );
		_remoteEndpoint.sendText( "You are connected to " + this.getClass().getName() );

		allEndpoints.add( this );
	}

	@Override
	public void onClose( Session session, CloseReason close ) {
		super.onClose( session, close );
		_session = null;
		_remoteEndpoint = null;
		logger.info( "Closing websocket {} - {}", close.getCloseCode(), close.getReasonPhrase() );
	}

	@Override
	public void onMessage( String message ) {
		_remoteEndpoint.sendText( message );
		logger.info( "Client sent message: '{}'", message );
	}

	@Override
	public void onError( Session session, Throwable throwable ) {
		super.onError( session, throwable );
		logger.error( "websocket error", throwable );
	}
}