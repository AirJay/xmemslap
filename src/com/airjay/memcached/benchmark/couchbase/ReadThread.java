package com.airjay.memcached.benchmark.couchbase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.StringDocument;

public class ReadThread extends Thread {

	protected Bucket bucket;
	protected CyclicBarrier barrier;
	private Map<String, Long> costMapPerThread;

	public ReadThread(Bucket bucket, CyclicBarrier barrier) {
		this.bucket = bucket;
		this.barrier = barrier;
	}

	public void run() {

		try {
			barrier.await();

			this.setCostMapPerThread(getKey(bucket, Cli.repeatCount,
					Cli.opTimeout));

			barrier.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Map<String, Long> getKey(Bucket bucket, int repeats, int opTimeout) {

		long avgGetCostPerThread = 0;
		long maxGetCostPerThread = Long.MIN_VALUE;
		long minGetCostPerThread = Long.MAX_VALUE;
		long sumGetCostPerThread = 0;
		Map<String, Long> costMapPerThread = new HashMap<String, Long>();

		for (int i = 1; i <= repeats; i++) {
			String key = "couchbase-check-noc-" + i;
			long startTime = System.nanoTime();
			bucket.get(key, StringDocument.class, opTimeout,
					TimeUnit.MILLISECONDS);
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
