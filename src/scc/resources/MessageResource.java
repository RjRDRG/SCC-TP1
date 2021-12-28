package scc.resources;

import scc.data.channel.ChannelsDBLayer;
import scc.data.message.Message;
import scc.data.message.MessageDAO;
import scc.data.message.MessagesDBLayer;
import scc.data.user.UsersDBLayer;

import java.util.UUID;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

/**
 * Resource for managing messages.
 */
@Path("/message")
public class MessageResource {
	private static boolean started = false;
	private static MessagesDBLayer messagesDBLayer;
	private static UsersDBLayer usersDBLayer;

	public MessageResource() {}

	public void start() {
		try {
			if (!started) {
				usersDBLayer = new UsersDBLayer();
				messagesDBLayer = new MessagesDBLayer();
				started = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebApplicationException(e.getMessage(), 500);
		}
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(Message message) {
		start();

		String messageId = UUID.randomUUID().toString().replace("-", "");;
		message.setId(messageId);

		messagesDBLayer.putMsg(new MessageDAO(message));

		return messageId;
	}

	/**
	 * Delete a message.
	 */
	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		start();

		MessageDAO messageDAO = messagesDBLayer.getMsgById(id);
		usersDBLayer.checkCookieUser(session, messageDAO.getUser());
		messagesDBLayer.delMsgById(id);
	}

}
