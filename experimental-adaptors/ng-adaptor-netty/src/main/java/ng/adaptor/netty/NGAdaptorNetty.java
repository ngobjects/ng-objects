package ng.adaptor.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import ng.appserver.NGAdaptor;

public class NGAdaptorNetty extends NGAdaptor {

	private int port;
	private static Logger logger = LoggerFactory.getLogger( NGAdaptorNetty.class );

	@Override
	public void start() {
		new NGAdaptorNetty().run();
	}

	public void run() {
		final EventLoopGroup bossGroup = new NioEventLoopGroup();
		final EventLoopGroup workerGroup = new NioEventLoopGroup();
		final ServerBootstrap b = new ServerBootstrap();

		b.group( bossGroup, workerGroup )
				.channel( NioServerSocketChannel.class )
				.handler( new LoggingHandler( LogLevel.INFO ) )
				.childHandler( new ChannelInitializer() {

					@Override
					protected void initChannel( Channel ch ) throws Exception {
						ChannelPipeline p = ch.pipeline();
						p.addLast( new HttpRequestDecoder() );
						p.addLast( new HttpResponseEncoder() );
						p.addLast( new CustomHttpServerHandler() );
					}
				} );
	}

	public static class CustomHttpServerHandler extends SimpleChannelInboundHandler {

		@Override
		protected void messageReceived( ChannelHandlerContext arg0, Object arg1 ) throws Exception {
			// TODO Auto-generated method stub

		}

	}
}