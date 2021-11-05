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
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import scc.cache.Cache;

public class UsersDBLayer {
	private static final String CONNECTION_URL = "https://scc52405.documents.azure.com:443/";
	private static final String DB_KEY = "YZ6iGrfNxSlizVpeX0d7GFG7AEL9Zl9fub1kgaGke1vFIIx9X4MEKyeeKuYzLafJVdNfB9qw2w4pCdFzqpECBA==";
	private static final String DB_NAME = "scc2122dbadrqrd";
	
	private static UsersDBLayer instance;

	public static synchronized UsersDBLayer getInstance() {
		if(instance == null) {
			CosmosClient client = new CosmosClientBuilder()
					.endpoint(CONNECTION_URL)
					.key(DB_KEY)
					.gatewayMode()		// replace by .directMode() for better performance
					.consistencyLevel(ConsistencyLevel.SESSION)
					.connectionSharingAcrossClientsEnabled(true)
					.contentResponseOnWriteEnabled(true)
					.buildClient();
			instance = new UsersDBLayer( client);
		}
		return instance;
	}
	
	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer users;
	
	public UsersDBLayer(CosmosClient client) {
		this.client = client;
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
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.del("user: " + user.getId());
		}
		return users.deleteItem(user, new CosmosItemRequestOptions());
	}
	
	public CosmosItemResponse<UserDAO> putUser(UserDAO user) {
		init();
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.set("user:" + user.getId(), new ObjectMapper().writeValueAsString(user));
		} catch (Exception e) {
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
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.set("user:" + user.getId(), new ObjectMapper().writeValueAsString(user));
		} catch (Exception e) {
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
