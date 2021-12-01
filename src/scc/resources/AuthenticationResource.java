package scc.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import scc.data.authentication.Login;
import scc.data.authentication.Session;
import scc.data.user.UserDAO;
import scc.data.user.UsersDBLayer;

@Path("/auth")
public class AuthenticationResource {

	private UsersDBLayer usersDBLayer;

	public AuthenticationResource() {
	}

	@PUT
	@Path("/start")
	public void start() {
		this.usersDBLayer = new UsersDBLayer();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response auth(Login login) {
		UserDAO userDAO = usersDBLayer.getUserById(login.getUser());
		if (userDAO == null)
			throw new NotFoundException();

		if (userDAO.getPwd().equals(login.getPwd())) {
			Session session = new Session(login.getUser());
			NewCookie cookie = new NewCookie("scc:session", session.getSessionId(), "/", null, "sessionid", 3600, false, true);
			usersDBLayer.putSession(session);
			return Response.ok().cookie(cookie).build();
		} else
			throw new WebApplicationException(403);
	}

}
