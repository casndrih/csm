package com.cintel.csm.user;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import com.cintel.csm.utilities.ProjectConstants;

@XmlRootElement
public class User {
	/**
	 * unique user id
	 */
	int id;
	/**
	 * unique username
	 */
	String username;
	/**
	 * unique user email
	 */
	String email;
	/**
	 * date of creation
	 */
	Date created;
	/**
	 * hypermedia: url of the user
	 */
	String userUrl;

	public User() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
		this.userUrl = ProjectConstants.USERS_BASE_URL + "/" + String.valueOf(id);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getUserUrl() {
		return userUrl;
	}

	public void setUserUrl(String userUrl) {
		this.userUrl = userUrl;
	}

}
