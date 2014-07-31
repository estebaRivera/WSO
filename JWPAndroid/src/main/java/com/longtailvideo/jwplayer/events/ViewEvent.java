package com.longtailvideo.jwplayer.events;

/**
 * An event related to the view.
 * 
 * @author tom
 */
public class ViewEvent extends PlayerEvent {
	/**
	 * The following properties should be defined for the playerError event:
	 * 
	 * enabled: Whether the view property was enabled.
	 */
	public static String JWPLAYER_VIEW_FULLSCREEN = "jwplayerViewFullscreen";

	private Boolean mEnabled = null;

	/**
	 * @param type
	 *            The event type.
	 */
	public ViewEvent(String type) {
		super(type);
	}

	/**
	 * @param type
	 *            The event type.
	 * @param enabled
	 *            Whether the property was enabled.
	 */
	public ViewEvent(String type, boolean enabled) {
		this(type);
		mEnabled = Boolean.valueOf(enabled);
	}

	/**
	 * @return Whether the property was enabled.
	 */
	public boolean isEnabled() {
		return mEnabled == null ? true : mEnabled.booleanValue();
	}

	@Override
	protected void appendDataString(StringBuilder builder) {
		super.appendDataString(builder);
		if (mEnabled != null) {
			builder.append(" enabled=").append(mEnabled);
		}
	}
}
