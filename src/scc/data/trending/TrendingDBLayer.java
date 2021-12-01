package scc.data.trending;

import java.util.List;
import java.util.stream.Collectors;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;

public class TrendingDBLayer {
    private static final String DB_NAME = "scc2122db";

    private final CosmosContainer trending;

    public TrendingDBLayer() {
        CosmosClient client = new CosmosClientBuilder()
                .endpoint(System.getenv("COSMOSDB_URL"))
                .key(System.getenv("COSMOSDB_KEY"))
                .gatewayMode() // replace by .directMode() for better performance
                .consistencyLevel(ConsistencyLevel.SESSION)
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        CosmosDatabase db = client.getDatabase(DB_NAME);
        this.trending = db.getContainer("TrendingChannels");
    }

    public List<TrendingChannelDAO> getTrending() {
        return trending.queryItems("SELECT * FROM TrendingChannels", new CosmosQueryRequestOptions(), TrendingChannelDAO.class)
                .stream().collect(Collectors.toList());
    }

}