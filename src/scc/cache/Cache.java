package scc.cache;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.mgt.AzureProperties;

import javax.servlet.ServletContext;
import java.util.Optional;

public class Cache {
	
	private static JedisPool instance;
	
	public synchronized static JedisPool getInstance(ServletContext context) {
		if(instance == null) {
			if(!Boolean.parseBoolean(Optional.ofNullable(AzureProperties.getProperty(context, "ENABLE_CACHE")).orElse("true")))
				return null;

			final JedisPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setMaxTotal(128);
			poolConfig.setMaxIdle(128);
			poolConfig.setMinIdle(16);
			poolConfig.setTestOnBorrow(true);
			poolConfig.setTestOnReturn(true);
			poolConfig.setTestWhileIdle(true);
			poolConfig.setNumTestsPerEvictionRun(3);
			poolConfig.setBlockWhenExhausted(true);
			instance = new JedisPool(
					poolConfig,
					AzureProperties.getProperty(context, "REDIS_URL"),
					6380,
					1000,
					AzureProperties.getProperty(context, "REDIS_KEY"),
					true
			);
		}

		return instance;
	}

}
