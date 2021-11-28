package scc.data.channel;

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
import java.util.List;
import java.util.stream.Collectors;

public class ChannelsDBLayer {
	private static final String DB_NAME = "scc2122db";

	private static ChannelsDBLayer instance;

	public static synchronized ChannelsDBLayer getInstance() {
		if (instance == null) {
			CosmosClient client = new CosmosClientBuilder()
					.endpoint(AzureProperties.getProperty("COSMOSDB_URL"))
					.key(AzureProperties.getProperty("COSMOSDB_KEY"))
					.gatewayMode() // replace by .directMode() for better performance
					.consistencyLevel(ConsistencyLevel.SESSION).connectionSharingAcrossClientsEnabled(true)
					.contentResponseOnWriteEnabled(true).buildClient();
			JedisPool cache = Cache.getInstance();
			instance = new ChannelsDBLayer(client,cache);
		}

		return instance;
	}

	private final CosmosClient client;
	private final JedisPool cache;

	private CosmosContainer channels;

	public ChannelsDBLayer(CosmosClient client, JedisPool cache) {
		this.client = client;
		this.cache = cache;
	}

	private synchronized void init() {
		if (channels != null) return;
		CosmosDatabase db = client.getDatabase(DB_NAME);
		channels = db.getContainer("Channels");
	}

	public boolean delChannelById(String id) {
		init();
		PartitionKey key = new PartitionKey(id);
		return channels.deleteItem(id, key, new CosmosItemRequestOptions()).getStatusCode() < 400;
	}

	public boolean putChannel(ChannelDAO channel) {
		init();
		try {
			cache.getResource().set("channel:" + channel.getIdChannel(), new ObjectMapper().writeValueAsString(channel));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return channels.createItem(channel).getStatusCode() < 400;
	}

	public ChannelDAO getChannelById(String id) {
		init();
		return channels.queryItems("SELECT * FROM Channels WHERE Channels.idChannel=\"" + id + "\"",
				new CosmosQueryRequestOptions(), ChannelDAO.class).stream().findFirst().orElse(null);
	}

	public List<ChannelDAO> getChannels(int offset, int limit) {
		init();
		return channels.queryItems("SELECT * FROM Channels OFFSET " + offset + " LIMIT " + limit,
				new CosmosQueryRequestOptions(), ChannelDAO.class).stream().collect(Collectors.toList());
	}

	public ChannelDAO updateChannel(ChannelDAO channel) {
		init();
		try {
			cache.getResource().set("channel:" + channel.getIdChannel(), new ObjectMapper().writeValueAsString(channel));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return channels.replaceItem(channel, channel.getIdChannel(),new PartitionKey(channel.getIdChannel()), new CosmosItemRequestOptions()).getItem();
	}

	public void close() {
		client.close();
	}

}
