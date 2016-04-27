package com.airjay.memcached.benchmark.spymemcached;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import com.airjay.memcached.benchmark.common.Constants;
import com.airjay.memcached.benchmark.common.StringGenerator;

public class Spymemcached {
	public static void main(String[] args) throws Exception {

		new Cli(args).parse();

		String server = Cli.host + ":" + Cli.port;

		if (Cli.connCount != Cli.threadCount) {
			System.out
					.println("For spymemcached, connCount need be equal to threadCount.");
			System.exit(0);
		}

		NumberFormat numberFormat = NumberFormat.getInstance();

		numberFormat.setMaximumFractionDigits(2);

		if (Cli.operation.equals("set")) {

			MemcachedClient memcachedClient = new MemcachedClient(
					AddrUtil.getAddresses(server));

			System.out.println("Spymemcached setkey startup");

			long startTime = System.nanoTime();
			Map<String, Long> costMap = setKey(memcachedClient, Cli.bytes,
					Cli.threadCount, Cli.repeatCount, Cli.opTimeout);
			long estimatedTime = System.nanoTime() - startTime;

			float repeat = Cli.repeatCount;

			System.out.println("Spymemcached setkey finish, cost time = "
					+ estimatedTime + "ns, " + "set count = " + repeat
					+ ", ops = " + (repeat / estimatedTime) * Constants.seed);

			System.out.println("avg set cost time = "
					+ costMap.get("avgSetCost") + "ns");
			System.out.println("max set cost time = "
					+ costMap.get("maxSetCost") + "ns");
			System.out.println("min set cost time = "
					+ costMap.get("minSetCost") + "ns");

		} else if (Cli.operation.equals("get")) {
			System.out.println("Spymemcached getkey startup");

			CyclicBarrier barrier = new CyclicBarrier(Cli.threadCount + 1);
			ArrayList<Thread> threadList = new ArrayList<Thread>();

			for (int i = 0; i < Cli.threadCount; i++) {
				threadList.add(new ReadThread(new MemcachedClient(AddrUtil
						.getAddresses(server)), barrier));
			}

			for (int j = 0; j < threadList.size(); j++) {
				threadList.get(j).start();
			}

			barrier.await();
			long startTime = System.nanoTime();
			barrier.await();
			long estimatedTime = System.nanoTime() - startTime;

			float totalRepeat = Cli.repeatCount * Cli.threadCount;

			System.out.println("Spymemcached getkey finish, cost time = "
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

	}

	public static Map<String, Long> setKey(MemcachedClient memcachedClient,
			int length, int threads, int repeats, int opTimeout)
			throws InterruptedException, TimeoutException, ExecutionException {

		long avgSetCost = 0;
		long maxSetCost = Long.MIN_VALUE;
		long minSetCost = Long.MAX_VALUE;
		long sumSetCost = 0;
		Map<String, Long> costMap = new HashMap<String, Long>();

		for (int i = 1; i <= repeats; i++) {
			String key = "couchbase-check-noc-" + i;
			String value = StringGenerator.generateValue(i, length);

			long startTime = System.nanoTime();
			memcachedClient.set(key, Constants.EXPIRE_TIME, value).get(
					opTimeout, TimeUnit.MILLISECONDS);
			long estimatedTime = System.nanoTime() - startTime;

			sumSetCost = sumSetCost + estimatedTime;
			avgSetCost = sumSetCost / i;
			maxSetCost = maxSetCost > estimatedTime ? maxSetCost
					: estimatedTime;
			minSetCost = minSetCost < estimatedTime ? minSetCost
					: estimatedTime;

		}

		memcachedClient.shutdown();

		costMap.put("avgSetCost", avgSetCost);
		costMap.put("maxSetCost", maxSetCost);
		costMap.put("minSetCost", minSetCost);

		return costMap;

	}
}
