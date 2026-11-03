package com.github.esrrhs.fakecore.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Netty-based WebSocket server. Preferred over the legacy Java-WebSocket {@link WSServer}.
 */
public class NettyWSServer
{
	private static final Logger log = LoggerFactory.getLogger(NettyWSServer.class);

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private Channel serverChannel;
	private int port;
	private String path = "/";
	private NettyWSHandler handler;

	public void ini(int port, NettyWSHandler handler)
	{
		ini(port, "/", handler);
	}

	public void ini(int port, String websocketPath, NettyWSHandler handler)
	{
		this.port = port;
		this.path = websocketPath == null || websocketPath.isEmpty() ? "/" : websocketPath;
		this.handler = handler == null ? new NettyWSHandler() : handler;
		this.bossGroup = new NioEventLoopGroup(1);
		this.workerGroup = new NioEventLoopGroup();
		listen();
	}

	private void listen()
	{
		final NettyWSHandler h = this.handler;
		final String wsPath = this.path;
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch)
					{
						ChannelPipeline p = ch.pipeline();
						p.addLast(new HttpServerCodec());
						p.addLast(new HttpObjectAggregator(65536));
						p.addLast(new WebSocketServerProtocolHandler(wsPath, null, true));
						p.addLast(new SimpleChannelInboundHandler<WebSocketFrame>() {
							@Override
							public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
							{
								if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete)
								{
									h.onOpen(ctx.channel());
								}
								super.userEventTriggered(ctx, evt);
							}

							@Override
							protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame)
							{
								if (frame instanceof TextWebSocketFrame)
								{
									h.onMessage(ctx.channel(), ((TextWebSocketFrame) frame).text());
								}
								else if (frame instanceof BinaryWebSocketFrame)
								{
									ByteBuf content = frame.content();
									h.onMessage(ctx.channel(), content.retain());
								}
								else if (frame instanceof PingWebSocketFrame)
								{
									ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
								}
								else if (frame instanceof CloseWebSocketFrame)
								{
									ctx.close();
								}
							}

							@Override
							public void channelInactive(ChannelHandlerContext ctx) throws Exception
							{
								h.onClose(ctx.channel());
								super.channelInactive(ctx);
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

		try
		{
			ChannelFuture f = b.bind(port).sync();
			serverChannel = f.channel();
			h.onStart();
			log.info("NettyWSServer listening on port {} path {}", port, path);
		}
		catch (Exception e)
		{
			log.error("NettyWSServer listen fail", e);
			stop();
			throw new RuntimeException("NettyWSServer listen fail " + port, e);
		}
	}

	public void sendText(Channel channel, String text)
	{
		if (channel != null && channel.isActive())
		{
			channel.writeAndFlush(new TextWebSocketFrame(text));
		}
	}

	public void sendBinary(Channel channel, byte[] data)
	{
		if (channel != null && channel.isActive() && data != null)
		{
			channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
		}
	}

	public void sendMsg(Channel channel, Message msg)
	{
		if (msg == null || msg.getData() == null)
		{
			return;
		}
		sendText(channel, new String(msg.getData(), StandardCharsets.UTF_8));
	}

	public void close(Channel channel)
	{
		if (channel != null)
		{
			channel.close();
		}
	}

	public boolean isListening()
	{
		return serverChannel != null && serverChannel.isActive();
	}

	public int getPort()
	{
		return port;
	}

	public void stop()
	{
		try
		{
			if (serverChannel != null)
			{
				serverChannel.close().syncUninterruptibly();
				serverChannel = null;
			}
		}
		finally
		{
			if (bossGroup != null)
			{
				bossGroup.shutdownGracefully();
				bossGroup = null;
			}
			if (workerGroup != null)
			{
				workerGroup.shutdownGracefully();
				workerGroup = null;
			}
		}
		log.info("NettyWSServer stopped port {}", port);
	}
}
