package com.github.esrrhs.fakecore.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by esrrhs on 2018/2/5.
 */
public class TcpServer extends TcpObject
{
	private static Logger log = LoggerFactory.getLogger(TcpServer.class);
	private EventLoopGroup bossEventLoopGroup;
	private EventLoopGroup workerEventLoopGroup;
	private Channel channel;
	private int port;
	private TcpHandler tcpHandler;
	private MessageCodec messageCodec;

	public void ini(int port, TcpHandler tcpHandler, MessageCodec messageCodec)
	{
		this.port = port;
		this.tcpHandler = tcpHandler;
		this.messageCodec = messageCodec;
		this.bossEventLoopGroup = new NioEventLoopGroup(1);
		this.workerEventLoopGroup = new NioEventLoopGroup();

		listen();
	}

	public void sendMsg(Channel channel, Message message)
	{
		channel.writeAndFlush(message);
	}

	public boolean listend()
	{
		return channel != null ? channel.isActive() : false;
	}

	private void listen()
	{
		System.setProperty("io.netty.allocator.type", "pooled");

		ServerBootstrap b = new ServerBootstrap();
		b.group(bossEventLoopGroup, workerEventLoopGroup).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception
					{
						ch.pipeline().addLast(new ReadTimeoutHandler(30)).addLast(new MsgDecoder(messageCodec))
								.addLast(new MsgEncoder(messageCodec)).addLast(tcpHandler);
					}
				}).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childOption(ChannelOption.AUTO_READ, true).childOption(ChannelOption.SO_LINGER, 0);

		ChannelFuture f = null;
		try
		{
			f = b.bind(port).sync();
		}
		catch (Exception e)
		{
			log.error("TcpServer listnening fail ", e);
			throw new RuntimeException("TcpServer listnening fail " + port);
		}
		channel = f.channel();
		log.info("TcpServer listnening on port {}", port);
	}

}
