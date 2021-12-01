package scc.resources;

import scc.data.channel.Channel;
import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
import scc.data.message.Message;
import scc.data.message.MessageDAO;
import scc.data.message.MessagesDBLayer;
import scc.data.user.UsersDBLayer;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resource for managing channels.
 */
@Path("/channel")
public class ChannelResource {

	private static boolean started = false;
	private static UsersDBLayer usersDBLayer;
	private static ChannelsDBLayer channelsDBLayer;
	private static MessagesDBLayer messagesDBLayer;

	public ChannelResource() {}

	public void start() {
		if(!started) {
			usersDBLayer = new UsersDBLayer();
			channelsDBLayer = new ChannelsDBLayer();
			messagesDBLayer = new MessagesDBLayer();
			started = true;
		}
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Channel create(@CookieParam("scc:session") Cookie session, Channel channel) {
		start();

		usersDBLayer.checkCookieUser(session, channel.getOwner());

		String channelId = UUID.randomUUID().toString().replace("-", "");;
		channel.setId(channelId);

		channelsDBLayer.createChannel(new ChannelDAO(channel));

		return channel;
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Channel get(@PathParam("id") String id) {
		start();

		return channelsDBLayer.getChannelById(id).toChannel();
	}

	@GET
	@Path("/{channel}/messages")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Message> get(@CookieParam("scc:session") Cookie session, @PathParam("channel") String channel, @QueryParam("st") int off, @QueryParam("len") int limit) {
		start();

		ChannelDAO channelDAO = channelsDBLayer.getChannelById(channel);
		usersDBLayer.checkCookieUser(session,channelDAO.getMembers());
		return messagesDBLayer.getMessages(channel,off,limit).stream().map(MessageDAO::toMessage).collect(Collectors.toList());
	}

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(@CookieParam("scc:session") Cookie session, Channel channel) {
		start();

		usersDBLayer.checkCookieUser(session, channel.getOwner());

		channelsDBLayer.updateChannel(new ChannelDAO(channel));
	}

	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		start();

		ChannelDAO channel = channelsDBLayer.getChannelById(id);
		usersDBLayer.checkCookieUser(session, channel.getOwner());
		channelsDBLayer.discardChannelById(id);
	}


	@DELETE
	@Path("/force/{id}")
	public void forceDelete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		start();

		ChannelDAO channel = channelsDBLayer.getChannelById(id);
		usersDBLayer.checkCookieUser(session, channel.getOwner());
		channelsDBLayer.delChannelById(id);
	}

}
