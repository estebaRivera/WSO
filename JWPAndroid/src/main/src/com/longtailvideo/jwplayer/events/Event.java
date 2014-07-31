package com.longtailvideo.jwplayer.events;

/**
 * The most basic type of event.
 * 
 * @author tom
 * 
 */
public class Event {
	// The type of event
	private final String mType;

	/**
	 * @param type
	 *            The event type. Should be one of the final static strings in
	 *            the used event subclass.
	 */
	public Event(String type) {
		mType = type;
	}

	/**
	 * @return The event type.
	 */
	public String getType() {
		return mType;
	}

	/**
	 * Build the data part of the string representation.
	 * 
	 * @param builder
	 *            The string builder to which to add event data information.
	 */
	protected void appendDataString(StringBuilder builder) {
		// Plain events have no data
	}

	@Override
	public String toString() {
		// Example: [event]
		StringBuilder builder = new StringBuilder("[").append(mType);
		appendDataString(builder);
		return builder.append("]").toString();
	}
}
