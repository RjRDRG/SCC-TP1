package scc.resources;

import scc.data.media.MediaBlobLayer;
import scc.data.user.UsersDBLayer;
import scc.utils.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource
{
	@Context ServletContext context;

	public MediaResource() {}


	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(byte[] contents) {
		String id = Hash.of(contents);
		MediaBlobLayer.getInstance(context).upload(id, contents);
		return id;
	}


	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] download(@PathParam("id") String id) {
		return MediaBlobLayer.getInstance(context).download(id);
	}

	@DELETE
	@Path("/{id}")
	public void delete(@PathParam("id") String id) {
		MediaBlobLayer.getInstance(context).delete(id);
	}
}
