package ng.appserver.wointegration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lives alongside the application and sends regular "lifebeats" (as in "Hello, I'm still here) to the wotaskd).
 *
 * Also manages other communication with wotaskd
 * - hasStarted
 * - willStop
 * - willCrash
 */

public class NGLifebeatThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger( NGLifebeatThread.class );

	/**
	 * Stores the value of the response to the last sent lifebeat (FIXME: I think)
	 */
	private final byte[] lifebeatResponseBuffer = new byte["HTTP/1.X XXX".length() + " Apple WebObjects\r\n".length() + "\r\n\r\n".length()];

	private final InetAddress _localAddress;
	private final int _lifebeatDestinationPort;
	private final long _lifebeatIntervalMS;

	private int _deathCounter;
	private Socket lifebeatSocket;
	private OutputStream lifebeatOS;
	private InputStream lifebeatIS;
	private DatagramSocket datagramSocket;
	private final byte[] _buffer = new byte[1000];
	private DatagramPacket incomingDatagramPacket;
	private DatagramPacket outgoingDatagramPacket;
	public MessageGenerator _messageGenerator;

	/**
	 * FIXME: Yeah, this sucks
	 */
	public static class MessageGenerator {
		private byte[] _hasStarted;
		private byte[] _lifebeat;
		public byte[] _willStop;
		private byte[] _willCrash;
		private byte[] _versionRequest;

		public MessageGenerator( final String appName, final String localhostName, final int appPort ) {
			final String preString = "GET /cgi-bin/WebObjects/wotaskd.woa/wlb?";
			final String postString = "&" + appName + "&" + localhostName + "&" + appPort + " HTTP/1.1\r\n\r\n";
			final String versionString = WOMPRequestHandler.KEY + "://queryVersion";

			_hasStarted = (preString + "hasStarted" + postString).getBytes();
			_lifebeat = (preString + "lifebeat" + postString).getBytes();
			_willStop = (preString + "willStop" + postString).getBytes();
			_willCrash = (preString + "willCrash" + postString).getBytes();
			_versionRequest = versionString.getBytes();
		}
	}

	public NGLifebeatThread( final String appName, final int appPort, final InetAddress appHost, final int lifebeatDestinationPort, final long lifebeatIntervalMS ) {
		logger.info( "Attempting to create LifebeatThread: {}, {}, {}, {}, {} ", appName, appPort, appHost, lifebeatDestinationPort, lifebeatIntervalMS );

		Objects.requireNonNull( appName );

		if( appPort < 1 ) {
			throw new IllegalArgumentException( "appPort must be a positive number" );
		}

		Objects.requireNonNull( appHost );

		if( lifebeatDestinationPort < 1 ) {
			throw new IllegalArgumentException( "lifebeatDestinationPort must be a positive number" );
		}

		if( lifebeatIntervalMS < 1 ) {
			throw new IllegalArgumentException( "lifebeatIntervalMS must be a positive number" );
		}

		_lifebeatDestinationPort = lifebeatDestinationPort;
		_lifebeatIntervalMS = lifebeatIntervalMS;

		_localAddress = appHost;

		setName( "LifebeatSendReceiveThread" );

		_messageGenerator = new MessageGenerator( appName, appHost.getHostName(), appPort );
	}

	public void sendMessage( byte[] aMessage ) {
		Objects.requireNonNull( aMessage );

		try {
			if( lifebeatSocket == null ) {
				logger.debug( "Creating new lifebeat socket" );

				lifebeatSocket = new Socket( _localAddress, _lifebeatDestinationPort, _localAddress, 0 );
				lifebeatSocket.setTcpNoDelay( true );
				lifebeatSocket.setSoLinger( false, 0 );
				lifebeatIS = lifebeatSocket.getInputStream();
				lifebeatOS = lifebeatSocket.getOutputStream();
			}

			// write the message
			lifebeatOS.write( aMessage );
			lifebeatOS.flush();

			// read response
			// 200 == OK, 400 == Bad Request, 500 == Force Quit
			int fetched = 0;
			int thisFetch = -1;
			while( fetched < lifebeatResponseBuffer.length ) {
				thisFetch = lifebeatIS.read( lifebeatResponseBuffer, fetched, lifebeatResponseBuffer.length - fetched );
				if( thisFetch != -1 ) {
					fetched += thisFetch;
				}
				else {
					break;
				}
			}

			if( (thisFetch == -1) || (lifebeatResponseBuffer[9] == '4') ) {
				// Trash this connection and create a new one
				// Note this means that we don't support 5.2 apps talking to 5.1 wotaskd
				// we'll increment the deathCounter each time!
				_closeLifebeatSocket();
			}
			else if( lifebeatResponseBuffer[9] == '5' ) {
				try {
					logger.info( "Force Quit received. Exiting now..." );
					// Send a crash message if we can
					lifebeatSocket = new Socket( _localAddress, _lifebeatDestinationPort, _localAddress, 0 );
					lifebeatOS = lifebeatSocket.getOutputStream();
					lifebeatOS.write( _messageGenerator._willCrash );
					lifebeatOS.flush();
					_closeLifebeatSocket();
				}
				finally {
					// OK to exit - code unused in Servlet Containers
					System.exit( 1 );
				}
			}
			else {
				_deathCounter = 0;
			}
		}
		catch( final java.io.IOException e ) {
			logger.debug( "Exception sending lifebeat to wotaskd: " + e );
			_closeLifebeatSocket();
		}
	}

	private void _closeLifebeatSocket() {
		// Closing everything
		lifebeatOS = null;
		lifebeatIS = null;
		if( lifebeatSocket != null ) {
			try {
				lifebeatSocket.close();
			}
			catch( final IOException ioe ) {
				logger.debug( "Exception closing lifebeat socket: " + ioe );
			}
			lifebeatSocket = null;
		}

		_deathCounter++;
	}

	private void udpMessage() {
		try {
			datagramSocket.send( outgoingDatagramPacket );
			incomingDatagramPacket.setLength( _buffer.length );
			datagramSocket.receive( incomingDatagramPacket );
			final String reply = new String( incomingDatagramPacket.getData(), StandardCharsets.UTF_8 );
			if( reply.startsWith( WOMPRequestHandler.KEY ) ) {
				_deathCounter = 0;
			}
		}
		catch( final Throwable e ) {
			logger.debug( "Exception checking for wotaskd using UDP: " + e );
		}
	}

	@Override
	public void run() {

		boolean _udpSocketNotAvailable = false;

		try {
			datagramSocket = new DatagramSocket( 0, _localAddress );
			datagramSocket.setSoTimeout( 5000 );
			outgoingDatagramPacket = new DatagramPacket( _messageGenerator._versionRequest, _messageGenerator._versionRequest.length, _localAddress, _lifebeatDestinationPort );
			incomingDatagramPacket = new DatagramPacket( _buffer, _buffer.length );
		}
		catch( final SocketException e ) {
			logger.error( "<_LifebeatThread> Exception creating datagramSocket ", e );
			_udpSocketNotAvailable = true;
		}

		sendMessage( _messageGenerator._hasStarted );

		try {
			Thread.sleep( _lifebeatIntervalMS );
		}
		catch( final InterruptedException ex ) {
			logger.debug( "Comms failure", ex );
		}

		while( true ) {
			// FIXME: Document what exactly we're doing here
			if( _deathCounter < 10 || _udpSocketNotAvailable ) {
				sendMessage( _messageGenerator._lifebeat );
			}
			else {
				udpMessage();
			}

			try {
				Thread.sleep( _lifebeatIntervalMS );
			}
			catch( final InterruptedException ex ) {
				logger.debug( "Thread interrupted", ex );
			}
		}
	}
}