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

public class MessageResource {

	public MessageResource() {}


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


	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(@CookieParam("scc:session") Cookie session, Message message) {
		MessageDAO messageDAO = MessagesDBLayer.getInstance().getMsgById(message.getIdMessage());
		UsersDBLayer.getInstance().checkCookieUser(session, messageDAO.getSend());
		MessagesDBLayer.getInstance().updateMessage(new MessageDAO (message));
	}


	@GET
	@Path("/{channel}/{off}/{limit}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Message> get(@CookieParam("scc:session") Cookie session, @PathParam("channel") String channel, @PathParam("off") String off, @PathParam("limit") String limit) {
		ChannelDAO channelDAO = ChannelsDBLayer.getInstance().getChannelById(channel);
		UsersDBLayer.getInstance().checkCookieUser(session,channelDAO.getMembers());
		return MessagesDBLayer.getInstance().getMessages(channel,Integer.parseInt(off), Integer.parseInt(limit)).stream().map(MessageDAO::toMessage).collect(Collectors.toList());
	}


	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		MessageDAO messageDAO = MessagesDBLayer.getInstance().getMsgById(id);
		UsersDBLayer.getInstance().checkCookieUser(session, messageDAO.getSend());
		MessagesDBLayer.getInstance().delMsgById(id);
	}

}
