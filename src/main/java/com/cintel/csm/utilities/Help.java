package com.cintel.csm.utilities;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("help")
public class Help {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
    	String txt = "--- CSM v 0.0.1 ---";
    	txt += "\n\n\tDescription:";
    	txt += "\n + Support JSON and XML";
    	txt += "\n + Authorization token must be passed as HTTP header in 'auth_token' field";
    	txt += "\n + Service key must be passed as HTT header in 'service_key' field";
    	txt += "\n\n\tAvailable REST methods:";
    	txt += "\n + /events GET -> List of all events";
    	txt += "\n + /events POST -> Create an event";
    	txt += "\n + /events/{id} GET -> Detail of an event";
    	txt += "\n + /events/{id} DELETE -> Delete an event";
    	txt += "\n + /events/{id}/vote POST -> Add a vote to the event";
    	txt += "\n + /events/{id}/vote DELETE -> Delete a vote from an event";
    	txt += "\n + /users/classic POST -> Create user using email, username and password";
    	txt += "\n + /users/{id} GET -> Detail of an user";
    	txt += "\n + /users/{id}/events GET -> Events posted by an user";
    	txt += "\n + /auth/classic POST -> Login using username and password";
    	txt += "\n + /auth/logout POST -> Invalidates an authentication token";

    	return txt;
    }
}
