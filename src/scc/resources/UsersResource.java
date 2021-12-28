package scc.resources;

import scc.data.authentication.Credentials;
import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
import scc.data.message.MessageDAO;
import scc.data.message.MessagesDBLayer;
import scc.data.user.User;
import scc.data.user.UserDAO;
import scc.data.user.UsersDBLayer;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource
{
	private static boolean started = false;
	private static ChannelsDBLayer channelsDBLayer;
	private static UsersDBLayer usersDBLayer;
	private static MessagesDBLayer messagesDBLayer;

	public UsersResource() {}

	public void start() {
		try {
			if (!started) {
				channelsDBLayer = new ChannelsDBLayer();
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
	public Credentials create(User user) {
		start();

		String userId = UUID.randomUUID().toString().replace("-", "");
		user.setId(userId);

		usersDBLayer.createUser(new UserDAO(user));

		return new Credentials(userId,user.getPwd());
	}
	

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(@CookieParam("scc:session") Cookie session, User user) {
		start();

		usersDBLayer.checkCookieUser(session,user.getId());
		usersDBLayer.updateUser(new UserDAO(user));
	}

	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		start();

		usersDBLayer.checkCookieUser(session,id);
		for(MessageDAO msg : messagesDBLayer.getMsgsSentByUser(id)) {
			msg.setUser("NA");
			messagesDBLayer.updateMessage(msg);
		}
		usersDBLayer.delUserById(id);
	}


	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public User get(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		start();

		usersDBLayer.checkCookieUser(session,id);
		return usersDBLayer.getUserById(id).toUser();
	}

	@GET
	@Path("/{id}/channels")
	@Produces(MediaType.APPLICATION_JSON)
	public String[] getChannels(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		start();

		usersDBLayer.checkCookieUser(session,id);
		return usersDBLayer.getUserById(id).toUser().getChannelIds();
	}


	@POST
	@Path("/subscribe/{id}/{channel}")
	public void subscribe(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @PathParam("channel") String channel) {
		start();

		usersDBLayer.checkCookieUser(session, id);

		ChannelDAO channelDAO = channelsDBLayer.getChannelById(channel);
		UserDAO userDAO = usersDBLayer.getUserById(id);

		if(channelDAO.isPublicChannel()) {
			Set<String> channels = Arrays.stream(userDAO.getChannelIds()).collect(Collectors.toSet());
			channels.add(channel);
			userDAO.setChannelIds(channels.toArray(new String[0]));
			usersDBLayer.updateUser(userDAO);

			Set<String> members = Arrays.stream(channelDAO.getMembers()).collect(Collectors.toSet());
			members.add(userDAO.getId());
			channelDAO.setMembers(members.toArray(new String[0]));
			channelsDBLayer.updateChannel(channelDAO);
		}
	}


	@POST
	@Path("/invite/{id}/{channel}/{other}")
	public void invite(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @PathParam("channel") String channel, @PathParam("other") String other) {
		start();

		usersDBLayer.checkCookieUser(session, id);

		ChannelDAO channelDAO = channelsDBLayer.getChannelById(channel);
		UserDAO userDAO = usersDBLayer.getUserById(other);

		if(Arrays.asList(channelDAO.getMembers()).contains(id)) {
			Set<String> channels = Arrays.stream(userDAO.getChannelIds()).collect(Collectors.toSet());
			channels.add(channel);
			userDAO.setChannelIds(channels.toArray(new String[0]));
			usersDBLayer.updateUser(userDAO);

			Set<String> members = Arrays.stream(channelDAO.getMembers()).collect(Collectors.toSet());
			members.add(other);
			channelDAO.setMembers(members.toArray(new String[0]));
			channelsDBLayer.updateChannel(channelDAO);
		}
	}
}
