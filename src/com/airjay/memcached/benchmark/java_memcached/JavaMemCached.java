package com.airjay.memcached.benchmark.java_memcached;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.BasicConfigurator;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;
import com.airjay.memcached.benchmark.common.Constants;
import com.airjay.memcached.benchmark.common.StringGenerator;

public class JavaMemCached {
	public static void main(String[] args) throws Exception {

		new Cli(args).parse();

		String server = Cli.host + ":" + Cli.port;

		BasicConfigurator.configure();
		SockIOPool pool = SockIOPool.getInstance();
		pool.setMinConn(1);
		pool.setMaxConn(Cli.connCount);
		pool.setMaxIdle(60 * 60 * 1000);
		pool.setServers(server.split(" "));
		pool.initialize();

		MemCachedClient memcachedClient = new MemCachedClient();

		NumberFormat numberFormat = NumberFormat.getInstance();

		numberFormat.setMaximumFractionDigits(2);

		if (Cli.operation.equals("set")) {
			System.out.println("Java-MemCached setkey startup");

			long startTime = System.nanoTime();
			Map<String, Long> costMap = setKey(memcachedClient, Cli.bytes,
					Cli.threadCount, Cli.repeatCount, Cli.opTimeout);
			long estimatedTime = System.nanoTime() - startTime;

			float repeat = Cli.repeatCount;

			System.out.println("Java-MemCached setkey finish, cost time = "
					+ estimatedTime + "ns, " + "set count = " + repeat
					+ ", ops = " + (repeat / estimatedTime) * Constants.seed);

			System.out.println("avg set cost time = "
					+ costMap.get("avgSetCost") + "ns");
			System.out.println("max set cost time = "
					+ costMap.get("maxSetCost") + "ns");
			System.out.println("min set cost time = "
					+ costMap.get("minSetCost") + "ns");

		} else if (Cli.operation.equals("get")) {
			System.out.println("Java-MemCached getkey startup");

			CyclicBarrier barrier = new CyclicBarrier(Cli.threadCount + 1);
			ArrayList<Thread> threadList = new ArrayList<Thread>();

			for (int i = 0; i < Cli.threadCount; i++) {
				threadList.add(new ReadThread(memcachedClient, barrier));
			}

			for (int j = 0; j < threadList.size(); j++) {
				threadList.get(j).start();
			}

			barrier.await();
			long startTime = System.nanoTime();
			barrier.await();
			long estimatedTime = System.nanoTime() - startTime;

			float totalRepeat = Cli.repeatCount * Cli.threadCount;

			System.out.println("Java-MemCached getkey finish, cost time = "
					+ estimatedTime + "ns, " + "get count = " + totalRepeat
					+ ", ops = " + (totalRepeat / estimatedTime)
					* Constants.seed);

			long avgGetCost = 0;
			long maxGetCost = Long.MIN_VALUE;
			long minGetCost = Long.MAX_VALUE;
			long sumGetCost = 0;
			Map<String, Long> costMap = new HashMap<String, Long>();

			for (int m = 0; m < threadList.size(); m++) {
				costMap = ((ReadThread) threadList.get(m))
						.getCostMapPerThread();
				sumGetCost = sumGetCost + costMap.get("avgGetCostPerThread");
				avgGetCost = sumGetCost / (m + 1);
				maxGetCost = maxGetCost > costMap.get("maxGetCostPerThread") ? maxGetCost
						: costMap.get("maxGetCostPerThread");
				minGetCost = minGetCost < costMap.get("minGetCostPerThread") ? minGetCost
						: costMap.get("minGetCostPerThread");
			}

			System.out.println("avg get cost time = " + avgGetCost + "ns");
			System.out.println("max get cost time = " + maxGetCost + "ns");
			System.out.println("min get cost time = " + minGetCost + "ns");

		}

		pool.shutDown();
	}

	public static Map<String, Long> setKey(MemCachedClient memcachedClient,
			int length, int threads, int repeats, int opTimeout) {

		long avgSetCost = 0;
		long maxSetCost = Long.MIN_VALUE;
		long minSetCost = Long.MAX_VALUE;
		long sumSetCost = 0;
		Map<String, Long> costMap = new HashMap<String, Long>();

		for (int i = 1; i <= repeats; i++) {
			String key = "couchbase-check-noc-" + i;
			String value = StringGenerator.generateValue(i, length);

			long startTime = System.nanoTime();
			memcachedClient.set(key, value);
			long estimatedTime = System.nanoTime() - startTime;

			sumSetCost = sumSetCost + estimatedTime;
			avgSetCost = sumSetCost / i;
			maxSetCost = maxSetCost > estimatedTime ? maxSetCost
					: estimatedTime;
			minSetCost = minSetCost < estimatedTime ? minSetCost
					: estimatedTime;

		}

		costMap.put("avgSetCost", avgSetCost);
		costMap.put("maxSetCost", maxSetCost);
		costMap.put("minSetCost", minSetCost);

		return costMap;

	}
}
