package scc.resources;

import scc.data.authentication.Credentials;
import scc.data.authentication.Login;
import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
import scc.data.message.MessagesDBLayer;
import scc.data.user.User;
import scc.data.user.UserDAO;
import scc.data.user.UsersDBLayer;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
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
	private ChannelsDBLayer channelsDBLayer;
	private UsersDBLayer usersDBLayer;

	public UsersResource() {}

	@PUT
	@Path("/start")
	public void start() {
		this.channelsDBLayer = new ChannelsDBLayer();
		this.usersDBLayer = new UsersDBLayer();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Credentials create(User user) {
		String userId = UUID.randomUUID().toString().replace("-", "");
		user.setId(userId);

		usersDBLayer.createUser(new UserDAO(user));

		return new Credentials(userId,user.getPwd());
	}
	

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(@CookieParam("scc:session") Cookie session, User user) {
		usersDBLayer.checkCookieUser(session,user.getId());
		usersDBLayer.updateUser(new UserDAO(user));
	}


	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		usersDBLayer.checkCookieUser(session,id);
		usersDBLayer.discardUserById(id);
	}

	@DELETE
	@Path("/force/{id}")
	public void forceDelete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		usersDBLayer.checkCookieUser(session,id);
		usersDBLayer.delUserById(id);
	}


	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public User get(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		usersDBLayer.checkCookieUser(session,id);
		return usersDBLayer.getUserById(id).toUser();
	}

	@GET
	@Path("/{id}/channels")
	@Produces(MediaType.APPLICATION_JSON)
	public String[] getChannels(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		usersDBLayer.checkCookieUser(session,id);
		return usersDBLayer.getUserById(id).toUser().getChannelIds();
	}


	@POST
	@Path("/subscribe/{id}/{channel}")
	public void subscribe(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @PathParam("channel") String channel) {
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
