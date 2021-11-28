package scc.resources;

import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
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
@Path("/users")
public class UsersResource
{
	@Context
	ServletContext context;

	public UsersResource() {}


	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(User user) {
		String userId = UUID.randomUUID().toString().replace("-", "");
		user.setId(userId);

		UsersDBLayer.getInstance(context).createUser(new UserDAO(user));

		return userId;
	}
	

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(@CookieParam("scc:session") Cookie session, User user) {
		UsersDBLayer.getInstance(context).checkCookieUser(session,user.getId());
		UsersDBLayer.getInstance(context).updateUser(new UserDAO(user));
	}


	@DELETE
	@Path("/{id}")
	public void delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		UsersDBLayer.getInstance(context).checkCookieUser(session,id);
		UsersDBLayer.getInstance(context).discardUserById(id);
	}

	@DELETE
	@Path("/force/{id}")
	public void forceDelete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		UsersDBLayer.getInstance(context).checkCookieUser(session,id);
		UsersDBLayer.getInstance(context).delUserById(id);
	}


	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public User get(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
		UsersDBLayer.getInstance(context).checkCookieUser(session,id);
		return UsersDBLayer.getInstance(context).getUserById(id).toUser();
	}


	@POST
	@Path("/subscribe/{id}/{channel}")
	public void subscribe(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @PathParam("channel") String channel) {
		UsersDBLayer.getInstance(context).checkCookieUser(session, id);

		ChannelDAO channelDAO = ChannelsDBLayer.getInstance(context).getChannelById(channel);
		UserDAO userDAO = UsersDBLayer.getInstance(context).getUserById(id);

		if(channelDAO.isPublicChannel()) {
			Set<String> channels = Arrays.stream(userDAO.getChannelIds()).collect(Collectors.toSet());
			channels.add(channel);
			userDAO.setChannelIds(channels.toArray(new String[0]));
			UsersDBLayer.getInstance(context).updateUser(userDAO);

			Set<String> members = Arrays.stream(channelDAO.getMembers()).collect(Collectors.toSet());
			members.add(userDAO.getId());
			channelDAO.setMembers(members.toArray(new String[0]));
			ChannelsDBLayer.getInstance(context).updateChannel(channelDAO);
		}
	}


	@POST
	@Path("/invite/{id}/{channel}/{other}")
	public void invite(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @PathParam("channel") String channel, @PathParam("other") String other) {
		UsersDBLayer.getInstance(context).checkCookieUser(session, id);

		ChannelDAO channelDAO = ChannelsDBLayer.getInstance(context).getChannelById(channel);
		UserDAO userDAO = UsersDBLayer.getInstance(context).getUserById(other);

		if(Arrays.asList(channelDAO.getMembers()).contains(id)) {
			Set<String> channels = Arrays.stream(userDAO.getChannelIds()).collect(Collectors.toSet());
			channels.add(channel);
			userDAO.setChannelIds(channels.toArray(new String[0]));
			UsersDBLayer.getInstance(context).updateUser(userDAO);

			Set<String> members = Arrays.stream(channelDAO.getMembers()).collect(Collectors.toSet());
			members.add(other);
			channelDAO.setMembers(members.toArray(new String[0]));
			ChannelsDBLayer.getInstance(context).updateChannel(channelDAO);
		}
	}
}
