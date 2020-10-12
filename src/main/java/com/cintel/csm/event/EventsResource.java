package com.cintel.csm.event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cintel.csm.authentication.Authenticator;
import com.cintel.csm.utilities.Message;
import com.cintel.csm.utilities.ProjectConstants;
import com.cintel.csm.utilities.exceptions.AuthorizationDBException;
import com.cintel.csm.utilities.exceptions.EventDBException;
import com.cintel.csm.utilities.exceptions.NoEventCreatedException;
import com.cintel.csm.utilities.exceptions.NoEventFoundException;
import com.cintel.csm.utilities.exceptions.NoUserFoundException;
import com.cintel.csm.utilities.exceptions.NoVoteCreatedException;
import com.cintel.csm.utilities.exceptions.VotesDBException;
/**
 * Resource representing events
 * <ul>
 * <li>GET /events</li>
 * <li>POST /events</li>
 * <li>GET /events/{id}</li>
 * <li>DELETE /events/{id}</li>
 * <li>POST /events/{id}/vote</li>
 * <li>DELETE /events/{id}/vote</li>
 * </ul>
 * @author Simone Ripamonti
 *
 */
@Path("/events")
public class EventsResource {
	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	/**
	 * Lists events, two ways are provided to filter events Uses
	 * latMin+latMax+lonMin+lonMax XOR lat+lon+radius
	 * 
	 * For a rectangle area search
	 * 
	 * @param latMin
	 * @param latMax
	 * @param lonMin
	 * @param lonMax
	 * 
	 *            For a search based on center and radius
	 * @param lat
	 * @param lon
	 * @param radius
	 * @return list of events matching the parameters (if any), or BAD_REQUEST
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response listEvents(@DefaultValue("") @QueryParam("latMin") String latMin,
			@DefaultValue("") @QueryParam("latMax") String latMax,
			@DefaultValue("") @QueryParam("lonMin") String lonMin,
			@DefaultValue("") @QueryParam("lonMax") String lonMax, @DefaultValue("") @QueryParam("lat") String lat,
			@DefaultValue("") @QueryParam("lon") String lon, @DefaultValue("") @QueryParam("rad") String rad) {
		if (NumberUtils.isNumber(latMin) && NumberUtils.isNumber(latMax) && NumberUtils.isNumber(lonMin)
				&& NumberUtils.isNumber(lonMax)) {
			Float latitudeMin, latitudeMax, longitudeMin, longitudeMax;
			latitudeMin = NumberUtils.toFloat(latMin);
			latitudeMax = NumberUtils.toFloat(latMax);
			longitudeMin = NumberUtils.toFloat(lonMin);
			longitudeMax = NumberUtils.toFloat(lonMax);

			try {
				List<Event> events = EventStorage.instance.getByArea(latitudeMin, latitudeMax, longitudeMin,
						longitudeMax);
				return Response.ok(events).build();
			} catch (EventDBException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e.getMessage())).build();
			}
		}
		if (NumberUtils.isNumber(lat) && NumberUtils.isNumber(lon) && NumberUtils.isNumber(rad)) {
			Float latitude, longitude, radius;
			latitude = NumberUtils.toFloat(lat);
			longitude = NumberUtils.toFloat(lon);
			radius = NumberUtils.toFloat(rad);

			try {
				List<Event> events = EventStorage.instance.getByRadius(latitude, longitude, radius);
				return Response.ok(events).build();

			} catch (EventDBException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e.getMessage())).build();

			}
		}
		return Response.status(Status.BAD_REQUEST).entity(new Message("EVENTS", "Please check the parameters!"))
				.build();
	}

	/**
	 * Create a new event
	 * 
	 * @param eventType
	 *            type of the event (mandatory)
	 * @param description
	 *            of the event
	 * 
	 *            Required to provide position as name of the location:
	 * @param country
	 *            of the event
	 * @param city
	 *            of the event
	 * @param street
	 *            of the event
	 * 
	 *            Or as coordinates:
	 * @param latitude
	 *            of the event
	 * @param longitude
	 *            of the event
	 * 
	 * @param authToken
	 *            (mandatory), used to assign the creator to the event
	 * @return UNAUTHORIZED if auth_token is invalid, BAD_REQUEST if the event
	 *         cannot be created with that parameters, CREATED if the event was
	 *         successfully created
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response createEvent(@FormParam("eventType") String eventType, @FormParam("description") String description,
			@FormParam("country") String country, @FormParam("city") String city, @FormParam("street") String street,
			@FormParam("latitude") String latitude, @FormParam("longitude") String longitude,
			@HeaderParam(ProjectConstants.AUTH_TOKEN) String authToken) {

		int userId;
		try {
			userId = Authenticator.getUserId(authToken);
		} catch (AuthorizationDBException e2) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("AUTHORIZATION", e2.getMessage()))
					.build();
		} catch (NoUserFoundException e2) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(new Message("AUTHORIZTION", "Your auth token is not valid!")).build();
		}

		EventType et = EventType.valueOf(eventType);

		if (NumberUtils.isNumber(latitude) && NumberUtils.isNumber(longitude)) {
			float lat = NumberUtils.toFloat(latitude);
			float lon = NumberUtils.toFloat(longitude);

			// get country, city, street
			String[] address = getAddress(lat, lon);

			Event e = new Event();
			e.setCountry(address[0]);
			e.setCity(address[1]);
			e.setStreet(address[2]);
			e.setEventType(et);
			e.setLatitude(lat);
			e.setLongitude(lon);
			e.setSubmitterId(userId);

			int id;
			try {
				id = EventStorage.instance.add(e);
			} catch (EventDBException e1) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e1.getMessage()))
						.build();
			} catch (NoEventCreatedException e1) {
				return Response.status(Status.BAD_REQUEST)
						.entity(new Message("EVENTS", "Please check the passed parameters!")).build();
			}
			return Response.created(URI.create(ProjectConstants.EVENTS_BASE_URL + "/" + String.valueOf(id))).build();

		} else {
			float[] coordinates = getCoordinates(country, city, street);

			if (coordinates == null) {
				// cannot find coordinates, aborting
				return Response.status(Status.BAD_REQUEST)
						.entity(new Message("EVENTS", "Please provide valid city-street-address or coordinates"))
						.build();
			}

			float lat = coordinates[0];
			float lon = coordinates[1];

			// valid coordinates found, save the new event
			Event e = new Event();
			e.setCountry(country);
			e.setCity(city);
			e.setStreet(street);
			e.setEventType(et);
			e.setLatitude(lat);
			e.setLongitude(lon);
			e.setSubmitterId(userId);

			int id;
			try {
				id = EventStorage.instance.add(e);
			} catch (EventDBException e1) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e1.getMessage()))
						.build();
			} catch (NoEventCreatedException e1) {
				return Response.status(Status.BAD_REQUEST)
						.entity(new Message("EVENTS", "Please check the passed parameters!")).build();
			}
			return Response.created(URI.create(ProjectConstants.EVENTS_BASE_URL + "/" + String.valueOf(id))).build();
		}
	}

	/**
	 * Returns an event based on the provided id
	 * 
	 * @param id
	 *            of the event
	 * @return the requested event, or NOT_FOUND or BAD_REQUEST
	 */
	@Path("{id}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getEventById(@PathParam("id") String id) {
		if (NumberUtils.isNumber(id)) {
			Event e;
			try {
				e = EventStorage.instance.getById(NumberUtils.toInt(id));
				return Response.ok(e).build();
			} catch (NoEventFoundException e1) {
				return Response.status(Status.NOT_FOUND)
						.entity(new Message("EVENTS", "No event with id " + id + " has been found")).build();

			} catch (EventDBException e1) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e1.getMessage()))
						.build();

			}
		}
		return Response.status(Status.BAD_REQUEST).entity(new Message("EVENTS", "Id must be a valid positive integer!"))
				.build();

	}

	/**
	 * Removes the event with the specified id
	 * 
	 * @param id
	 *            of the event
	 * @return NO_CONTENT if delete was successfull, NOT_FOUND if there is no
	 *         event with that id, BAD_REQUEST if id is not valid
	 */
	@DELETE
	@Path("{id}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response deleteEvent(@PathParam("id") String id,
			@HeaderParam(ProjectConstants.AUTH_TOKEN) String authToken) {
		if (NumberUtils.isNumber(id)) {

			// find who is requesting the delete
			int requestingUser;
			try {
				requestingUser = Authenticator.getUserId(authToken);
			} catch (AuthorizationDBException e2) {
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new Message("AUHTORIZATION", e2.getMessage())).build();
			} catch (NoUserFoundException e2) {
				return Response.status(Status.UNAUTHORIZED)
						.entity(new Message("AUTHORIZATION", "Your auth token is not valid!")).build();
			}
			// find if he is superuser
			boolean superuser;
			try {
				superuser = Authenticator.isSuperuser(requestingUser);
			} catch (NoUserFoundException e1) {
				// should never happen!
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new Message("AUTHORIZATION", "User disappeared! Something went really wrong :("))
						.build();
			} catch (AuthorizationDBException e1) {
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new Message("AUTHORIZATION", e1.getMessage())).build();
			}

			// find the owner of the event
			int ownerUser;
			try {
				ownerUser = EventStorage.instance.getSubmitter(NumberUtils.toInt(id));
			} catch (EventDBException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e.getMessage()))
						.build();
			} catch (NoEventFoundException e) {
				return Response.status(Status.NOT_FOUND).entity(new Message("EVENTS", "No event with id " + id))
						.build();
			}

			if (requestingUser != ownerUser || !superuser) {
				return Response.status(Status.UNAUTHORIZED)
						.entity(new Message("AUTHORIZATION", "You are not the owner of event " + id)).build();
			}

			boolean result;
			try {
				result = EventStorage.instance.remove(NumberUtils.toInt(id));
			} catch (EventDBException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e.getMessage()))
						.build();
			}
			if (result) {
				return Response.status(Status.NO_CONTENT).build();
			}
		}
		return Response.status(Status.BAD_REQUEST).entity(new Message("EVENTS", "Id must be a valid positive integer!"))
				.build();
	}

	@POST
	@Path("{id}/vote")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response vote(@PathParam("id") String eventId, @HeaderParam(ProjectConstants.AUTH_TOKEN) String authToken) {

		// check if user is valid
		int userId;
		try {
			userId = Authenticator.getUserId(authToken);
		} catch (AuthorizationDBException e2) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("AUTHORIZATION", e2.getMessage()))
					.build();
		} catch (NoUserFoundException e2) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(new Message("AUTHORIZATION", "Your auth token is not valid!")).build();
		}

		// check if event is valid
		try {
			EventStorage.instance.getById(NumberUtils.toInt(eventId));
		} catch (NoEventFoundException e1) {
			return Response.status(Status.NOT_FOUND)
					.entity(new Message("EVENTS", "No event with id " + eventId + " has been found")).build();
		} catch (EventDBException e1) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e1.getMessage())).build();
		}

		// submit the vote
		if (NumberUtils.isNumber(eventId)) {
			try {
				boolean result = EventStorage.instance.vote(userId, NumberUtils.toInt(eventId));
				if (result) {
					return Response.status(Status.NO_CONTENT).build();
				} else {
					return Response.status(Status.NO_CONTENT)
							.entity(new Message("EVENTS", "This user already voted this event!")).build();
				}
			} catch (VotesDBException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e.getMessage()))
						.build();
			} catch (NoVoteCreatedException e) {
				return Response.status(Status.NO_CONTENT)
						.entity(new Message("EVENTS", "This user already voted this event!")).build();
			}
		}
		return Response.status(Status.BAD_REQUEST).entity(new Message("EVENTS", "Id must be a valid positive integer!"))
				.build();
	}

	@DELETE
	@Path("{id}/vote")
	public Response unvote(@PathParam("id") String eventId, @HeaderParam(ProjectConstants.AUTH_TOKEN) String authToken) {
		// check if user is valid
				int userId;
				try {
					userId = Authenticator.getUserId(authToken);
				} catch (AuthorizationDBException e2) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("AUTHORIZATION", e2.getMessage()))
							.build();
				} catch (NoUserFoundException e2) {
					return Response.status(Status.UNAUTHORIZED)
							.entity(new Message("AUTHORIZATION", "Your auth token is not valid!")).build();
				}

				// check if event is valid
				try {
					EventStorage.instance.getById(NumberUtils.toInt(eventId));
				} catch (NoEventFoundException e1) {
					return Response.status(Status.NOT_FOUND)
							.entity(new Message("EVENTS", "No event with id " + eventId + " has been found")).build();
				} catch (EventDBException e1) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e1.getMessage())).build();
				}

				// submit the vote
				if (NumberUtils.isNumber(eventId)) {
					try {
						boolean result = EventStorage.instance.unvote(userId, NumberUtils.toInt(eventId));
						if (result) {
							return Response.status(Status.NO_CONTENT).entity(new Message("EVENTS", "Vote deleted")).build();
						} else {
							return Response.status(Status.NO_CONTENT)
									.entity(new Message("EVENTS", "Vote already doesn't exist!")).build();
						}
					} catch (VotesDBException e) {
						return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Message("EVENTS", e.getMessage()))
								.build();
					}
				}
				return Response.status(Status.BAD_REQUEST).entity(new Message("EVENTS", "Id must be a valid positive integer!"))
						.build();
	}

	/**
	 * Find coordinates given country, city and street
	 * 
	 * @param country
	 * @param city
	 * @param street
	 * @return [latitude, longitude] if found, otherwise null
	 */
	private float[] getCoordinates(String country, String city, String street) {
		String union = country + ", " + city + ", " + street;
		URL url;
		try {
			url = new URL("http://maps.googleapis.com/maps/api/geocode/json?address=" + URIUtil.encodeQuery(union)
					+ "&sensor=true");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			if (conn.getResponseCode() != 200) {
				return null;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output = "", json = "";
			while ((output = br.readLine()) != null) {
				json += output;
			}

			JSONObject obj = new JSONObject(json);
			String latString = (String) obj.getJSONArray("results").getJSONObject(0).getJSONObject("geometry")
					.getJSONObject("location").get("latitude");
			String lonString = (String) obj.getJSONArray("results").getJSONObject(0).getJSONObject("geometry")
					.getJSONObject("location").get("longitude");

			if (NumberUtils.isNumber(latString) && NumberUtils.isNumber(lonString)) {
				float lat = NumberUtils.toFloat(latString);
				float lon = NumberUtils.toFloat(lonString);
				float[] result = { lat, lon };
				return result;
			}

		} catch (IOException e) {
			return null;
		}

		return null;
	}

	/**
	 * Find country, city, address given the coordinates
	 * 
	 * @param latitude
	 * @param longitude
	 * @return [country, city, address] if applicable
	 */
	private String[] getAddress(float latitude, float longitude) {
		String union = String.valueOf(latitude) + "," + String.valueOf(longitude);
		URL url;
		try {
			url = new URL("http://maps.googleapis.com/maps/api/geocode/json?address=" + URIUtil.encodeQuery(union)
					+ "&sensor=true");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			if (conn.getResponseCode() != 200) {
				return null;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output = "", json = "";
			while ((output = br.readLine()) != null) {
				json += output;
			}

			JSONObject obj = new JSONObject(json);
			String streetNumber = "";
			String[] result = new String[3];

			JSONArray addressComponents = obj.getJSONArray("results").getJSONObject(0)
					.getJSONArray("address_components");
			for (int i = 0; i < addressComponents.length(); i++) {
				String types = (String) addressComponents.getJSONObject(i).getJSONArray("types").getString(0);

				if (types.equals("street_number")) {
					streetNumber = (String) addressComponents.getJSONObject(i).getString("long_name");
				} else if (types.equals("route")) {
					result[2] = (String) addressComponents.getJSONObject(i).getString("long_name");
				} else if (types.equals("administrative_area_level_3")) {
					result[1] = (String) addressComponents.getJSONObject(i).getString("long_name");
				} else if (types.equals("country")) {
					result[0] = (String) addressComponents.getJSONObject(i).getString("long_name");
				}
			}

			if (!streetNumber.equals("")) {
				result[2] = result[2] + ", " + streetNumber;
			}

			return result;

		} catch (IOException e) {
			return null;
		}
	}
}
