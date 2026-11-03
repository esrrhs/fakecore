package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ProcessorPoolTest
{
	@Test
	void routesByHashAndPreservesOrderingPerBucket() throws Exception
	{
		int size = 4;
		CountDownLatch latch = new CountDownLatch(20);
		ConcurrentHashMap<Integer, AtomicInteger> last = new ConcurrentHashMap<>();
		AtomicInteger orderViolations = new AtomicInteger();

		ProcessorPool<int[]> pool = new ProcessorPool<>(size, 64, new ProcessFunc<int[]>() {
			@Override
			public void process(int[] msg)
			{
				int hash = msg[0];
				int seq = msg[1];
				int bucket = Math.abs(hash) % size;
				AtomicInteger prev = last.computeIfAbsent(bucket, k -> new AtomicInteger(-1));
				int p = prev.getAndSet(seq);
				if (p >= seq)
				{
					orderViolations.incrementAndGet();
				}
				latch.countDown();
			}
		});

		for (int i = 0; i < 20; i++)
		{
			int hash = i % 2; // two keys -> two buckets
			pool.putMsg(hash, new int[] { hash, i });
		}

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertEquals(0, orderViolations.get());
		assertEquals(size, pool.getProcessorsSize());
	}
}
