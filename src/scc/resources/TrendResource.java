package scc.resources;

import scc.data.channel.Channel;
import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
import scc.data.message.Message;
import scc.data.message.MessageDAO;
import scc.data.message.MessagesDBLayer;
import scc.data.trending.TrendingChannel;
import scc.data.trending.TrendingChannelDAO;
import scc.data.trending.TrendingDBLayer;
import scc.data.user.UsersDBLayer;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resource for managing channels.
 */
@Path("/trend")
public class TrendResource {

	private static boolean started = false;
	private static TrendingDBLayer trendingDBLayer;

	public TrendResource() {}

	public void start() {
		if(!started) {
			trendingDBLayer = new TrendingDBLayer();
			started = true;
		}
	}

	@GET
	@Path("/channel")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TrendingChannel> get() {
		start();

		return trendingDBLayer.getTrending().stream().map(TrendingChannelDAO::toTrendingChannel).collect(Collectors.toList());
	}

}
