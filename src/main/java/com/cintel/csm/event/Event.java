package com.cintel.csm.event;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import com.cintel.csm.utilities.ProjectConstants;

/**
 * Class representing an event
 * 
 * @author Simone Ripamonti
 *
 */
@XmlRootElement
public class Event {

	/**
	 * unique id of the event
	 */
	private int id;
	/**
	 * creation date
	 */
	private Date date;
	/**
	 * type of the event
	 */
	private EventType eventType;
	/**
	 * text description of the event
	 */
	private String description;
	/**
	 * country of the event
	 */
	private String country;
	/**
	 * city of the event
	 */
	private String city;
	/**
	 * street of the event
	 */
	private String street;
	/**
	 * latitude of the event
	 */
	private Float latitude;
	/**
	 * longitude of the event
	 */
	private Float longitude;
	/**
	 * number of votes
	 */
	private int votes;
	/**
	 * foreign key, user that submitted the event
	 */
	private int submitterId;
	/**
	 * hypermedia: url of this event
	 */
	private String eventUrl;
	/**
	 * hypermedia: url of the submitter
	 */
	private String submitterUrl;

	public Event() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
		this.eventUrl = ProjectConstants.USERS_BASE_URL + "/" + String.valueOf(id);
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public EventType getEventType() {
		return eventType;
	}

	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public Float getLatitude() {
		return latitude;
	}

	public void setLatitude(Float latitude) {
		this.latitude = latitude;
	}

	public Float getLongitude() {
		return longitude;
	}

	public void setLongitude(Float longitude) {
		this.longitude = longitude;
	}

	public int getSubmitterId() {
		return submitterId;
	}

	public void setSubmitterId(int submitterId) {
		this.submitterId = submitterId;
		this.submitterUrl = ProjectConstants.USERS_BASE_URL + "/" + String.valueOf(submitterId);
	}

	public String getEventUrl() {
		return eventUrl;
	}

	public void setEventUrl(String eventUrl) {
		this.eventUrl = eventUrl;
	}

	public String getSubmitterUrl() {
		return submitterUrl;
	}

	public void setSubmitterUrl(String submitterUrl) {
		this.submitterUrl = submitterUrl;
	}

	public int getVotes() {
		return votes;
	}

	public void setVotes(int votes) {
		this.votes = votes;
	}

}
