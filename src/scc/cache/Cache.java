package scc.cache;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Cache {

	private static final String RedisHostname = "scc2122redis.redis.cache.windows.net";
	private static final String RedisKey = "onG5VVs2ucaD07I993cNmlnXCPpkm5dpzdiBPf9xOfE=";
	
	private static JedisPool instance;
	
	public synchronized static JedisPool getCachePool() {
		if( instance != null)
			return instance;
		final JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		instance = new JedisPool(poolConfig, RedisHostname, 6380, 1000, RedisKey, true);
		return instance;
	}

}
