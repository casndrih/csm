package com.cintel.csm.authentication;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import com.cintel.csm.utilities.ProjectConstants;
import com.cintel.csm.utilities.database.Database;
import com.cintel.csm.utilities.exceptions.AuthorizationDBException;
import com.cintel.csm.utilities.exceptions.NoTokenCreatedException;
import com.cintel.csm.utilities.exceptions.NoUserFoundException;
import com.cintel.csm.utilities.exceptions.SecretDBException;

/**
 * Authentication related methods
 * 
 * @author Simone Ripamonti
 *
 */
public class Authenticator {
	/**
	 * Retrieves user id given the token
	 * 
	 * @param authToken
	 * @return the user is
	 * @throws AuthorizationDBException
	 * @throws NoUserFoundException
	 *             if the authToken is of no user
	 */
	public static int getUserId(String authToken) throws AuthorizationDBException, NoUserFoundException {
		System.out.println("[DB] getting user id with token " + authToken);
		try (Connection connection = Database.getConnection()) {
			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT ID FROM gsx95369n3oh2zo6.authorization WHERE TOKEN = ? AND ISVALID = 1",
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setString(1, authToken);
			ResultSet result = getStmt.executeQuery();
			System.out.println("[DB] Query executed");
			if (result.next()) {
				System.out.println("[DB] Id found");
				return result.getInt(1);
			} else {
				// no match
				System.out.println("[DB] Id not found");
				throw new NoUserFoundException();
			}

		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("[DB] EXCEPTION in Authenicator.getUserId()");
			System.out.println(e.getMessage());
			throw new AuthorizationDBException("ERROR when finding user id from token", e);
		}
	}

	/**
	 * Check if an id is superuser
	 * 
	 * @param id
	 *            of the user
	 * @return the status
	 * @throws NoUserFoundException
	 *             if the id is of no user
	 * @throws AuthorizationDBException
	 */
	public static boolean isSuperuser(int id) throws NoUserFoundException, AuthorizationDBException {
		System.out.println("[DB] Checking if " + id + " is superuser");
		try (Connection connection = Database.getConnection()) {

			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT SUPERUSER FROM gsx95369n3oh2zo6.authorization WHERE ID = ?",
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setInt(1, id);
			ResultSet result = getStmt.executeQuery();
			System.out.println("[DB] Query executed");
			if (result.next()) {
				System.out.println("[DB] User found");
				if (result.getInt(1) > 0) {
					return true;
				} else {
					return false;
				}
			} else {
				System.out.println("[DB] User not found");
				throw new NoUserFoundException();
			}
		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("EXCEPTION in Authenticator.isSuperuser()");
			System.out.println(e.getMessage());
			throw new AuthorizationDBException("ERROR when finding if user is superuser", e);
		}
	}

	/**
	 * Checks if a username-password pair match a valid user id
	 * 
	 * @param username
	 * @param password
	 * @return the id of the user, if there's a match
	 * @throws NoUserFoundException
	 *             if no match
	 * @throws SecretDBException
	 */
	public static int checkPassword(String username, String password) throws NoUserFoundException, SecretDBException {
		System.out.println("[DB] Checking password for " + username);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT gsx95369n3oh2zo6.users.ID FROM gsx95369n3oh2zo6.users JOIN gsx95369n3oh2zo6.secret WHERE USERNAME = ? AND PASSWORD = ?",
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setString(1, username);
			getStmt.setString(2, password);
			ResultSet result = getStmt.executeQuery();
			System.out.println("[DB] Query executed");
			if (result.next()) {
				System.out.println("[DB] Password ok");
				return result.getInt(1);
			} else {
				// wrong pair
				System.out.println("[DB] No match");
				throw new NoUserFoundException();
			}
		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("EXCEPTION in Authenticator.checkPassword()");
			System.out.println(e.getMessage());
			throw new SecretDBException("ERROR when checking username and password", e);
		}
	}

	/**
	 * Generates a new token for the given user id
	 * 
	 * @param id
	 *            the user id
	 * @return the generated token
	 * @throws AuthorizationDBException
	 * @throws NoTokenCreatedException
	 */
	public static String generateToken(int id) throws AuthorizationDBException, NoTokenCreatedException {
		System.out.println("[DB] Generating token for " + id);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement updStmt = connection
					.prepareStatement("UPDATE gsx95369n3oh2zo6.authorization SET TOKEN = ?, ISVALID = 1 WHERE ID = ?");
			String token = UUID.randomUUID().toString();
			updStmt.setString(1, token);
			updStmt.setInt(2, id);
			int result = updStmt.executeUpdate();

			if (result > 0) {
				System.out.println("[DB] Token created");
				return token;
			} else {
				System.out.println("[DB] No token created");
				throw new NoTokenCreatedException();
			}
		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("EXCEPTION in Authenticator.genereateToken()");
			System.out.println(e.getMessage());
			throw new AuthorizationDBException("ERROR when creating token", e);
		}

	}

	/**
	 * Invalidates the token of the user
	 * 
	 * @param id
	 *            of the user
	 * @return true if all is ok
	 * @throws AuthorizationDBException
	 */
	public static boolean invalidateToken(int id) throws AuthorizationDBException {
		System.out.println("[DB] Invalidating token for " + id);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement updStmt = connection
					.prepareStatement("UPDATE gsx95369n3oh2zo6.authorization SET ISVALID = 0 WHERE ID = ?");
			updStmt.setInt(1, id);
			int result = updStmt.executeUpdate();
			if (result > 0) {
				System.out.println("[DB] Token invalidated");
				return true;
			} else {
				System.out.println("[DB] Token was already invalid?");
				return false;
			}
		} catch (ClassNotFoundException | URISyntaxException | SQLException e) {
			System.out.println("[DB] EXCEPTION in Authenticator.invalidateToken()");
			System.out.println(e.getMessage());
			throw new AuthorizationDBException("ERROR when invalidating token", e);
		}
	}

	/**
	 * Checks if the provided service key is valid
	 * 
	 * @param serviceKey
	 * @return true if valid, otherwise false
	 */
	public static boolean isServiceKeyValid(String serviceKey) {
		if (ProjectConstants.SERVICE_KEYS.contains(serviceKey))
			return true;
		return false;
	}

	/**
	 * Checks if the provided authorization token is valid
	 * 
	 * @param authToken
	 * @return true if valid, otherwise false
	 */
	public static boolean isAuthTokenValid(String authToken) {
		try {
			getUserId(authToken);
			return true;
		} catch (AuthorizationDBException | NoUserFoundException e) {
			return false;
		}
	}
}
