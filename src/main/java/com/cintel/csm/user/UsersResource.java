package com.cintel.csm.user;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.validator.routines.EmailValidator;

import com.cintel.csm.event.Event;
import com.cintel.csm.event.EventStorage;
import com.cintel.csm.utilities.Message;
import com.cintel.csm.utilities.ProjectConstants;
import com.cintel.csm.utilities.exceptions.EventDBException;
import com.cintel.csm.utilities.exceptions.NoUserCreatedException;
import com.cintel.csm.utilities.exceptions.NoUserFoundException;
import com.cintel.csm.utilities.exceptions.UserDBException;

@Path("/users")
public class UsersResource {
	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	/**
	 * Create a new user
	 * 
	 * @param username
	 *            (unique)
	 * @param email
	 *            (unique)
	 * @param password
	 *            at least 8 chars long, containing at least one uppercase, one
	 *            lowercase and one number
	 * @return BAD_REQUEST if email/username/password not valid and if
	 *         username/email already in use, CREATED if the user was
	 *         successfully created
	 */
	@Path("classic")
	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createUser(@FormParam("username") String username, @FormParam("email") String email,
			@FormParam("password") String password) {

		if (!EmailValidator.getInstance().isValid(email)) {
			return Response.status(Status.BAD_REQUEST).entity(new Message("USERS", "Email is not valid")).build();
		}
		// 8 characters, at least 1 lowercase, 1 uppercase, 1 number, no spaces
		String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$";
		if (!password.matches(pattern)) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new Message("USERS",
							"Password must be 8 characters long, containing at least one uppercase, one lowercase and one number"))
					.build();
		}
		// 4-20 characters, no special symbols and spaces
		pattern = "^(?=.*[A-Za-z0-9])(?=\\S+$).{4,20}$";
		if (!username.matches(pattern)) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new Message("USERS",
							"Username must be between 4 and 20 characters, not containing spaces or special symbols"))
					.build();
		}
		User u = new User();
		u.setCreated(new Date());
		u.setEmail(email);
		u.setUsername(username);
		int id;
		try {
			id = UserStorage.instance.addWithPassword(u, password);
			return Response.created(URI.create(ProjectConstants.USERS_BASE_URL + "/" + String.valueOf(id))).build();
		} catch (UserDBException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("USERS", e.getMessage())).build();
		} catch (NoUserCreatedException e) {
			return Response.status(Status.BAD_REQUEST).entity(new Message("USERS", "Username or email already in use"))
					.build();
		}

	}

	/**
	 * Shows information about a specified user (username, email)
	 * 
	 * @param id
	 *            of the user
	 * @return BAD_REQUEST if not a valid id, NOT_FOUND if the user does not
	 *         exist, OK if the user exists
	 */
	@Path("{id}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getUser(@PathParam("id") String id) {
		if (NumberUtils.isNumber(id)) {
			User u;
			try {
				u = UserStorage.instance.getById(NumberUtils.toInt(id));
				return Response.ok(u).build();
			} catch (NoUserFoundException e) {
				return Response.status(Status.NOT_FOUND).entity(new Message("USERS","No user with id " + id + " has been found")).build();
			} catch (UserDBException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("USERS",e.getMessage())).build();
			}

		}
		return Response.status(Status.BAD_REQUEST).entity(new Message("USERS","Id must be a valid positive integer!")).build();
	}

	/**
	 * Returns the events created by this user
	 * 
	 * @param id
	 *            of the user
	 * @return BAD_REQUEST if not a valid id, NOT_FOUND if the user does not
	 *         exist, OK if the user exists
	 */
	@Path("{id}/events")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getUserEvents(@PathParam("id") String id) {
		if (NumberUtils.isNumber(id)) {
			try {
				// just used to generate exception if user does not exist!
				UserStorage.instance.getById(NumberUtils.toInt(id));
				// finding events for this user
				List<Event> events = EventStorage.instance.getByUser(NumberUtils.toInt(id));
				return Response.ok(events).build();
			} catch (NoUserFoundException e) {
				return Response.status(Status.NOT_FOUND).entity(new Message("USERS","No user with id " + id + " has been found")).build();
			} catch (UserDBException | EventDBException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("USERS",e.getMessage())).build();
			}

		}
		return Response.status(Status.BAD_REQUEST).entity(new Message("USERS","Id must be a valid positive integer!")).build();
	}

}
