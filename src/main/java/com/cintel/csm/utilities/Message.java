package com.cintel.csm.utilities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Message {
	private String argument;
	private String message;
	
	public Message(){
		
	}
	
	public Message(String argument, String message){
		this.argument = argument;
		this.message = message;
	}

	public String getArgument() {
		return argument;
	}

	public void setArgument(String argument) {
		this.argument = argument;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	

}
