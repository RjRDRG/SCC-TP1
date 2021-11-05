package scc.data.user;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import scc.cache.Cache;
import scc.mgt.AzureProperties;

import javax.servlet.ServletContext;

public class UsersDBLayer {
	private static final String DB_NAME = "scc2122dbadrqrd";
	
	private static UsersDBLayer instance;

	public static synchronized UsersDBLayer getInstance(ServletContext context) {
		if(instance == null) {
			try {
				CosmosClient client = new CosmosClientBuilder()
						.endpoint(AzureProperties.getProperty(context, "COSMOSDB_URL"))
						.key(AzureProperties.getProperty(context, "COSMOSDB_KEY"))
						.gatewayMode()		// replace by .directMode() for better performance
						.consistencyLevel(ConsistencyLevel.SESSION)
						.connectionSharingAcrossClientsEnabled(true)
						.contentResponseOnWriteEnabled(true)
						.buildClient();
				JedisPool cache = Cache.getInstance(context);
				instance = new UsersDBLayer(client,cache);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return instance;
	}
	
	private final CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer users;
	private final JedisPool cache;
	
	public UsersDBLayer(CosmosClient client, JedisPool cache) {
		this.client = client;
		this.cache = cache;
	}
	
	private synchronized void init() {
		if(db != null) return;
		db = client.getDatabase(DB_NAME);
		users = db.getContainer("Users");
	}

	public CosmosItemResponse<Object> delUserById(String id) {
		init();
		PartitionKey key = new PartitionKey( id);
		return users.deleteItem(id, key, new CosmosItemRequestOptions());
	}
	
	public CosmosItemResponse<Object> delUser(UserDAO user) {
		init();
		cache.getResource().del("user: " + user.getId());
		return users.deleteItem(user, new CosmosItemRequestOptions());
	}
	
	public CosmosItemResponse<UserDAO> putUser(UserDAO user) {
		init();
		try {
			cache.getResource().set("user:" + user.getId(), new ObjectMapper().writeValueAsString(user));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return users.createItem(user);
	}
	
	public CosmosPagedIterable<UserDAO> getUserById( String id) {
		init();
		return users.queryItems("SELECT * FROM Users WHERE Users.id=\"" + id + "\"", new CosmosQueryRequestOptions(), UserDAO.class);
	}
	
	public CosmosItemResponse<UserDAO> updateUser(UserDAO user) {
		init();

		try {
			cache.getResource().set("user:" + user.getId(), new ObjectMapper().writeValueAsString(user));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return users.replaceItem(user, user.get_rid(),new PartitionKey(user.getId()), new CosmosItemRequestOptions());		
	}

	
	public CosmosPagedIterable<UserDAO> getUsers(int off, int limit) {
		init();
		return users.queryItems("SELECT * FROM Users OFFSET "+off+" LIMIT "+limit, new CosmosQueryRequestOptions(), UserDAO.class);
	}

	
	public void close() {
		client.close();
	}
	
	
}
