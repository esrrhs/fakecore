package com.github.esrrhs.fakecore.net;

import com.alibaba.fastjson.JSON;
import com.github.esrrhs.fakecore.Core;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by esrrhs on 2018/2/5.
 */
@ChannelHandler.Sharable
public class TcpHandler extends SimpleChannelInboundHandler<Message>
{
	private static Logger log = LoggerFactory.getLogger(TcpHandler.class);

	private TcpObject host;

	public TcpHandler(TcpObject host)
	{
		this.host = host;
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception
	{

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception
	{
		if (message.getId() == Message.MSG_ID_PING)
		{
			processPing(ctx.channel(), message);
			return;
		}
		else if (message.getId() == Message.MSG_ID_PONG)
		{
			processPong(ctx.channel(), message);
			return;
		}
		else if (message.getId() == Message.MSG_ID_HANDSHAKE)
		{
			processHandShake(ctx.channel(), message);
			return;
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
	{
		if (IdleStateEvent.class.isAssignableFrom(evt.getClass()))
		{
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.WRITER_IDLE)
			{
				long now = System.currentTimeMillis();

				log.info("WRITER IDLE {}", now);

				Message msg = new Message();
				msg.setId(Message.MSG_ID_PING);
				Message.PingPongData pingPongData = new Message.PingPongData();
				pingPongData.time = System.currentTimeMillis();
				pingPongData.gsId = Core.getGsId();
				msg.setData(JSON.toJSONString(pingPongData).getBytes());

				if (host instanceof TcpClient)
				{
					((TcpClient) host).sendMsg(msg);
				}
			}
		}
	}

	private void processHandShake(Channel channel, Message message)
	{
		Message.HandShakeData handShakeData = JSON.parseObject(message.getData(), Message.HandShakeData.class);
		log.info("handshake from {} {}", channel.remoteAddress(), handShakeData.gsId);
	}

	private void processPing(Channel channel, Message message)
	{
		Message.PingPongData pingPongData = JSON.parseObject(message.getData(), Message.PingPongData.class);

		Message msg = new Message();

		msg.setId(Message.MSG_ID_PONG);
		msg.setData(message.getData());

		channel.writeAndFlush(msg);

		log.info("ping from {} {}", channel.remoteAddress(), pingPongData.gsId);
	}

	private void processPong(Channel channel, Message msg)
	{
		Message.PingPongData pingPongData = JSON.parseObject(msg.getData(), Message.PingPongData.class);
		log.info("pong from {} {}", channel.remoteAddress(), System.currentTimeMillis() - pingPongData.time);
	}

}
