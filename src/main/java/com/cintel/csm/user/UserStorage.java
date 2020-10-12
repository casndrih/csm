package com.cintel.csm.user;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.cintel.csm.utilities.database.Database;
import com.cintel.csm.utilities.exceptions.NoUserCreatedException;
import com.cintel.csm.utilities.exceptions.NoUserFoundException;
import com.cintel.csm.utilities.exceptions.NoUserLoginCreatedException;
import com.cintel.csm.utilities.exceptions.SecretDBException;
import com.cintel.csm.utilities.exceptions.UserDBException;

public enum UserStorage {
	instance;

	public int addWithPassword(User u, String password) throws UserDBException, NoUserCreatedException {
		int id = createUser(u);
		if (id > 0) {
			try {
				createLogin(id, password);
			} catch (NoUserLoginCreatedException | SecretDBException e) {
				// need to rollback and throw exception
				remove(id);
				throw new NoUserCreatedException();
			}
		}
		return id;
	}

	public int addWithoutPassword(User u) throws UserDBException, NoUserCreatedException {
		return createUser(u);
	}

	public User getById(int id) throws NoUserFoundException, UserDBException {
		System.out.println("[DB] Beginning search for id" + id);
		try (Connection connection = Database.getConnection()) {

			PreparedStatement getStmt = connection.prepareStatement("SELECT * FROM gsx95369n3oh2zo6.users WHERE ID = ?",
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setInt(1, id);
			ResultSet results = getStmt.executeQuery();
			System.out.println("[DB] Query executed for " + id);
			User u = null;
			if (results.next()) {
				u = new User();
				u.setId(results.getInt(1));
				u.setUsername(results.getString(2));
				u.setEmail(results.getString(3));
				u.setCreated(results.getDate(4));
				System.out.println("[DB] User found with id " + id);
			} else {
				System.out.println("[DB] No user found with id " + id);
				throw new NoUserFoundException();
			}

			return u;
		} catch (URISyntaxException | SQLException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in UserStorage.getById()");
			System.out.println(e.getMessage());
			throw new UserDBException("ERROR when finding user by id", e);
		}
	}

	public User getByEmail(String email) throws NoUserFoundException, UserDBException {
		System.out.println("[DB] Beginning search for email" + email);
		try (Connection connection = Database.getConnection()) {
			PreparedStatement getStmt = connection.prepareStatement(
					"SELECT * FROM gsx95369n3oh2zo6.users WHERE EMAIL = ?", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			getStmt.clearParameters();
			getStmt.setString(1, email);
			System.out.println("[DB] Query executed for " + email);
			ResultSet results = getStmt.executeQuery();
			User u = null;
			if (results.next()) {
				u = new User();
				u.setId(results.getInt(1));
				u.setUsername(results.getString(2));
				u.setEmail(results.getString(3));
				u.setCreated(results.getDate(4));
				System.out.println("[DB] User found with email " + email);
				return u;
			} else {
				System.out.println("[DB] No user found with email " + email);
				throw new NoUserFoundException();
			}
		} catch (URISyntaxException | SQLException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in UserStorage.getByEmail()");
			System.out.println(e.getMessage());
			throw new UserDBException("ERROR when finding user by email", e);
		}
	}

	public boolean remove(int id) throws UserDBException {
		System.out.println("[DB] Beginning deletion of id " + id);
		try (Connection connection = Database.getConnection()) {
			PreparedStatement delStmt = connection.prepareStatement("DELETE FROM gsx95369n3oh2zo6.users WHERE ID = ?",
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			delStmt.clearParameters();
			delStmt.setInt(1, id);
			int result = delStmt.executeUpdate();
			System.out.println("[DB] Update executed for " + id + ", affected rows " + result);
			if (result > 0) {
				return true;
			} else {
				return false;
			}
		} catch (URISyntaxException | SQLException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in UserStorage.remove()");
			System.out.println(e.getMessage());
			throw new UserDBException("ERROR when deleting user", e);
		}
	}

	private int createUser(User u) throws UserDBException, NoUserCreatedException {
		System.out.println("[DB] Beginning user creation " + u);
		try (Connection connection = Database.getConnection()) {
			PreparedStatement createStmt = connection.prepareStatement(
					"INSERT INTO gsx95369n3oh2zo6.users (USERNAME, EMAIL, CREATED) VALUES (?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			createStmt.clearParameters();
			createStmt.setString(1, u.getUsername());
			createStmt.setString(2, u.getEmail());
			createStmt.setDate(3, new Date(System.currentTimeMillis()));
			createStmt.executeUpdate();
			System.out.println("[DB] Update executed for " + u);
			ResultSet results = createStmt.getGeneratedKeys();
			int id = -1;
			if (results.next()) {
				id = results.getInt(1);
				System.out.println("[DB] User " + u + " id is " + id);
			} else {
				throw new NoUserCreatedException();
			}

			System.out.println("[DB] Beginning user authorization creation for " + u);
			PreparedStatement createStmt2 = connection.prepareStatement(
					"INSERT INTO gsx95369n3oh2zo6.authorization (ID, SUPERUSER, TOKEN, ISVALID) VALUES (?,?,?,?)");
			createStmt2.clearParameters();
			createStmt2.setInt(1, id);
			createStmt2.setInt(2, 0);
			createStmt2.setString(3, "");
			createStmt2.setInt(4, 0);
			createStmt2.executeUpdate();
			System.out.println("[DB] Completed creation of " + u);
			return id;
		} catch (URISyntaxException | SQLException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in UserStorage.createUser()");
			System.out.println(e.getMessage());
			throw new UserDBException("ERROR when creating user", e);
		}
	}

	private boolean createLogin(int id, String password)
			throws NoUserLoginCreatedException, UserDBException, SecretDBException {
		System.out.println("[DB] Beginning creation of credentials for id " + id);
		try (Connection connection = Database.getConnection()) {
			PreparedStatement createStmt = connection.prepareStatement(
					"INSERT INTO gsx95369n3oh2zo6.secret (ID, PASSWORD) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
			createStmt.clearParameters();
			createStmt.setInt(1, id);
			createStmt.setString(2, password);
			int result = createStmt.executeUpdate();
			System.out.println("[DB] Update executed for id " + id + ", affected rows " + result);
			if (result > 0) {
				System.out.println("[DB] Credential has been created");
				return true;
			} else {
				// no key generated!
				System.out.println("[DB] Credential hasn't been created");
				throw new NoUserLoginCreatedException();
			}
		} catch (SQLException | URISyntaxException | ClassNotFoundException e) {
			System.out.println("[DB] EXCEPTION in UserStorage.createLogin()");
			System.out.println(e.getMessage());
			throw new SecretDBException("ERROR when creating login", e);
		}
	}
}
