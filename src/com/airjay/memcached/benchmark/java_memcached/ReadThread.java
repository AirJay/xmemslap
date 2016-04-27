package com.airjay.memcached.benchmark.java_memcached;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.exception.MemcachedException;

import com.danga.MemCached.MemCachedClient;

public class ReadThread extends Thread {

	protected MemCachedClient memcachedClient;
	protected CyclicBarrier barrier;
	private Map<String, Long> costMapPerThread;

	public ReadThread(MemCachedClient memcachedClient, CyclicBarrier barrier) {
		this.memcachedClient = memcachedClient;
		this.barrier = barrier;
	}

	public void run() {

		try {
			barrier.await();

			this.setCostMapPerThread(getKey(memcachedClient, Cli.repeatCount,
					Cli.opTimeout));

			barrier.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Map<String, Long> getKey(MemCachedClient memcachedClient,
			int repeats, int opTimeout) throws TimeoutException,
			InterruptedException, MemcachedException, ExecutionException {

		long avgGetCostPerThread = 0;
		long maxGetCostPerThread = Long.MIN_VALUE;
		long minGetCostPerThread = Long.MAX_VALUE;
		long sumGetCostPerThread = 0;
		Map<String, Long> costMapPerThread = new HashMap<String, Long>();

		for (int i = 1; i <= repeats; i++) {
			String key = "couchbase-check-noc-" + i;
			long startTime = System.nanoTime();
			memcachedClient.get(key);
			long estimatedTime = System.nanoTime() - startTime;

			sumGetCostPerThread = sumGetCostPerThread + estimatedTime;
			avgGetCostPerThread = sumGetCostPerThread / i;
			maxGetCostPerThread = maxGetCostPerThread > estimatedTime ? maxGetCostPerThread
					: estimatedTime;
			minGetCostPerThread = minGetCostPerThread < estimatedTime ? minGetCostPerThread
					: estimatedTime;

		}

		costMapPerThread.put("avgGetCostPerThread", avgGetCostPerThread);
		costMapPerThread.put("maxGetCostPerThread", maxGetCostPerThread);
		costMapPerThread.put("minGetCostPerThread", minGetCostPerThread);

		return costMapPerThread;
	}

	public Map<String, Long> getCostMapPerThread() {
		return costMapPerThread;
	}

	public void setCostMapPerThread(Map<String, Long> costMapPerThread) {
		this.costMapPerThread = costMapPerThread;
	}

}
