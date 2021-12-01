package scc.resources;

import scc.data.channel.ChannelsDBLayer;
import scc.data.message.Message;
import scc.data.message.MessageDAO;
import scc.data.message.MessagesDBLayer;
import scc.data.user.UsersDBLayer;

import java.util.UUID;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

/**
 * Resource for managing messages.
 */
@Path("/message")
public class MessageResource {

	private MessagesDBLayer messagesDBLayer;
	private UsersDBLayer usersDBLayer;

	public MessageResource() {}

	@PUT
	@Path("/start")
	public void start() {
		this.messagesDBLayer = new MessagesDBLayer();
		this.usersDBLayer = new UsersDBLayer();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(Message message) {
		String messageId = UUID.randomUUID().toString().replace("-", "");;
		message.setId(messageId);

		messagesDBLayer.putMsg(new MessageDAO(message));

		return messageId;
	}

	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		MessageDAO messageDAO = messagesDBLayer.getMsgById(id);
		usersDBLayer.checkCookieUser(session, messageDAO.getUser());
		messagesDBLayer.delMsgById(id);
	}

}
