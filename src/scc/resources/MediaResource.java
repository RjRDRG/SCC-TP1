package scc.resources;

import scc.data.channel.ChannelsDBLayer;
import scc.data.media.MediaVolumeLayer;
import scc.data.message.MessagesDBLayer;
import scc.data.user.UsersDBLayer;
import scc.utils.Hash;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource
{
	private static boolean started = false;
	private static MediaVolumeLayer mediaVolumeLayer;

	public MediaResource() {}

	public void start() {
		try {
			if (!started) {
				mediaVolumeLayer = new MediaVolumeLayer();
				started = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebApplicationException(e.getMessage(), 500);
		}
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(byte[] contents) {
		start();

		String id = Hash.of(contents);
		mediaVolumeLayer.upload(id, contents);
		return id;
	}


	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] download(@PathParam("id") String id) {
		start();

		return mediaVolumeLayer.download(id);
	}

	@DELETE
	@Path("/{id}")
	public void delete(@PathParam("id") String id) {
		start();

		mediaVolumeLayer.delete(id);
	}
}
