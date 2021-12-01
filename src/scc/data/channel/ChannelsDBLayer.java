package scc.data.channel;

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
import scc.data.user.UserDAO;
import scc.mgt.AzureProperties;

import javax.servlet.ServletContext;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

public class ChannelsDBLayer {
	private static final String DB_NAME = "scc2122db";
	public static final String CHANNEL = "channel:";

	private static ChannelsDBLayer instance;

	public static synchronized ChannelsDBLayer getInstance(ServletContext context) {
		if (instance == null) {
			CosmosClient client = new CosmosClientBuilder()
					.endpoint(AzureProperties.getProperty(context, "COSMOSDB_URL"))
					.key(AzureProperties.getProperty(context, "COSMOSDB_KEY"))
					.gatewayMode() // replace by .directMode() for better performance
					.consistencyLevel(ConsistencyLevel.SESSION).connectionSharingAcrossClientsEnabled(true)
					.contentResponseOnWriteEnabled(true).buildClient();
			JedisPool cache = Cache.getInstance(context);
			instance = new ChannelsDBLayer(context, client,cache);
		}

		return instance;
	}

	private final ServletContext context;
	private final CosmosClient client;
	private final JedisPool cache;

	private CosmosContainer channels;

	public ChannelsDBLayer(ServletContext context, CosmosClient client, JedisPool cache) {
		this.context = context;
		this.client = client;
		this.cache = cache;
	}

	private synchronized void init() {
		if (channels != null) return;
		CosmosDatabase db = client.getDatabase(DB_NAME);
		channels = db.getContainer("Channels");
	}

	public void delChannelById(String id) {
		init();
		if(cache!=null) {
			cache.getResource().del(CHANNEL + id);
		}

		int status = channels.deleteItem(id, new PartitionKey(id), new CosmosItemRequestOptions()).getStatusCode();
		if(status >= 400) throw new WebApplicationException(status);
	}

	public void discardChannelById(String id) {
		init();
		ChannelDAO channelDAO = getChannelById(id);
		channelDAO.setGarbage(true);
		updateChannel(channelDAO);
	}

	public void createChannel(ChannelDAO channel) {
		init();
		if(cache!=null) {
			try {
				cache.getResource().set(CHANNEL + channel.getId(), new ObjectMapper().writeValueAsString(channel));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}

		int status = channels.createItem(channel).getStatusCode();
		if(status >= 400) throw new WebApplicationException(status);
	}

	public ChannelDAO getChannelById(String id) {
		init();
		if(cache!=null) {
			String res = cache.getResource().get(CHANNEL + id);
			if (res != null) {
				try {
					return new ObjectMapper().readValue(res, ChannelDAO.class);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			}
		}
		return channels.queryItems("SELECT * FROM Channels WHERE Channels.id=\"" + id + "\"",
				new CosmosQueryRequestOptions(), ChannelDAO.class).stream().findFirst()
				.orElseThrow(NotFoundException::new);
	}

	public void updateChannel(ChannelDAO channel) {
		init();
		if(cache!=null) {
			try {
				cache.getResource().set(CHANNEL + channel.getId(), new ObjectMapper().writeValueAsString(channel));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
		if(channels.replaceItem(channel, channel.getId(), new PartitionKey(channel.getId()), new CosmosItemRequestOptions()).getStatusCode() >= 400)
			throw new BadRequestException();
	}

	public void close() {
		client.close();
	}

}
