package scc.resources;

import scc.data.channel.Channel;
import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
import scc.data.user.User;
import scc.data.user.UserDAO;
import scc.data.user.UsersDBLayer;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.UUID;

/**
 * Resource for managing channels.
 */
@Path("/channel")
public class ChannelResource {

	public ChannelResource() {}

	/**
	 * Create a new channel.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(@CookieParam("scc:session") Cookie session, Channel channel) {
		UsersDBLayer.getInstance().checkCookieUser(session, channel.getOwner());

		String channelId = UUID.randomUUID().toString();
		channel.setIdChannel(channelId);

		ChannelsDBLayer.getInstance().putChannel(new ChannelDAO(channel));

		return channelId;
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Channel get(@PathParam("id") String id) {
		ChannelDAO channel = ChannelsDBLayer.getInstance().getChannelById(id);
		if(channel == null)
			throw new NotFoundException();

		return channel.toChannel();
	}

	/**
	 * Update a new channel.
	 */
	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(@CookieParam("scc:session") Cookie session, Channel channel) {
		UsersDBLayer.getInstance().checkCookieUser(session, channel.getOwner());

		ChannelsDBLayer.getInstance().updateChannel(new ChannelDAO(channel));
	}

	/**
	 * Delete a channel.
	 */
	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		ChannelDAO channel = ChannelsDBLayer.getInstance().getChannelById(id);
		UsersDBLayer.getInstance().checkCookieUser(session, channel.getOwner());

		if (!ChannelsDBLayer.getInstance().delChannelById(id))
			throw new BadRequestException();
	}

}
