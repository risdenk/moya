package org.moya.core.memcached;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.moya.core.yarn.MConstants;

import zookeeper.groups.JoinGroup;

import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.CacheStorage;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;

public class StartMemcached {

	private static final Log LOG = LogFactory.getLog(StartMemcached.class);

	public StartMemcached() {

	}

	public static boolean start() throws MalformedURLException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} //slow start waiting 10 seconds. 
		
		LOG.debug("Getting ENV Settings and ZK Servers");

		Map<String, String> envs = System.getenv();
		String ZKHosts = "";
		
		 if (envs.containsKey(MConstants.ZOOKEEPERHOSTS)) {
		        ZKHosts = envs.get(MConstants.ZOOKEEPERHOSTS);
		    }
		 
		 System.out.println("ZKH = "+ ZKHosts);
		
		
		LOG.debug("Setting up classes");
		

		LOG.info("Attempting to start jmemcached server deamon");
		final MemCacheDaemon<LocalCacheElement> daemon = new MemCacheDaemon<LocalCacheElement>();

		CacheStorage<Key, LocalCacheElement> storage;
		InetSocketAddress c = new InetSocketAddress(8555);
		storage = ConcurrentLinkedHashMap.create(                                 // FIRST IN FIRST OUT - 1mill KV - little under 512-64mb. 
				ConcurrentLinkedHashMap.EvictionPolicy.FIFO, 1000000, 469762048); //Eviction Style, max KV to hold, max size to hold in bytes
		daemon.setCache(new CacheImpl(storage));
		daemon.setBinary(false);
		daemon.setAddr(c);
		daemon.setIdleTime(120);
		daemon.setVerbose(true);
		daemon.start();
		LOG.info("jmemcached server deamon started");

		// StartJettyTest.main(new String[] {});

		try {
			// Add self in zookeer /moya/ group
			//Hostname string is comma seperated list of host:port of ZK servers
			JoinGroup.main(new String[] {
					ZKHosts,
					"moya",
					InetAddress.getLocalHost().getHostName() + ":"
							+ c.getPort() });
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;// junk
		}

		return true; // got to this point say true
	}

	public static void main(String[] args) throws MalformedURLException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {

		start();
		LOG.info("Exiting StartMemcached");
	}

}
