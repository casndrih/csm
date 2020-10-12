package com.cintel.csm.authentication;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.cintel.csm.utilities.Message;
import com.cintel.csm.utilities.ProjectConstants;
import com.cintel.csm.utilities.exceptions.AuthorizationDBException;
import com.cintel.csm.utilities.exceptions.NoTokenCreatedException;
import com.cintel.csm.utilities.exceptions.NoUserFoundException;
import com.cintel.csm.utilities.exceptions.SecretDBException;

/**
 * Authentication resource, exposed at /auth supports login with
 * username+password
 * 
 * @author Simone Ripamonti
 *
 */
@Path("auth")
public class AuthenticationResource {

	/**
	 * Login using username and password
	 * 
	 * @param username
	 * @param password
	 * @return OK, with the authorization token; UNAUTHORIZED, if the login is
	 *         incorrect; INTERNAL_SERVER_ERROR, if there was a problem with the
	 *         request
	 */
	@POST
	@Path("/classic")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response loginClassic(@FormParam("username") String username, @FormParam("password") String password) {
		try {
			int userId = Authenticator.checkPassword(username, password);
			String authToken = Authenticator.generateToken(userId);
			AuthToken toBeReturned = new AuthToken();
			toBeReturned.setAuthToken(authToken);
			toBeReturned.setUserId(userId);
			toBeReturned.setUsername(username);
			return Response.ok(toBeReturned).build();
		} catch (NoUserFoundException e) {
			return Response.status(Status.UNAUTHORIZED).entity(new Message("AUTHORIZATION", "Login is incorrect"))
					.build();
		} catch (SecretDBException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("DATABASE", e.getMessage()))
					.build();
		} catch (AuthorizationDBException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("DATABASE", e.getMessage()))
					.build();
		} catch (NoTokenCreatedException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new Message("DATABASE", "Problem in generating the token")).build();
		}

	}

	/**
	 * Logout by invalidating a token
	 * 
	 * @param authToken
	 * @return OK, if the provided token was valid and now it is no more;
	 *         UNAUTHORIZED, if the token is already invalid;
	 *         INTERNAL_SERVER_ERROR, if there was a problem with your request
	 */
	@POST
	@Path("/logout")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response logout(@HeaderParam(ProjectConstants.AUTH_TOKEN) String authToken) {
		try {
			int userId = Authenticator.getUserId(authToken);
			Authenticator.invalidateToken(userId);
			return Response.ok(new Message("AUTHORIZATION", "Logged out, discard your token")).build();
		} catch (AuthorizationDBException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("DATABASE", e.getMessage()))
					.build();
		} catch (NoUserFoundException e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(new Message("AUTHORIZATION", "Your auth token is already invalid")).build();
		}
	}

}
