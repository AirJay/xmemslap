package com.airjay.memcached.benchmark.xmemcached;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;

import com.airjay.memcached.benchmark.common.Constants;
import com.airjay.memcached.benchmark.common.StringGenerator;

public class Xmemcached {
	public static void main(String[] args) throws Exception {

		new Cli(args).parse();

		String server = Cli.host + ":" + Cli.port;

		MemcachedClientBuilder builder = new XMemcachedClientBuilder(
				AddrUtil.getAddresses(server));
		builder.setConnectionPoolSize(Cli.connCount);

		MemcachedClient memcachedClient = builder.build();
		memcachedClient.setOptimizeGet(false);
		memcachedClient.setMergeFactor(0);
		memcachedClient.setOptimizeMergeBuffer(false);

		NumberFormat numberFormat = NumberFormat.getInstance();

		numberFormat.setMaximumFractionDigits(2);

		if (Cli.operation.equals("set")) {
			System.out.println("Xmemcached setkey startup");

			long startTime = System.nanoTime();
			Map<String, Long> costMap = setKey(memcachedClient, Cli.bytes,
					Cli.threadCount, Cli.repeatCount, Cli.opTimeout);
			long estimatedTime = System.nanoTime() - startTime;

			float repeat = Cli.repeatCount;

			System.out.println("Xmemcached setkey finish, cost time = "
					+ estimatedTime + "ns, " + "set count = " + repeat
					+ ", ops = " + (repeat / estimatedTime) * Constants.seed);

			System.out.println("avg set cost time = "
					+ costMap.get("avgSetCost") + "ns");
			System.out.println("max set cost time = "
					+ costMap.get("maxSetCost") + "ns");
			System.out.println("min set cost time = "
					+ costMap.get("minSetCost") + "ns");

		} else if (Cli.operation.equals("get")) {
			System.out.println("Xmemcached getkey startup");

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

			System.out.println("Xmemcached getkey finish, cost time = "
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

		memcachedClient.shutdown();
	}

	public static Map<String, Long> setKey(MemcachedClient memcachedClient,
			int length, int threads, int repeats, int opTimeout)
			throws TimeoutException, InterruptedException, MemcachedException {

		long avgSetCost = 0;
		long maxSetCost = Long.MIN_VALUE;
		long minSetCost = Long.MAX_VALUE;
		long sumSetCost = 0;
		Map<String, Long> costMap = new HashMap<String, Long>();

		for (int i = 1; i <= repeats; i++) {
			String key = "couchbase-check-noc-" + i;
			String value = StringGenerator.generateValue(i, length);

			long startTime = System.nanoTime();
			memcachedClient.set(key, Constants.EXPIRE_TIME, value, opTimeout);
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
