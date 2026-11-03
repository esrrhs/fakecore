package com.github.esrrhs.fakecore.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Netty-based WebSocket client. Preferred over the legacy Java-WebSocket {@link WSClient}.
 */
public class NettyWSClient
{
	private static final Logger log = LoggerFactory.getLogger(NettyWSClient.class);

	private EventLoopGroup group;
	private Channel channel;
	private String host;
	private int port;
	private String path = "/";
	private NettyWSHandler handler;
	private volatile boolean stopped;

	public void ini(String host, int port, NettyWSHandler handler)
	{
		ini(host, port, "/", handler);
	}

	public void ini(String host, int port, String websocketPath, NettyWSHandler handler)
	{
		this.host = host;
		this.port = port;
		this.path = websocketPath == null || websocketPath.isEmpty() ? "/" : websocketPath;
		this.handler = handler == null ? new NettyWSHandler() : handler;
		this.group = new NioEventLoopGroup(1);
		this.stopped = false;
		connect();
	}

	private void connect()
	{
		try
		{
			URI uri = new URI("ws://" + host + ":" + port + path);
			final WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri,
					WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
			final NettyWSHandler h = this.handler;

			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch)
				{
					ChannelPipeline p = ch.pipeline();
					p.addLast(new HttpClientCodec());
					p.addLast(new HttpObjectAggregator(65536));
					p.addLast(new SimpleChannelInboundHandler<Object>() {
						private WebSocketClientHandshaker hs = handshaker;

						@Override
						public void channelActive(ChannelHandlerContext ctx)
						{
							hs.handshake(ctx.channel());
						}

						@Override
						protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception
						{
							Channel ch = ctx.channel();
							if (!hs.isHandshakeComplete())
							{
								hs.finishHandshake(ch, (io.netty.handler.codec.http.FullHttpResponse) msg);
								h.onOpen(ch);
								return;
							}
							if (msg instanceof TextWebSocketFrame)
							{
								h.onMessage(ch, ((TextWebSocketFrame) msg).text());
							}
							else if (msg instanceof BinaryWebSocketFrame)
							{
								ByteBuf content = ((BinaryWebSocketFrame) msg).content();
								h.onMessage(ch, content.retain());
							}
							else if (msg instanceof PingWebSocketFrame)
							{
								ch.writeAndFlush(new PongWebSocketFrame(((PingWebSocketFrame) msg).content().retain()));
							}
							else if (msg instanceof CloseWebSocketFrame)
							{
								ch.close();
							}
						}

						@Override
						public void channelInactive(ChannelHandlerContext ctx) throws Exception
						{
							h.onClose(ctx.channel());
							super.channelInactive(ctx);
							scheduleReconnect();
						}

						@Override
						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
						{
							h.onError(ctx.channel(), cause);
							ctx.close();
						}
					});
				}
			});

			ChannelFuture f = b.connect(host, port).sync();
			channel = f.channel();
			log.info("NettyWSClient connecting {}:{}{}", host, port, path);
		}
		catch (Exception e)
		{
			log.error("NettyWSClient connect fail {}:{}", host, port, e);
			scheduleReconnect();
		}
	}

	private void scheduleReconnect()
	{
		if (stopped || group == null)
		{
			return;
		}
		group.schedule(new Runnable() {
			@Override
			public void run()
			{
				if (!stopped)
				{
					log.info("NettyWSClient reconnect {}:{}", host, port);
					connect();
				}
			}
		}, 1, TimeUnit.SECONDS);
	}

	public void sendText(String text)
	{
		Channel ch = channel;
		if (ch != null && ch.isActive())
		{
			ch.writeAndFlush(new TextWebSocketFrame(text));
		}
	}

	public void sendBinary(byte[] data)
	{
		Channel ch = channel;
		if (ch != null && ch.isActive() && data != null)
		{
			ch.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
		}
	}

	public void sendMsg(Message message)
	{
		if (message == null || message.getData() == null)
		{
			return;
		}
		sendText(new String(message.getData(), StandardCharsets.UTF_8));
	}

	public boolean isConnected()
	{
		return channel != null && channel.isActive();
	}

	public void close()
	{
		stopped = true;
		if (channel != null)
		{
			channel.close();
			channel = null;
		}
		if (group != null)
		{
			group.shutdownGracefully();
			group = null;
		}
		log.info("NettyWSClient closed {}:{}", host, port);
	}
}
