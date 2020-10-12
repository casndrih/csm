package com.cintel.csm.utilities.exceptions;

public class UserDBException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5855213611291344890L;

	public UserDBException(String string, Exception e) {
		super(string, e);
	}

}
