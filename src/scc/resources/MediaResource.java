package scc.resources;

import scc.data.media.MediaVolumeLayer;
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
		if(!started) {
			mediaVolumeLayer = new MediaVolumeLayer();
			started = true;
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
