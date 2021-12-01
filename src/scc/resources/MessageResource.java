package scc.resources;


import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
import scc.data.message.Message;
import scc.data.message.MessageDAO;
import scc.data.message.MessagesDBLayer;
import scc.data.user.UsersDBLayer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

	@Context
	ServletContext context;

	public MessageResource() {}


	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(Message message) {
		String messageId = UUID.randomUUID().toString().replace("-", "");;
		message.setId(messageId);

		MessagesDBLayer.getInstance(context).putMsg(new MessageDAO(message));

		return messageId;
	}

	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		MessageDAO messageDAO = MessagesDBLayer.getInstance(context).getMsgById(id);
		UsersDBLayer.getInstance(context).checkCookieUser(session, messageDAO.getUser());
		MessagesDBLayer.getInstance(context).delMsgById(id);
	}

}
