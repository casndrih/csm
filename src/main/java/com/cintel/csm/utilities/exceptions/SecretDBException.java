package com.cintel.csm.utilities.exceptions;

public class SecretDBException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4670698978035028971L;

	public SecretDBException(String string, Exception e) {
		super(string, e);
	}
	
}
