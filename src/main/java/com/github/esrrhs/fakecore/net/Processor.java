package com.github.esrrhs.fakecore.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by esrrhs on 2018/2/7.
 */
public class Processor<T> extends Thread
{
	private static Logger log = LoggerFactory.getLogger(Processor.class);

	private ArrayBlockingQueue<T> queue;
	private ProcessFunc processFunc;

	public Processor(int queueSize, ProcessFunc processFunc)
	{
		this.queue = new ArrayBlockingQueue<>(queueSize);
		this.processFunc = processFunc;
	}

	public void putMsg(T message)
	{
		try
		{
			queue.offer(message, 100, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			log.error("putMsg ", e);
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				T msg = queue.take();
				if (msg != null)
				{
					processFunc.process(msg);
				}
			}
			catch (Exception e)
			{
				log.error("Processor run error ", e);
			}
		}
	}
}
