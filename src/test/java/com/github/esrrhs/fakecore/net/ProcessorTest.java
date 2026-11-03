package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class ProcessorTest
{
	@Test
	void processesQueuedMessage() throws Exception
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> seen = new AtomicReference<>();
		Processor<String> processor = new Processor<>(8, new ProcessFunc<String>() {
			@Override
			public void process(String msg)
			{
				seen.set(msg);
				latch.countDown();
			}
		});
		processor.setDaemon(true);
		processor.start();
		processor.putMsg("hello");
		assertTrue(latch.await(2, TimeUnit.SECONDS));
		assertEquals("hello", seen.get());
	}

	@Test
	void putMsgSurvivesFullQueue() throws Exception
	{
		CountDownLatch block = new CountDownLatch(1);
		CountDownLatch entered = new CountDownLatch(1);
		Processor<Integer> processor = new Processor<>(1, new ProcessFunc<Integer>() {
			@Override
			public void process(Integer msg)
			{
				entered.countDown();
				try
				{
					block.await(2, TimeUnit.SECONDS);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}
		});
		processor.setDaemon(true);
		processor.start();
		processor.putMsg(1);
		assertTrue(entered.await(2, TimeUnit.SECONDS));
		// queue size 1, worker blocked holding current; offer may succeed or time out — must not throw
		processor.putMsg(2);
		processor.putMsg(3);
		block.countDown();
	}
}
