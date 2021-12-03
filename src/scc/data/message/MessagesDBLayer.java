package scc.data.message;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import scc.cache.Cache;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessagesDBLayer {
	private static final String DB_NAME = "scc2122db";
	private static final String RECENT_MSGS = "mostRecentMsgs:";
	private static final int MAX_MSG_IN_CACHE = 20;

	private final CosmosContainer messages;
	private final JedisPool cache;

	public MessagesDBLayer() {
		CosmosClient client = new CosmosClientBuilder()
				.endpoint(System.getenv("COSMOSDB_URL"))
				.key(System.getenv("COSMOSDB_KEY"))
				.gatewayMode()		// replace by .directMode() for better performance
				.consistencyLevel(ConsistencyLevel.SESSION)
				.multipleWriteRegionsEnabled(true)
				.connectionSharingAcrossClientsEnabled(true)
				.contentResponseOnWriteEnabled(true)
				.buildClient();
		this.cache = Cache.getInstance();
		CosmosDatabase db = client.getDatabase(DB_NAME);
		this.messages = db.getContainer("Messages");
	}

	public void delMsgById(String id) {
		MessageDAO msg = getMsgById(id);
		if(msg == null)
			throw new NotFoundException();

		PartitionKey key = new PartitionKey(msg.getChannel());
		int status = messages.deleteItem(msg.getId(), key, new CosmosItemRequestOptions()).getStatusCode();
		if(status >= 400) throw new WebApplicationException(status);
	}
	
	public void putMsg(MessageDAO msg) {
		int status = messages.createItem(msg).getStatusCode();
		if(status >= 400) throw new WebApplicationException(status);
	}
	
	public MessageDAO getMsgById(String id) {
		return messages.queryItems("SELECT * FROM Messages WHERE Messages.id=\"" + id + "\"", new CosmosQueryRequestOptions(), MessageDAO.class).stream().findFirst()
				.orElseThrow(NotFoundException::new);
	}

	public List<MessageDAO> getMessages(String channel, int off, int limit) {
		List<MessageDAO> messageDAOS = new ArrayList<>(limit);
		ObjectMapper m = new ObjectMapper();

		int cachedMessages = 0;

		if(cache!=null) {
			try(Jedis jedis = cache.getResource()) {
				if (off < MAX_MSG_IN_CACHE) {
					List<MessageDAO> cacheMsgs = jedis.lrange(RECENT_MSGS + channel, off, Math.min(limit - 1, MAX_MSG_IN_CACHE - 1)).stream().map(
							s -> {
								try {
									return m.readValue(s, MessageDAO.class);
								} catch (JsonProcessingException e) {
									e.printStackTrace();
									throw new RuntimeException(e);
								}
							}
					).collect(Collectors.toList());
					cachedMessages = cacheMsgs.size();
					messageDAOS.addAll(cacheMsgs);
				}
			}
		}

		if(limit > cachedMessages) {
			messageDAOS.addAll(
					messages.queryItems(
							"SELECT * FROM Messages WHERE Messages.channel=\"" + channel + "\" ORDER BY Messages._ts DESC OFFSET " + (off + cachedMessages) + " LIMIT " + (limit - cachedMessages),
							new CosmosQueryRequestOptions(), MessageDAO.class
					)
					.stream().collect(Collectors.toList())
			);
		}

		return messageDAOS;
	}

	public void updateMessage(MessageDAO msg) {
		int status = messages.replaceItem(msg, msg.getId(), new PartitionKey(msg.getChannel()), new CosmosItemRequestOptions()).getStatusCode();
		if(status >= 400) throw new WebApplicationException(status);
	}

}
