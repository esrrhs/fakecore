package com.github.esrrhs.fakecore.net;

import com.alibaba.fastjson.JSON;
import com.github.esrrhs.fakecore.Core;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by esrrhs on 2018/2/5.
 */
public class TcpClientThread extends Thread
{
	private static Logger log = LoggerFactory.getLogger(TcpClientThread.class);
	private String ip;
	private int port;
	private int gsId;
	private String gsName;
	private EventLoopGroup group;
	private Channel channel;
	private ArrayBlockingQueue<Message> queue;
	private TcpHandler tcpHandler;
	private MessageCodec messageCodec;
	private long createTime;

	public void ini(EventLoopGroup group, int gsId, String gsName, String ip, int port, int queueSize,
			TcpHandler tcpHandler, MessageCodec messageCodec)
	{
		this.ip = ip;
		this.gsId = gsId;
		this.gsName = gsName;
		this.port = port;
		this.group = group;
		this.queue = new ArrayBlockingQueue<Message>(queueSize);
		this.tcpHandler = tcpHandler;
		this.messageCodec = messageCodec;
		this.createTime = System.currentTimeMillis();
		this.start();
	}

	public void sendMsg(Message message)
	{
		try
		{
			queue.offer(message, 100, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			log.error("sendMsg ", e);
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				if (!isConnected())
				{
					connect();
					if (!isConnected())
					{
						Thread.sleep(1000);
						continue;
					}
					handShake();
				}

				if (queue.isEmpty())
				{
					sleep(10);
					continue;
				}

				for (int i = 0; i < 100; i++)
				{
					Message msg = queue.poll();
					if (msg != null)
					{
						channel.write(msg);
					}
					else
					{
						break;
					}
				}
				channel.flush();
			}
			catch (Exception e)
			{
				log.error("TcpClient run error " + ip + " " + port, e);
			}
		}
	}

	private void handShake()
	{
		Message msg = new Message();
		msg.setId(Message.MSG_ID_HANDSHAKE);
		Message.HandShakeData handShakeData = new Message.HandShakeData();
		handShakeData.gsId = Core.getGsId();
		msg.setData(JSON.toJSONString(handShakeData).getBytes());

		channel.writeAndFlush(msg);
	}

	private void connect()
	{
		try
		{
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception
				{
					int heartbeatTime = 3;
					// channel上多久没有写数据就认为连接失效了
					int downTime = 20;
					ch.pipeline().addLast(new IdleStateHandler(heartbeatTime, heartbeatTime, heartbeatTime))
							.addLast(new WriteTimeoutHandler(downTime)).addLast(new ReadTimeoutHandler(downTime))
							.addLast(new MsgDecoder(messageCodec)).addLast(new MsgEncoder(messageCodec))
							.addLast(tcpHandler);
				}
			}).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
					.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000).option(ChannelOption.TCP_NODELAY, true)
					.option(ChannelOption.SO_LINGER, 0).option(ChannelOption.SO_RCVBUF, 8 * 1024 * 1024)
					.option(ChannelOption.SO_SNDBUF, 8 * 1024 * 1024).option(ChannelOption.MAX_MESSAGES_PER_READ, 1000);

			SocketAddress address = new InetSocketAddress(ip, port);

			ChannelFuture f = b.connect(address).sync();
			if (f.isSuccess())
			{
				log.info("connect {} ok", f.channel().remoteAddress());
				channel = f.channel();
			}
			else
			{
				channel = null;
			}
		}
		catch (Exception e)
		{
			log.info("connect fail", e.toString());
			channel = null;
		}
	}

	public boolean isConnected()
	{
		return channel == null ? false : channel.isActive();
	}

	public int getGsId()
	{
		return gsId;
	}

	public long getCreateTime()
	{
		return createTime;
	}

	public String getIp()
	{
		return ip;
	}

	public int getPort()
	{
		return port;
	}

	public String getGsName()
	{
		return gsName;
	}

	public void closeChannel()
	{
		log.info("close channel {} {} ", ip, port);

		if (channel != null)
		{
			channel.close();
		}

		channel = null;
	}

}
