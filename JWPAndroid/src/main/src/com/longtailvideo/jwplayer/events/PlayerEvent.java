package com.longtailvideo.jwplayer.events;

/**
 * A generic event for the player.
 * 
 * @author tom
 */
public class PlayerEvent extends Event {
	/**
	 * The following properties should be defined for the playerError event:
	 * 
	 * message: The error message.
	 */
	public static final String JWPLAYER_ERROR = "jwplayerError";

	private String mMessage;

	/**
	 * @param type
	 *            The type of player event. Should be one of the static final
	 *            Strings in the class definition of this event class or one of
	 *            its descendant classes.
	 * @param message
	 *            The message describing the event.
	 */
	public PlayerEvent(String type, String message) {
		super(type);
		mMessage = message;
	}

	/**
	 * @param type
	 *            The type of player event. Should be one of the static final
	 *            Strings in the class definition of this event class or one of
	 *            its descendant classes.
	 */
	public PlayerEvent(String type) {
		this(type, null);
	}

	/**
	 * @return The message describing the event.
	 */
	public String getMessage() {
		return mMessage;
	}

	/**
	 * @param message
	 *            The message describing the event.
	 */
	public void setMessage(String message) {
		mMessage = message;
	}

	@Override
	protected void appendDataString(StringBuilder builder) {
		super.appendDataString(builder);
		if (mMessage != null) {
			builder.append(" \"").append(mMessage).append("\"");
		}
	}
}
