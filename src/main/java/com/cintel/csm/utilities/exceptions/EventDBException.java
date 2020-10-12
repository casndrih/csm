package com.cintel.csm.utilities.exceptions;

public class EventDBException extends Exception {

	public EventDBException(String string) {
		super(string);
	}
	
	public EventDBException(String string, Throwable cause){
		super(string, cause);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3133246010133922611L;

}
