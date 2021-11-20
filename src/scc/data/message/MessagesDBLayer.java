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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.JedisPool;
import scc.cache.Cache;
import scc.mgt.AzureProperties;

import javax.servlet.ServletContext;


public class MessagesDBLayer {
	private static final String DB_NAME = "scc2122dbadrqrd";

	private static MessagesDBLayer instance;

	public static synchronized MessagesDBLayer getInstance(ServletContext context) {
		if( instance == null) {
			CosmosClient client = new CosmosClientBuilder()
					.endpoint(AzureProperties.getProperty(context, "COSMOSDB_URL"))
					.key(AzureProperties.getProperty(context, "COSMOSDB_KEY"))
					.gatewayMode()		// replace by .directMode() for better performance
					.consistencyLevel(ConsistencyLevel.SESSION)
					.connectionSharingAcrossClientsEnabled(true)
					.contentResponseOnWriteEnabled(true)
					.buildClient();
			JedisPool cache = Cache.getInstance(context);
			instance = new MessagesDBLayer(client, cache);
		}

		return instance;
	}
	
	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer messages;
	private final JedisPool cache;

	public MessagesDBLayer(CosmosClient client, JedisPool cache) {
		this.client = client;
		this.cache = cache;
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
		cache.getResource().del("message: " + msg.getIdMessage());
		return messages.deleteItem(msg, new CosmosItemRequestOptions());
	}
	
	public CosmosItemResponse<MessageDAO> putMsg(MessageDAO msg) {
		init();
		try {
			cache.getResource().set("message:" + msg.getIdMessage(), new ObjectMapper().writeValueAsString(msg));
		} catch (JsonProcessingException e) {
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
		try {
			cache.getResource().set("message:" + msg.getIdMessage(), new ObjectMapper().writeValueAsString(msg));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return messages.replaceItem(msg, msg.get_rid(), new PartitionKey(msg.getIdMessage()), new CosmosItemRequestOptions());
		}

	public void close() {
		client.close();
	}
	

}
