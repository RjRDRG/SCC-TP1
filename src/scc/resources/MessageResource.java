package scc.resources;


import scc.data.message.Message;
import scc.data.message.MessageDAO;
import scc.data.message.MessagesDBLayer;

import java.util.UUID;
import javax.servlet.ServletContext;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Resource for managing messages.
 */

public class MessageResource {

	public MessageResource() {}

	/**
	 * Create a new message.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(Message message) {
		String messageId = UUID.randomUUID().toString();
		message.setIdMessage(messageId);

		MessagesDBLayer.getInstance().putMsg(new MessageDAO(message));

		return messageId;
	}

	/**
	 * Update a new message.
	 */
	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(Message message) {
		MessagesDBLayer.getInstance().updateMessage(new MessageDAO (message));
	}

	/**
	 * Delete a message.
	 */
	@DELETE
	@Path("/{id}")
	public void delete(@PathParam("id") String id) {
		MessagesDBLayer.getInstance().delMsgById(id);
	}

}
