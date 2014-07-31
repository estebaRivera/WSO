package com.longtailvideo.jwplayer.events;

import com.longtailvideo.jwplayer.JWPlayer;

/**
 * An event related to media playback.
 * 
 * @author tom
 */
public class MediaEvent extends PlayerEvent {
	/**
	 * The following properties should be defined for the mediaBuffer event:
	 * 
	 * bufferPercent: The percentage of the video that exists in the buffer.
	 */
	public static final String JWPLAYER_MEDIA_BUFFER = "jwplayerMediaBuffer";
	/**
	 * The mediaBufferFull event requires no properties.
	 */
	public static final String JWPLAYER_MEDIA_BUFFER_FULL = "jwplayerMediaBufferFull";
	/**
	 * The mediaComplete event requires no properties.
	 */
	public static final String JWPLAYER_MEDIA_COMPLETE = "jwplayerMediaComplete";
	/**
	 * The following properties should be defined for the mediaError event:
	 * 
	 * message: The error message.
	 */
	public static final String JWPLAYER_MEDIA_ERROR = "jwplayerMediaError";
	/**
	 * The mediaLoaded event requires no properties.
	 */
	public static final String JWPLAYER_MEDIA_LOADED = "jwplayerMediaLoaded";
	/**
	 * The following properties should be defined for the mediaSeek event:
	 * 
	 * position: The position of the stream in milliseconds
	 * 
	 * offset: The requested target position in milliseconds
	 */
	public static final String JWPLAYER_MEDIA_SEEK = "jwplayerMediaSeek";
	/**
	 * The following properties should be defined for the mediaTime event:
	 * 
	 * position: The position of the stream in milliseconds
	 * 
	 * duration: The duration of the stream in milliseconds
	 */
	public static final String JWPLAYER_MEDIA_TIME = "jwplayerMediaTime";
	/**
	 * The following properties should be defined for the mediaMeta event:
	 * 
	 * metadata: The changed metadata
	 */
	public static final String JWPLAYER_MEDIA_META = "jwplayerMediaMeta";
	/**
	 * The following properties should be defined for the mediaSelected event:
	 * 
	 * url: The selected URL
	 */
	public static final String JWPLAYER_MEDIA_SELECTED = "jwplayerMediaSelected";

	private int mBufferPercent = -1;
	private int mDuration = -1;
	private int mOffset = -1;
	private int mPosition = -1;
	private JWPlayer.Metadata mMetadata = null;
	private String mUrl = null;

	/**
	 * @param type
	 *            The type of media event. Should be one of the static final
	 *            Strings in the class definition of this event class or one of
	 *            its descendant classes.
	 * @param message
	 *            The message describing the event.
	 */
	public MediaEvent(String type, String message) {
		super(type, message);
	}

	/**
	 * @param type
	 *            The type of media event. Should be one of the static final
	 *            Strings in the class definition of this event class or one of
	 *            its descendant classes.
	 */
	public MediaEvent(String type) {
		super(type);
	}

	/**
	 * @return The percentage of the video that is currently buffered.
	 */
	public int getBufferPercent() {
		return mBufferPercent;
	}

	/**
	 * @param bufferPercent
	 *            The percentage of the video that is currently buffered.
	 */
	public void setBufferPercent(int bufferPercent) {
		mBufferPercent = bufferPercent;
	}

	/**
	 * @return The duration in milliseconds.
	 */
	public int getDuration() {
		return mDuration;
	}

	/**
	 * @param duration
	 *            The duration in milliseconds.
	 */
	public void setDuration(int duration) {
		mDuration = duration;
	}

	/**
	 * @return The offset in milliseconds.
	 */
	public int getOffset() {
		return mOffset;
	}

	/**
	 * @param offset
	 *            The offset in milliseconds.
	 */
	public void setOffset(int offset) {
		mOffset = offset;
	}

	/**
	 * @return The playback position in milliseconds.
	 */
	public int getPosition() {
		return mPosition;
	}

	/**
	 * @param position
	 *            The playback position in milliseconds.
	 */
	public void setPosition(int position) {
		mPosition = position;
	}

	/**
	 * @param metadata
	 *            The piece of metadata which was changed.
	 */
	public void setMetadata(JWPlayer.Metadata metadata) {
		mMetadata = metadata;
	}

	/**
	 * @return The piece of metadata which was changed.
	 */
	public JWPlayer.Metadata getMetadata() {
		return mMetadata;
	}

	/**
	 * @return The media URL.
	 */
	public String getUrl() {
		return mUrl;
	}

	/**
	 * @param url
	 *            The media URL.
	 */
	public void setUrl(String url) {
		mUrl = url;
	}

	@Override
	protected void appendDataString(StringBuilder builder) {
		super.appendDataString(builder);
		if (mBufferPercent != -1) {
			builder.append(" bufferPercent=").append(mBufferPercent);
		}
		if (mDuration != -1) {
			builder.append(" duration=").append(mDuration);
		}
		if (mOffset != -1) {
			builder.append(" offset=").append(mOffset);
		}
		if (mPosition != -1) {
			builder.append(" position=").append(mPosition);
		}
		if (mMetadata != null) {
			builder.append(" metadata=").append(mMetadata);
		}
		if (mUrl != null) {
			builder.append(" url=").append(mUrl);
		}
	}

	/**
	 * The set of error messages for the error events.
	 * 
	 * @author tom
	 */
	public static class Error {
		/**
		 * Returned when a file could not be loaded over the network.
		 */
		public static final String ERROR_FILE_NOT_FOUND = "File not found";
		/**
		 * Returned when a playback is not possible.
		 */
		public static final String ERROR_FILE_COULD_NOT_BE_PLAYED = "File could not be played";
		/**
		 * Returned when no chosen streams can be played.
		 */
		public static final String ERROR_NO_PLAYABLE_STREAMS = "No playable streams";
	}
}
