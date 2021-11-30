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

import redis.clients.jedis.JedisPool;
import scc.cache.Cache;
import scc.data.media.MediaBlobLayer;
import scc.mgt.AzureProperties;

import javax.servlet.ServletContext;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessagesDBLayer {
	private static final String DB_NAME = "scc2122db";
	private static final String RECENT_MSGS = "MostRecentMsgs";
	private static final int MAX_MSG_IN_CACHE = 20;

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
			instance = new MessagesDBLayer(context, client, cache);
		}

		return instance;
	}

	private final ServletContext context;
	private final CosmosClient client;
	private final JedisPool cache;

	private CosmosContainer messages;

	public MessagesDBLayer(ServletContext context, CosmosClient client, JedisPool cache) {
		this.context = context;
		this.client = client;
		this.cache = cache;
	}
	
	private synchronized void init() {
		if(messages != null) return;
		CosmosDatabase db = client.getDatabase(DB_NAME);
		messages = db.getContainer("Messages");
	}

	public void delMsgById(String id) {
		init();

		MessageDAO msg = getMsgById(id);
		if(msg == null)
			throw new NotFoundException();

		PartitionKey key = new PartitionKey(msg.getChannel());
		if(messages.deleteItem(msg.getId(), key, new CosmosItemRequestOptions()).getStatusCode() >= 400)
			throw new BadRequestException();

		if(cache!=null) {
			List<String> msgs = cache.getResource().lrange(RECENT_MSGS + msg.getChannel(), 0, -1);

			if (!msgs.isEmpty()) {
				cache.getResource().del(RECENT_MSGS + msg.getChannel());

				ObjectMapper mapper = new ObjectMapper();

				for (String s : msgs) {
					MessageDAO m = null;
					try {
						m = mapper.readValue(s, MessageDAO.class);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
					if (!m.getId().equals(id)) {
						cache.getResource().lpush(RECENT_MSGS + msg.getChannel(), s);
					}
				}
			}
		}

		if(msg.getIdPhoto() != null && msg.getIdPhoto().equals(""))
			MediaBlobLayer.getInstance(context).delete(msg.getIdPhoto());
	}
	
	public void putMsg(MessageDAO msg) {
		init();
		if(messages.createItem(msg).getStatusCode() >= 400)
			throw new BadRequestException();
	}
	
	public MessageDAO getMsgById(String id) {
		init();
		return messages.queryItems("SELECT * FROM Messages WHERE Messages.id=\"" + id + "\"", new CosmosQueryRequestOptions(), MessageDAO.class).stream().findFirst()
				.orElseThrow(NotFoundException::new);
	}

	public List<MessageDAO> getMessages(String channel, int off, int limit) {
		init();
		List<MessageDAO> messageDAOS = new ArrayList<>(limit);
		ObjectMapper m = new ObjectMapper();

		int cachedMessages = 0;

		if(cache!=null) {
			if (off < MAX_MSG_IN_CACHE) {
				cachedMessages = Math.min(limit, MAX_MSG_IN_CACHE - off);
				messageDAOS.addAll(cache.getResource().lrange(RECENT_MSGS + channel, off, Math.min(limit - 1, MAX_MSG_IN_CACHE - 1)).stream().map(
						s -> {
							try {
								return m.readValue(s, MessageDAO.class);
							} catch (JsonProcessingException e) {
								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
				).collect(Collectors.toList()));
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

	/*
	public void updateMessage(MessageDAO msg) {
		init();
		if(cache!=null) {
			List<String> lst = cache.getResource().lrange(RECENT_MSGS + msg.getChannel(), 0, -1);
			ObjectMapper mapper = new ObjectMapper();
			for (String s : lst) {
				MessageDAO m = null;
				try {
					m = mapper.readValue(s, MessageDAO.class);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				if (msg.equals(m)) {
					delMsgById(m.getId());
					try {
						cache.getResource().lpush(RECENT_MSGS + msg.getChannel(), mapper.writeValueAsString(msg));
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
		if(messages.replaceItem(msg, msg.getId(), new PartitionKey(msg.getChannel()), new CosmosItemRequestOptions()).getStatusCode() >= 400)
			throw new BadRequestException();
	}*/

	public void deleteChannelsMessages(String channel) {
		init();
		if(cache!=null) {
			cache.getResource().del(RECENT_MSGS + channel);
		}
		messages.queryItems("DELETE FROM Messages WHERE Messages.channel=\"" + channel + "\"", new CosmosQueryRequestOptions(), MessageDAO.class);
	}

	public void close() {
		client.close();
	}
	

}
