package com.longtailvideo.jwplayer.media;

import android.util.Log;
import android.view.SurfaceHolder;

import com.longtailvideo.jwplayer.JWPlayer;
import com.longtailvideo.jwplayer.JWPlayer.PlayerState;
import com.longtailvideo.jwplayer.events.Event;
import com.longtailvideo.jwplayer.events.EventDispatcher;
import com.longtailvideo.jwplayer.events.EventListener;
import com.longtailvideo.jwplayer.events.MediaEvent;
import com.longtailvideo.jwplayer.events.PlayerStateEvent;
import com.longtailvideo.jwplayer.media.adaptive.QualityManager;
import com.longtailvideo.jwplayer.model.Model;

/**
 * A provider of a media player for a specified media type.
 * 
 * The MediaProvider superclass provides an empty media player.
 * 
 * Events:
 * 
 * playerState: When the media changes state.
 * 
 * mediaLoaded: When a new media URL is loaded.
 * 
 * mediaSeek: When a seek request is received.
 * 
 * mediaError: When an error has occurred.
 * 
 * qualityChange: When the quality of the current stream changed.
 * 
 * qualityLevels: When the set of quality levels of the stream is set.
 * 
 * @author tom
 */
public class MediaProvider extends EventDispatcher {
	private static final String ERROR_TAG = "JWPlayerError";

	/**
	 * The player of which this media provider is a part.
	 */
	protected JWPlayer mPlayer;

	/**
	 * The model to which this provider belongs.
	 */
	protected Model mModel;

	// The current player state
	private JWPlayer.PlayerState mState;

	/**
	 * The current quality manager.
	 */
	protected QualityManager mQualityManager;

	/**
	 * Whether a load call has been done.
	 */
	protected boolean mLoaded;

	/**
	 * @param player
	 *            The player of which this media provider is a part.
	 * @param model
	 *            The model to which this provider belongs.
	 */
	public MediaProvider(JWPlayer player, Model model) {
		mState = JWPlayer.PlayerState.IDLE;
		mPlayer = player;
		mModel = model;
		mLoaded = false;
		setupQualityManager();
	}

	/**
	 * Load the media at the given URL.
	 * 
	 * @param url
	 *            The URL pointing to the media item to load.
	 */
	public void load(String url) {
		dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_LOADED));
		mLoaded = true;
	}

	/**
	 * Start playing the currently selected media.
	 */
	public void play() {
		if (getState() != JWPlayer.PlayerState.PLAYING) {
			// The player state is updated after the first position change
			setState(JWPlayer.PlayerState.PLAYING);
		}
	}

	/**
	 * Pause the media if it is playing.
	 */
	public void pause() {
		if (getState() != JWPlayer.PlayerState.PAUSED) {
			setState(JWPlayer.PlayerState.PAUSED);
		}
	}

	/**
	 * Seek the media to the specified position.
	 * 
	 * @param position
	 *            The target position in milliseconds from the start.
	 */
	public void seek(int position) {
		fireSeekEvent(position);
	}

	/**
	 * Fire the seek event.
	 * 
	 * @param position
	 *            The requested target position.
	 */
	protected void fireSeekEvent(int position) {
		MediaEvent evt = new MediaEvent(MediaEvent.JWPLAYER_MEDIA_SEEK);
		evt.setPosition(getPosition());
		evt.setOffset(position);
		dispatchEvent(evt);
	}

	/**
	 * Stop the media if it is playing.
	 */
	public void stop() {
		if (getState() != JWPlayer.PlayerState.IDLE) {
			setState(JWPlayer.PlayerState.IDLE);
		}
		mLoaded = false;
	}

	/**
	 * @return The current position in milliseconds, or 0 if it is not
	 *         available.
	 */
	@SuppressWarnings("static-method")
	protected int getPosition() {
		return 0;
	}

	/**
	 * @return The duration of the media in milliseconds, or 0 if it is not
	 *         available.
	 */
	@SuppressWarnings("static-method")
	protected int getDuration() {
		return 0;
	}

	/**
	 * @return Whether the current stream is seekable.
	 */
	@SuppressWarnings("static-method")
	public boolean canSeek() {
		return false;
	}

	/**
	 * @return Whether the current stream is a live stream.
	 */
	@SuppressWarnings("static-method")
	public boolean isLive() {
		return false;
	}

	/**
	 * @return the current state of the media.
	 */
	public JWPlayer.PlayerState getState() {
		return mState;
	}

	/**
	 * Change the state of the media. Dispatches the playerState event.
	 * 
	 * @param state
	 *            The new media state.
	 */
	protected void setState(JWPlayer.PlayerState state) {
		PlayerStateEvent evt = new PlayerStateEvent(
				PlayerStateEvent.JWPLAYER_PLAYER_STATE, state, mState);
		mState = state;
		dispatchEvent(evt);
	}

	/**
	 * @param holder
	 *            The SurfaceHolder holding the surface to which to render.
	 */
	public void setDisplay(SurfaceHolder holder) {
		// By default, do not render at all
	}

	/**
	 * Dispatch an error event and log the error. This should only be used for
	 * unexpected errors. For more common errors, a more user-readable string is
	 * desired.
	 * 
	 * @param message
	 *            A message describing the error. Returned in the error event.
	 * @param e
	 *            The exception describing the error. This exception is logged.
	 */
	protected void error(String message, Exception e) {
		error(message, "An unexpected error occurred: ["
				+ e.getClass().getSimpleName() + "] " + e.getMessage());
	}

	/**
	 * Dispatch an error event and log the error.
	 * 
	 * @param message
	 *            A message describing the error. Returned in the error event.
	 * @param detail
	 *            A more detailed error message. This message is only logged.
	 */
	protected synchronized void error(String message, String detail) {
		Log.e(ERROR_TAG, detail);

		// Generate and print a stack trace
		StackTraceElement[] trace;
		try {
			throw new Exception();
		}
		catch (Exception e) {
			trace = e.getStackTrace();
		}
		Log.e(ERROR_TAG, "Stack trace:");
		int idx;
		for (idx = 0; idx < trace.length; ++idx) {
			if (!MediaProvider.class.getName()
					.equals(trace[idx].getClassName())
					|| !"error".equals(trace[idx].getMethodName())) {
				break;
			}
		}
		for (; idx < trace.length; ++idx) {
			Log.e(ERROR_TAG,
					"\t" + trace[idx].getClassName() + "."
							+ trace[idx].getMethodName() + "("
							+ trace[idx].getFileName() + ":"
							+ trace[idx].getLineNumber() + ")");
		}

		stop();
		dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_ERROR, message));
	}

	/**
	 * Update the video dimensions.
	 * 
	 * @param width
	 *            the new video width, or 0 if unknown.
	 * @param height
	 *            the new video height, or 0 if unknown.
	 */
	protected void setDimensions(int width, int height) {
		MediaEvent evt = new MediaEvent(MediaEvent.JWPLAYER_MEDIA_META);
		evt.setMetadata(new JWPlayer.Metadata.Dimensions(width, height));
		dispatchEvent(evt);
	}

	/**
	 * Release resources used by the player.
	 */
	public void release() {
		stop();
		// Nothing to release
	}

	/**
	 * Initialize the quality manager and attach all required listeners.
	 */
	protected void setupQualityManager() {
		mQualityManager = new QualityManager();
		mQualityManager.addGlobalEventListener(new EventListener<Event>() {
			@Override
			public void handle(Event event) {
				MediaProvider.this.dispatchEvent(event);
			}
		});
	}

	/**
	 * @return The quality manager.
	 */
	public QualityManager getQualityManager() {
		return mQualityManager;
	}

	/**
	 * Trigger the buffering state. Will automatically resolve itself.
	 */
	public void triggerBuffer() {
		if (getState() == PlayerState.PLAYING) {
			setState(JWPlayer.PlayerState.BUFFERING);
		}
	}
}
