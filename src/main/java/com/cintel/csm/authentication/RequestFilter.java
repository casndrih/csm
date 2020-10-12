package com.cintel.csm.authentication;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.cintel.csm.utilities.ProjectConstants;

/**
 * Request filter. 
 * <ul>
 * <li>always allow /help</li>
 * <li>deny if service key is invalid</li>
 * <li>allow all GET</li>
 * <li>allow all POST to /auth or /users
 * <li>deny if auth token is invalid</li>
 * </ul>
 * @author Simone Ripamonti
 *
 */
@Provider
@PreMatching
public class RequestFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String path = requestContext.getUriInfo().getPath();

		System.out.println("[FILTERING] path "+path);
		
		// IMPORTANT!!! First, Acknowledge any pre-flight test from browsers for
		// this case before validating the headers (CORS stuff)
		if (requestContext.getRequest().getMethod().equals("OPTIONS")) {
			requestContext.abortWith(Response.status(Response.Status.OK).build());
			return;
		}

		// allow help page
		if (path.startsWith("help")) {
			return;
		}
		
		
		// check if service key exists and is valid
		String serviceKey = requestContext.getHeaderString(ProjectConstants.SERVICE_KEY);
		if (!Authenticator.isServiceKeyValid(serviceKey)) {
			// Kick anyone without a valid service key
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Service key is invalid").build());
			return;
		}

		// authorize all gets
		if (requestContext.getMethod().equals("GET")) {
			return;
		}

		// authorize login and creation
		if (path.startsWith("auth") || path.startsWith("users")) {
			return;
		}

		String authToken = requestContext.getHeaderString(ProjectConstants.AUTH_TOKEN);
		if (!Authenticator.isAuthTokenValid(authToken)) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Auth token is invalid").build());
			return;
		}
		
		return;

	}

}
