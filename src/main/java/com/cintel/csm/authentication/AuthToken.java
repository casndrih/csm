package com.cintel.csm.authentication;

import javax.xml.bind.annotation.XmlRootElement;

import com.cintel.csm.utilities.ProjectConstants;

/**
 * Representation of an authorization token, used when returning the token after
 * a request
 * 
 * @author Simone Ripamonti
 *
 */
@XmlRootElement
public class AuthToken {
	/**
	 * the token
	 */
	private String authToken;
	/**
	 * the user id
	 */
	private int userId;
	/**
	 * the user name
	 */
	private String username;
	/**
	 * the user url
	 */
	private String userUrl;

	public AuthToken() {

	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
		this.userUrl = ProjectConstants.USERS_BASE_URL + "/" + userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUserUrl() {
		return userUrl;
	}

	public void setUserUrl(String userUrl) {
		this.userUrl = userUrl;
	}

}
