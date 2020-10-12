package com.cintel.csm.event;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.cintel.csm.utilities.database.Database;
import com.cintel.csm.utilities.exceptions.EventDBException;
import com.cintel.csm.utilities.exceptions.NoEventCreatedException;
import com.cintel.csm.utilities.exceptions.NoEventFoundException;
import com.cintel.csm.utilities.exceptions.NoVoteCreatedException;
import com.cintel.csm.utilities.exceptions.VotesDBException;

public enum EventStorage {
	instance;

	/**
	 * Add event to the storage
	 * 
	 * @param e
	 *            event to be added
	 * @return id of the added event
	 * @throws NoEventCreatedException
	 * @throws EventDBException
	 */
	public int add(Event e) throws EventDBException, NoEventCreatedException {
		System.out.println("[DB] Creating new event " + e);
		try (Connection connection = Database.getConnection()) {
			PreparedStatement createStmt = connection.prepareStatement(
					"INSERT INTO gsx95369n3oh2zo6.events (DATE, EVENTTYPE, DESCRIPTION,"
							+ "COUNTRY, CITY, STREET, LATITUDE, LONGITUDE, SUBMITTERID) VALUES (?,?,?,?,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			createStmt.setDate(1, (Date) e.getDate());
			createStmt.setString(2, e.getEventType().toString());
			createStmt.setString(3, e.getDescription());
			createStmt.setString(4, e.getCountry());
			createStmt.setString(5, e.getCity());
			createStmt.setString(6, e.getStreet());
			createStmt.setFloat(7, e.getLatitude());
			createStmt.setFloat(8, e.getLongitude());
			createStmt.setInt(9, e.getSubmitterId());
			createStmt.executeUpdate();
			ResultSet results = createStmt.getGeneratedKeys();
			System.out.println("[DB] Update executed");
			if (results.next()) {
				int id = results.getInt(1);
				System.out.println("[DB] Created event id " + id);
				return id;
			} else {
				System.out.println("[DB] No event created");
				throw new NoEventCreatedException();
			}
		} catch (SQLException | URISyntaxException | ClassNotFoundException e1) {
			System.out.println("[DB] EXCEPTION in EventStorage.add()");
			System.out.println(e1.getMessage());
			throw new EventDBException("ERROR when creating event", e1);
		}
	}

	/**
	 * Remove event from the storage
	 * 
	 * @param id
	 *            event to be removed
	 * @return true if event is removed, otherwise false
	 * @throws EventDBException
	 */
	public boolean remove(int id) throws EventDBException {
		System.out.println("[DB] Removing event with id " + id);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement deleteStmt = connection.prepareStatement(
					"DELETE FROM gsx95369n3oh2zo6.events WHERE ID = ?", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			deleteStmt.clearParameters();
			deleteStmt.setInt(1, id);
			int count = deleteStmt.executeUpdate();
			System.out.println("[DB] Update executed");
			if (count > 0) {
				System.out.println("[DB] Event deleted");
				return true;
			} else {
				System.out.println("[DB] No event deleted");
				return false;
			}
		} catch (URISyntaxException | SQLException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in EventStorage.remove()");
			System.out.println(e.getMessage());
			throw new EventDBException("ERROR in deleting event by id", e);
		}
	}

	/**
	 * Find event based on latitude and longitude
	 * 
	 * @param latitudeMin
	 *            of the event
	 * @param latitudeMax
	 *            of the event
	 * @param longiduteMin
	 *            of the event
	 * @param longiduteMax
	 *            of the event
	 * @return a list of events in the selected area
	 * @throws EventDBException
	 */
	public List<Event> getByArea(Float latitudeMin, Float latitudeMax, Float longitudeMin, Float longitudeMax)
			throws EventDBException {
		System.out.println("[DB] Getting list of events by area");
		try (Connection connection = Database.getConnection()) {

			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT * FROM gsx95369n3oh2zo6.events WHERE LATITUDE < ? AND LATITUDE > ? AND LONGITUDE < ? AND LONGITUDE > ?",
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setFloat(1, latitudeMax);
			getStmt.setFloat(2, latitudeMin);
			getStmt.setFloat(3, longitudeMax);
			getStmt.setFloat(4, longitudeMin);
			ResultSet result = getStmt.executeQuery();
			System.out.println("[DB] Query executed");
			List<Event> events = new ArrayList<Event>();

			while (result.next()) {
				Event temp = new Event();
				temp.setId(result.getInt(1));
				temp.setDate(result.getDate(2));
				temp.setEventType(EventType.valueOf(result.getString(3)));
				temp.setDescription(result.getString(4));
				temp.setCountry(result.getString(5));
				temp.setCity(result.getString(6));
				temp.setStreet(result.getString(7));
				temp.setLatitude(result.getFloat(8));
				temp.setLongitude(result.getFloat(9));
				temp.setSubmitterId(result.getInt(10));

				try {
					System.out.println("[DB] Finding votes");
					int votes = getVotes(temp.getId());
					temp.setVotes(votes);
					System.out.println("[DB] Found " + votes);
				} catch (VotesDBException e) {
					System.out.println("[DB] No votes found");
					temp.setVotes(0);
				}

				events.add(temp);
			}
			System.out.println("[DB] Found " + events.size() + " events");
			return events;
		} catch (URISyntaxException | SQLException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in EventStorage.getByArea()");
			System.out.println(e.getMessage());
			throw new EventDBException("ERROR when finding events by coordinates", e);
		}
	}

	/**
	 * Find events based on radius starting from a position
	 * 
	 * @param latitude
	 *            of the center
	 * @param longitude
	 *            of the center
	 * @param radius
	 *            where to search
	 * @return a list of events in the selected area
	 * @throws EventDBException
	 */
	public List<Event> getByRadius(Float latitude, Float longitude, Float radius) throws EventDBException {
		return getByArea(latitude - radius, latitude + radius, longitude - radius, longitude + radius);
	}

	/**
	 * Find an event given its id
	 * 
	 * @param id
	 *            id of the event
	 * @return the found event if it exists, or null
	 * @throws EventDBException
	 * @throws NoEventFoundException
	 */
	public Event getById(int id) throws NoEventFoundException, EventDBException {
		System.out.println("[DB] Getting event by id " + id);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT * FROM gsx95369n3oh2zo6.events WHERE ID = ?", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setInt(1, id);
			ResultSet result = getStmt.executeQuery();
			System.out.println("[DB] Query executed");
			Event temp = null;
			if (result.next()) {
				temp = new Event();
				temp.setId(result.getInt(1));
				temp.setDate(result.getDate(2));
				temp.setEventType(EventType.valueOf(result.getString(3)));
				temp.setDescription(result.getString(4));
				temp.setCountry(result.getString(5));
				temp.setCity(result.getString(6));
				temp.setStreet(result.getString(7));
				temp.setLatitude(result.getFloat(8));
				temp.setLongitude(result.getFloat(9));
				temp.setSubmitterId(result.getInt(10));

				try {
					System.out.println("[DB] Finding votes");
					int votes = getVotes(temp.getId());
					temp.setVotes(votes);
					System.out.println("[DB] Found " + votes);
				} catch (VotesDBException e) {
					System.out.println("[DB] No votes found");
					temp.setVotes(0);
				}

				return temp;
			} else {
				System.out.println("[DB] No event found");
				throw new NoEventFoundException();
			}

		} catch (URISyntaxException | SQLException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in EventStorage.getById()");
			System.out.println(e.getMessage());
			throw new EventDBException("ERROR in finding event by id", e);
		}
	}

	public int getSubmitter(int id) throws EventDBException, NoEventFoundException {
		System.out.println("[DB] Getting submitter by event id " + id);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT SUBMITTERID FROM gsx95369n3oh2zo6.events WHERE ID = ?", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setInt(1, id);
			ResultSet result = getStmt.executeQuery();
			System.out.println("[DB] Query executed");
			if (result.next()) {
				System.out.println("[DB] Event found");
				return result.getInt(1);
			} else {
				System.out.println("[DB] No event found");
				throw new NoEventFoundException();

			}
		} catch (URISyntaxException | SQLException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in EventStorage.getSubmitter()");
			System.out.println(e.getMessage());
			throw new EventDBException("ERROR when finding submitter by event id", e);
		}
	}

	public List<Event> getByUser(int id) throws EventDBException {
		System.out.println("[DB] Getting event by user "+id);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT * FROM gsx95369n3oh2zo6.events WHERE SUBMITTERID = ?", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setInt(1, id);
			ResultSet result = getStmt.executeQuery();
			System.out.println("[DB] Query executed");
			List<Event> events = new ArrayList<Event>();
			while (result.next()) {
				Event temp = new Event();
				temp.setId(result.getInt(1));
				temp.setDate(result.getDate(2));
				temp.setEventType(EventType.valueOf(result.getString(3)));
				temp.setDescription(result.getString(4));
				temp.setCountry(result.getString(5));
				temp.setCity(result.getString(6));
				temp.setStreet(result.getString(7));
				temp.setLatitude(result.getFloat(8));
				temp.setLongitude(result.getFloat(9));
				temp.setSubmitterId(result.getInt(10));
				
				try {
					System.out.println("[DB] Finding votes");
					int votes = getVotes(temp.getId());
					temp.setVotes(votes);
					System.out.println("[DB] Found " + votes);
				} catch (VotesDBException e) {
					System.out.println("[DB] No votes found");
					temp.setVotes(0);
				}
				
				events.add(temp);
			}
			System.out.println("[DB] Found "+events.size()+" events");
			return events;
		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("[DB] EXCEPTION in EventStorage.getByUser()");
			System.out.println(e.getMessage());
			throw new EventDBException("ERROR when finding events by submitter id", e);
		}
	}

	public boolean vote(int userid, int eventid) throws VotesDBException, NoVoteCreatedException {
		System.out.println("[DB] Voting by "+userid+" to "+eventid);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement createStmt = connection.prepareStatement(
					"INSERT INTO gsx95369n3oh2zo6.votes (USERID, EVENTID) VALUES (?,?)",
					Statement.RETURN_GENERATED_KEYS);
			createStmt.clearParameters();
			createStmt.setInt(1, userid);
			createStmt.setInt(2, eventid);
			int result = createStmt.executeUpdate();
			System.out.println("[DB] Update executed");
			if (result > 0) {
				System.out.println("[DB] Vote created");
				return true;
			} else {
				System.out.println("[DB] Vote not created");
				throw new NoVoteCreatedException();
			}
		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("[DB] EXCEPTION in EventStorage.vote()");
			System.out.println(e.getMessage());
			throw new VotesDBException("ERROR when creating vote", e);
		}

	}

	public boolean unvote(int userid, int eventid) throws VotesDBException {
		System.out.println("[DB] Removing vote by "+userid+" from "+eventid);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement delStmt = connection.prepareStatement(
					"DELETE FROM gsx95369n3oh2zo6.votes WHERE USERID = ? AND EVENTID = ?",
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			delStmt.clearParameters();
			delStmt.setInt(1, userid);
			delStmt.setInt(2, eventid);
			int count = delStmt.executeUpdate();
			System.out.println("[DB] Update executed");
			if (count > 0) {
				System.out.println("[DB] Vote removed");
				return true;
			} else {
				System.out.println("[DB] Vote already removed");
				return false;
			}
		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("[DB] EXCEPTION in EventStorage.unvote()");
			System.out.println(e.getMessage());
			throw new VotesDBException("ERROR when deleting vote", e);
		}

	}

	public int getVotes(int eventId) throws VotesDBException {
		System.out.println("[DB] Getting votes for event "+eventId);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT COUNT(*) FROM gsx95369n3oh2zo6.votes WHERE EVENTID = ?", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setInt(1, eventId);
			ResultSet results = getStmt.executeQuery();
			System.out.println("[DB] Query executed");
			if (results.next()){
				return results.getInt(1);
			} else {
				return 0;
			}
		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("[DB] EXCEPTION in EventStorage.getVotes()");
			System.out.println(e.getMessage());
			throw new VotesDBException("EROR when finding vote", e);
		}
	}
}
