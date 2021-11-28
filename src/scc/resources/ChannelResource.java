package scc.resources;

import scc.data.channel.Channel;
import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
import scc.data.user.UsersDBLayer;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * Resource for managing channels.
 */
@Path("/channel")
public class ChannelResource {

	@Context
	ServletContext context;

	public ChannelResource() {}

	/**
	 * Create a new channel.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(@CookieParam("scc:session") Cookie session, Channel channel) {
		UsersDBLayer.getInstance(context).checkCookieUser(session, channel.getOwner());

		String channelId = UUID.randomUUID().toString();
		channel.setIdChannel(channelId);

		if (ChannelsDBLayer.getInstance(context).putChannel(new ChannelDAO(channel)).getStatusCode() >= 400)
			throw new BadRequestException();

		return channelId;
	}

	/**
	 * Update a new channel.
	 */
	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(@CookieParam("scc:session") Cookie session, Channel channel) {
		UsersDBLayer.getInstance(context).checkCookieUser(session, channel.getOwner());

		if (ChannelsDBLayer.getInstance(context).updateChannel(new ChannelDAO(channel)).getStatusCode() >= 400)
			throw new BadRequestException();
	}

	/**
	 * Delete a channel.
	 */
	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		UsersDBLayer.getInstance(context).checkCookieUser(session, channel.getOwner());

		if (ChannelsDBLayer.getInstance(context).delChannelById(id).getStatusCode() >= 400)
			throw new BadRequestException();
	}

}
