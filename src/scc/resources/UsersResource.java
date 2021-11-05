package scc.resources;

import scc.data.user.User;
import scc.data.user.UserDAO;
import scc.data.user.UsersDBLayer;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.UUID;

/**
 * Resource for managing users.
 */
@Path("/users")
public class UsersResource
{
	@Context
	ServletContext context;

	public UsersResource() {}

	/**
	 * Create a new user.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(User user) {
		String userId = UUID.randomUUID().toString();
		user.setId(userId);

		if(UsersDBLayer.getInstance(context).putUser(new UserDAO(user)).getStatusCode() >= 400)
			throw new BadRequestException();

		return userId;
	}
	
	/**
	 * Update a new user.
	 */
	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(User user) {
		if(UsersDBLayer.getInstance(context).updateUser(new UserDAO(user)).getStatusCode() >= 400)
			throw new BadRequestException();
	}

	/**
	 * Delete a user.
	 */
	@DELETE
	@Path("/{id}")
	public void delete(@PathParam("id") String id) {
		if(UsersDBLayer.getInstance(context).delUserById(id).getStatusCode() >= 400)
			throw new BadRequestException();
	}

	/**
	 * Lists the ids of images stored.
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public User get(@PathParam("id") String id) {
		Optional<UserDAO> userDAO = UsersDBLayer.getInstance(context).getUserById(id).stream().findFirst();
		if(userDAO.isEmpty())
			throw new NotFoundException();

		return userDAO.get().toUser();
	}
}
