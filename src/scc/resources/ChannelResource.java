package scc.resources;

import scc.data.channel.Channel;
import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * Resource for managing channels.
 */
public class ChannelResource {

	final ChannelsDBLayer dbLayer;

	public ChannelResource() {
			dbLayer = ChannelsDBLayer.getInstance();
		}

	/**
	 * Create a new channel.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(Channel channel) {
		String channelId = UUID.randomUUID().toString();
		channel.setId(channelId);

		if (dbLayer.putChannel(new ChannelDAO(channel)).getStatusCode() >= 400)
			throw new BadRequestException();

		return channelId;
	}

	/**
	 * Update a new channel.
	 */
	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(Channel channel) {
		if (dbLayer.updateChannel(new ChannelDAO(channel)).getStatusCode() >= 400)
			throw new BadRequestException();
	}

	/**
	 * Delete a channel.
	 */
	@DELETE
	@Path("/{id}")
	public void delete(@PathParam("id") String id) {
		if (dbLayer.delChannelById(id).getStatusCode() >= 400)
			throw new BadRequestException();
	}

}
