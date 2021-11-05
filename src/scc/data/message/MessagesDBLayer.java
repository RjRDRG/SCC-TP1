package scc.data.message;

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


public class MessagesDBLayer {
	private static final String CONNECTION_URL = "https://scc52405.documents.azure.com:443/";
	private static final String DB_KEY = "YZ6iGrfNxSlizVpeX0d7GFG7AEL9Zl9fub1kgaGke1vFIIx9X4MEKyeeKuYzLafJVdNfB9qw2w4pCdFzqpECBA==";
	private static final String DB_NAME = "scc2122dbadrqrd";
	
	
	private static MessagesDBLayer instance;

	public static synchronized MessagesDBLayer getInstance() {
		if( instance != null)
			return instance;

		CosmosClient client = new CosmosClientBuilder()
		         .endpoint(CONNECTION_URL)
		         .key(DB_KEY)
		         .gatewayMode()		// replace by .directMode() for better performance
		         .consistencyLevel(ConsistencyLevel.SESSION)
		         .connectionSharingAcrossClientsEnabled(true)
		         .contentResponseOnWriteEnabled(true)
		         .buildClient();
		instance = new MessagesDBLayer( client);
		return instance;
		
	}
	
	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer messages;
	
	public MessagesDBLayer(CosmosClient client) {
		this.client = client;
	}
	
	private synchronized void init() {
		if( db != null)
			return;
		db = client.getDatabase(DB_NAME);
		messages = db.getContainer("Messages");
		
	}

	public CosmosItemResponse<Object> delMsgById(String id) {
		init();
		PartitionKey key = new PartitionKey( id);
		return messages.deleteItem(id, key, new CosmosItemRequestOptions());
	}
	
	public CosmosItemResponse<Object> delMsg (MessageDAO msg) {
		init();
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.del("message: " + msg.getId());
		}
		return messages.deleteItem(msg, new CosmosItemRequestOptions());
	}
	
	public CosmosItemResponse<MessageDAO> putMsg(MessageDAO msg) {
		init();
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.set("message:" + msg.getId(), new ObjectMapper().writeValueAsString(msg));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return messages.createItem(msg);
	}
	
	public CosmosPagedIterable<MessageDAO> getMsgById( String id) {
		init();
		return messages.queryItems("SELECT * FROM Messages WHERE Messages.id=\"" + id + "\"", new CosmosQueryRequestOptions(), MessageDAO.class);
	}

	public CosmosPagedIterable<MessageDAO> getMessages(int off, int limit) {
		init();
		return messages.queryItems("SELECT * FROM Messages OFFSET "+ off + "LIMIT " + limit, new CosmosQueryRequestOptions(), MessageDAO.class);
	}
	
	public CosmosPagedIterable<MessageDAO> getAllMessages() {
		init();
		return messages.queryItems("SELECT * FROM Messages", new CosmosQueryRequestOptions(), MessageDAO.class);
	}
	
	public CosmosItemResponse<MessageDAO> updatemsg(MessageDAO msg) {
		init();
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.set("message:" + msg.getId(), new ObjectMapper().writeValueAsString(msg));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return messages.replaceItem(msg, msg.get_rid(), new PartitionKey(msg.getId()), new CosmosItemRequestOptions());
		}

	public void close() {
		client.close();
	}
	

}
