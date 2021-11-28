package scc.resources;

import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
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
@Path("/users")
public class UsersResource
{

	public UsersResource() {}


	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(User user) {
		String userId = UUID.randomUUID().toString();
		user.setIdUser(userId);

		UsersDBLayer.getInstance().putUser(new UserDAO(user));

		return userId;
	}
	

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(@CookieParam("scc:session") Cookie session, User user) {
		UsersDBLayer.getInstance().checkCookieUser(session,user.getIdUser());
		UsersDBLayer.getInstance().updateUser(new UserDAO(user));
	}


	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		UsersDBLayer.getInstance().checkCookieUser(session,id);
		UsersDBLayer.getInstance().discardUserById(id);
	}

	@DELETE
	@Path("/force/{id}")
	public void forceDelete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		UsersDBLayer.getInstance().checkCookieUser(session,id);
		UsersDBLayer.getInstance().delUserById(id);
	}


	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public User get(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		UsersDBLayer.getInstance().checkCookieUser(session,id);
		return UsersDBLayer.getInstance().getUserById(id).toUser();
	}


	@POST
	@Path("/subscribe/{id}/{channel}")
	public void subscribe(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @PathParam("channel") String channel) {
		UsersDBLayer.getInstance().checkCookieUser(session, id);

		ChannelDAO channelDAO = ChannelsDBLayer.getInstance().getChannelById(channel);
		UserDAO userDAO = UsersDBLayer.getInstance().getUserById(id);

		if(channelDAO.isPublicChannel()) {
			Set<String> channels = Arrays.stream(userDAO.getChannelIds()).collect(Collectors.toSet());
			channels.add(channel);
			userDAO.setChannelIds(channels.toArray(new String[0]));
			UsersDBLayer.getInstance().updateUser(userDAO);

			Set<String> members = Arrays.stream(channelDAO.getMembers()).collect(Collectors.toSet());
			members.add(userDAO.getIdUser());
			channelDAO.setMembers(members.toArray(new String[0]));
			ChannelsDBLayer.getInstance().updateChannel(channelDAO);
		}
	}


	@POST
	@Path("/invite/{id}/{channel}/{other}")
	public void invite(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @PathParam("channel") String channel, @PathParam("other") String other) {
		UsersDBLayer.getInstance().checkCookieUser(session, id);

		ChannelDAO channelDAO = ChannelsDBLayer.getInstance().getChannelById(channel);
		UserDAO userDAO = UsersDBLayer.getInstance().getUserById(other);

		if(Arrays.asList(channelDAO.getMembers()).contains(id)) {
			Set<String> channels = Arrays.stream(userDAO.getChannelIds()).collect(Collectors.toSet());
			channels.add(channel);
			userDAO.setChannelIds(channels.toArray(new String[0]));
			UsersDBLayer.getInstance().updateUser(userDAO);

			Set<String> members = Arrays.stream(channelDAO.getMembers()).collect(Collectors.toSet());
			members.add(id);
			channelDAO.setMembers(members.toArray(new String[0]));
			ChannelsDBLayer.getInstance().updateChannel(channelDAO);
		}
	}
}
