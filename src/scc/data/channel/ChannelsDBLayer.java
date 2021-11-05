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
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import scc.cache.Cache;

public class ChannelsDBLayer {
	private static final String CONNECTION_URL = "https://scc52405.documents.azure.com:443/";
	private static final String DB_KEY = "YZ6iGrfNxSlizVpeX0d7GFG7AEL9Zl9fub1kgaGke1vFIIx9X4MEKyeeKuYzLafJVdNfB9qw2w4pCdFzqpECBA==";
	private static final String DB_NAME = "scc2122dbTp1_49067_52405_45081";

	private static ChannelsDBLayer instance;

	public static synchronized ChannelsDBLayer getInstance() {
		if (instance != null)
			return instance;

		CosmosClient client = new CosmosClientBuilder()
				.endpoint(CONNECTION_URL)
				.key(DB_KEY)
				.gatewayMode() // replace by .directMode() for better performance
				.consistencyLevel(ConsistencyLevel.SESSION).connectionSharingAcrossClientsEnabled(true)
				.contentResponseOnWriteEnabled(true).buildClient();
		instance = new ChannelsDBLayer(client);
		return instance;
	}

	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer channels;

	public ChannelsDBLayer(CosmosClient client) {
		this.client = client;
	}

	private synchronized void init() {
		if (db != null)
			return;
		db = client.getDatabase(DB_NAME);
		channels = db.getContainer("Channels");
	}

	public CosmosItemResponse<Object> delChannelById(String id) {
		init();
		PartitionKey key = new PartitionKey(id);
		return channels.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<Object> delChannel(ChannelDAO channel) {
		init();
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.del("channel: " + channel.getId());
		}
		return channels.deleteItem(channel, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<ChannelDAO> putChannel(ChannelDAO channel) {
		init();
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.set("channel:" + channel.getId(), new ObjectMapper().writeValueAsString(channel));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return channels.createItem(channel);
	}

	public CosmosPagedIterable<ChannelDAO> getChannelById(String id) {
		init();
		return channels.queryItems("SELECT * FROM Channels WHERE Channels.id=\"" + id + "\"",
				new CosmosQueryRequestOptions(), ChannelDAO.class);
	}

	public CosmosPagedIterable<ChannelDAO> getChannels(int offset, int limit) {
		init();
		return channels.queryItems("SELECT * FROM Channels OFFSET " + offset + " LIMIT " + limit,
				new CosmosQueryRequestOptions(), ChannelDAO.class);
	}
	
	public CosmosItemResponse<ChannelDAO> updateChannel(ChannelDAO channel) {
		init();
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.set("channel:" + channel.getId(), new ObjectMapper().writeValueAsString(channel));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return channels.replaceItem(channel, channel.get_rid(),new PartitionKey(channel.getId()), new CosmosItemRequestOptions());
	}

	public void close() {
		client.close();
	}

}
