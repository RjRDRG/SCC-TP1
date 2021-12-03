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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import scc.cache.Cache;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

public class ChannelsDBLayer {
	private static final String DB_NAME = "scc2122db";
	public static final String CHANNEL = "channel:";

	private final CosmosContainer channels;
	private final JedisPool cache;

	public ChannelsDBLayer() {
		CosmosClient client = new CosmosClientBuilder()
				.endpoint(System.getenv( "COSMOSDB_URL"))
				.key(System.getenv( "COSMOSDB_KEY"))
				.gatewayMode() // replace by .directMode() for better performance
				.multipleWriteRegionsEnabled(true)
				.consistencyLevel(ConsistencyLevel.SESSION)
				.connectionSharingAcrossClientsEnabled(true)
				.contentResponseOnWriteEnabled(true).buildClient();

		cache = Cache.getInstance();

		CosmosDatabase db = client.getDatabase(DB_NAME);
		channels = db.getContainer("Channels");
	}

	public void delChannelById(String id) {
		if(cache!=null) {
			try(Jedis jedis = cache.getResource()) {
				jedis.del(CHANNEL + id);
			}
		}

		int status = channels.deleteItem(id, new PartitionKey(id), new CosmosItemRequestOptions()).getStatusCode();
		if(status >= 400) throw new WebApplicationException(status);
	}

	public void discardChannelById(String id) {
		ChannelDAO channelDAO = getChannelById(id);
		channelDAO.setGarbage(true);
		updateChannel(channelDAO);
	}

	public void createChannel(ChannelDAO channel) {
		int status = channels.createItem(channel).getStatusCode();
		if(status >= 400) throw new WebApplicationException(status);
	}

	public ChannelDAO getChannelById(String id) {
		if(cache!=null) {
			try(Jedis jedis = cache.getResource()) {
				String res = jedis.get(CHANNEL + id);
				if (res != null) {
					try {
						return new ObjectMapper().readValue(res, ChannelDAO.class);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return channels.queryItems("SELECT * FROM Channels WHERE Channels.id=\"" + id + "\"",
				new CosmosQueryRequestOptions(), ChannelDAO.class).stream().findFirst()
				.orElseThrow(NotFoundException::new);
	}

	public void updateChannel(ChannelDAO channel) {
		int status = channels.replaceItem(channel, channel.getId(), new PartitionKey(channel.getId()), new CosmosItemRequestOptions()).getStatusCode();
		if(status >= 400) throw new WebApplicationException(status);
	}
}
