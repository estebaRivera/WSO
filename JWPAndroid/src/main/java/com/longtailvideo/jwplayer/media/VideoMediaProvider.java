package com.longtailvideo.jwplayer.media;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;

import com.longtailvideo.jwplayer.JWPlayer;
import com.longtailvideo.jwplayer.JWPlayer.PlayerState;
import com.longtailvideo.jwplayer.events.MediaEvent;
import com.longtailvideo.jwplayer.model.Model;
import com.longtailvideo.jwplayer.utils.PollingWorker;

/**
 * A media provider for a regular video.
 * 
 * Events: See MediaProvider.
 * 
 * mediaTime: Every +/- 100ms while the media is playing.
 * 
 * mediaBufferFull: When the media buffer is full enough to resume playback.
 * 
 * mediaBuffer: Every +/- 1s, if the buffer percentage has updated.
 * 
 * mediaComplete: When media playback has completed.
 * 
 * mediaMeta: When new metadata about the stream are available.
 * 
 * @author tom
 */
public class VideoMediaProvider extends MediaProvider implements
		MediaPlayer.OnBufferingUpdateListener,
		MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnInfoListener {
	private static final String TAG = "VideoMediaProvider";

	// The interval at which to poll the video position while playing
	private static final int POSITION_WORKER_INTERVAL = 100;
	// The max number of subsequent unchanged timestamps before state is
	// switched to buffering
	private static final int POSITION_UNCHANGED_TRIGGER_BUFFERING = 5;

	/**
	 * The internal Android media player playing back the file.
	 */
	protected MediaPlayer mMediaPlayer;

	/**
	 * Whether (and where, in milliseconds) to seek when the player gets out of
	 * the prepare state.
	 */
	protected int mSeekWhenPrepared = -1;

	/**
	 * Pause the media after buffering is complete.
	 * 
	 * Necessary for pausing during the preparation phase of the MediaPlayer
	 * object and because of an Android bug which causes the player to not
	 * resume if it is paused while buffering.
	 */
	protected boolean mPauseWhenBuffered = false;

	// The previous buffer percentage. Used for conditionally dispatching the
	// buffer event.
	private int mBufferPercent = -1;

	/**
	 * Whether to get video size updates from the video stream. Used for streams
	 * where the resolution is read from a manifest.
	 */
	protected boolean mVideoSizeFromStream = true;

	/**
	 * If set, the next "unknown" error from the media player is ignored.
	 * 
	 * Because we have little control over the media player, we use this to make
	 * sure only one error event is fired. This way, the player can be prepared
	 * while a manifest is being loaded in HLS.
	 */
	protected boolean mIgnoreNextPlayerError = false;

	/**
	 * The worker which periodically sends out the position event.
	 */
	protected PositionWorker mPositionWorker;

	// The URL which is currently loaded in the media player
	private String mUrl;

	/**
	 * The display to use for the media player May be used to queue a display
	 * change.
	 */
	protected SurfaceHolder mDisplay;

	/**
	 * Use setAndroidPlayerState to change, for easy tracing of problems.
	 */
	private AndroidPlayerState mAndroidPlayerState;

	/**
	 * The previous timestamp.
	 * 
	 * Used for finding buffering in streams that do not provide accurate
	 * buffering info.
	 */
	protected int mPrevPosition;

	/**
	 * The number of subsequent unchanged timestamps.
	 * 
	 * Used for finding buffering in streams that do not provide accurate
	 * buffering info.
	 */
	protected int mPositionUnchanged;

	/**
	 * @param player
	 *            The player of which this media provider is a part.
	 * @param model
	 *            The model to which this provider belongs.
	 */
	public VideoMediaProvider(JWPlayer player, Model model) {
		super(player, model);
		mPositionWorker = new PositionWorker();
	}

	@Override
	public void load(String url) {
		/*
		 * Pre: true
		 */
		stop();
		setupMediaPlayer();
		setDataSource(url);

		// The video is the only place we can get dimension information in this
		// media provider
		mVideoSizeFromStream = true;

		mQualityManager.setDefault();
		prepareMediaPlayer(true);
		super.load(url);
		/*
		 * Post: mMediaPlayer != null && mAndroidPlayerState ==
		 * AndroidPlayerState.PREPARING && mLoaded
		 */
	}

	/**
	 * Setup the media player. Part of the load operation.
	 */
	protected void setupMediaPlayer() {
		/*
		 * Pre: true
		 */
		if (mMediaPlayer == null
				|| isAndroidPlayerStateIn(AndroidPlayerState.END)) {
			mMediaPlayer = new MediaPlayer();
			mAndroidPlayerState = AndroidPlayerState.IDLE;
			mMediaPlayer.setDisplay(mDisplay);
			// Event listeners
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnErrorListener(this);
			mMediaPlayer.setOnInfoListener(this);
			mMediaPlayer.setOnVideoSizeChangedListener(this);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		}

		/*
		 * Post: mMediaPlayer != null && mAndroidPlayerState &=
		 * AndroidPlayerState.END
		 */
	}

	/**
	 * Set the data source for the internal media player.
	 * 
	 * @param url
	 *            The URL pointing to the video data.
	 */
	protected void setDataSource(String url) {
		/*
		 * Pre: isAndroidPlayerStateIn(AndroidPlayerState.IDLE)
		 */
		mUrl = url;
		if (!isAndroidPlayerStateIn(AndroidPlayerState.IDLE)) {
			Log.e(TAG,
					"Trying to set data source, but Android media player state is "
							+ getAndroidPlayerState());
		}
		try {
			mMediaPlayer.setDataSource(url);
			setAndroidPlayerState(AndroidPlayerState.INITIALIZED);
		}
		catch (IllegalArgumentException e) {
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
		}
		catch (SecurityException e) {
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
		}
		catch (IllegalStateException e) {
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
		}
		catch (IOException e) {
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
		}
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.INITIALIZED)
		 */
	}

	/**
	 * Start preparing the media player. Part of the load operation.
	 * 
	 * @param bufferState
	 *            Whether to enter the buffering state.
	 */
	protected void prepareMediaPlayer(boolean bufferState) {
		/*
		 * Pre: isAndroidPlayerStateIn(AndroidPlayerState.INITIALIZED)
		 */
		mPauseWhenBuffered = false;
		if (bufferState && getState() != JWPlayer.PlayerState.BUFFERING) {
			setState(JWPlayer.PlayerState.BUFFERING);
		}
		if (!isAndroidPlayerStateIn(AndroidPlayerState.INITIALIZED)) {
			Log.e(TAG,
					"Trying to prepare media player, but Android media player state is "
							+ getAndroidPlayerState());
		}
		mMediaPlayer.prepareAsync();
		setAndroidPlayerState(AndroidPlayerState.PREPARING);
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.PREPARING)
		 */
	}

	/**
	 * Start playing the currently selected media.
	 * 
	 * @param playingState
	 *            Whether to set the state to playing.
	 */
	public void play(boolean playingState) {
		/*
		 * Pre: true
		 */
		if (!mLoaded) {
			// No loaded media. Ignore this call.
			Log.w(TAG, "No loaded media.");
			return;
		}
		mPauseWhenBuffered = false;
		if (!isAndroidPlayerBuffering()) {
			if (!isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
					AndroidPlayerState.BUFFERING, AndroidPlayerState.PREPARED,
					AndroidPlayerState.PAUSED,
					AndroidPlayerState.PLAYBACK_COMPLETED)) {
				Log.e(TAG,
						"Trying to start media player, but Android media player state is "
								+ getAndroidPlayerState());
			}
			mMediaPlayer.start();
			setAndroidPlayerState(AndroidPlayerState.STARTED);
			mPositionWorker.start();
			if (playingState && getState() != JWPlayer.PlayerState.PLAYING) {
				setState(PlayerState.PLAYING);
			}
			setDisplay(mDisplay);
		}
		else {
			// After the internal player finishes buffering, it will continue
			// anyway.
			if (getState() != JWPlayer.PlayerState.BUFFERING) {
				setState(JWPlayer.PlayerState.BUFFERING);
			}
		}
		/*
		 * Post: !mLoaded || androidPlayerIsBuffering() ||
		 * isAndroidPlayerStateIn(AndroidPlayerState.STARTED)
		 */
	}

	@Override
	public void play() {
		play(true);
	}

	@Override
	public void pause() {
		/*
		 * Pre: true
		 */
		if (!mLoaded) {
			// No loaded media. Ignore this call.
			return;
		}
		mPauseWhenBuffered = true;
		mPositionWorker.stop();
		if (!isAndroidPlayerBuffering()) {
			// We should not pause a buffering Android media player.
			// For some reason, resuming this player is impossible.
			// It will stay in the buffering state, but not get new data.
			if (!isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
					AndroidPlayerState.PAUSED)) {
				Log.e(TAG,
						"Trying to pause media player, but Android media player state is "
								+ getAndroidPlayerState());
			}
			mMediaPlayer.pause();
			setAndroidPlayerState(AndroidPlayerState.PAUSED);
		}
		super.pause();
		/*
		 * Post: !mLoaded || (androidPlayerIsBuffering() && mPauseWhenBuffered)
		 * || isAndroidPlayerStateIn(AndroidPlayerState.PAUSED)
		 */
	}

	@Override
	public void seek(int position) {
		/*
		 * Pre: true
		 */
		if (!mLoaded) {
			// No loaded media. Ignore this call.
			return;
		}
		fireSeekEvent(position);
		// Delay the seek until after the media player is prepared
		if (!isAndroidPlayerPrepared()) {
			mSeekWhenPrepared = position;
			return;
		}
		if (position >= getDuration()) {
			dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_COMPLETE));
			stop();
			return;
		}
		if (!isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
				AndroidPlayerState.BUFFERING, AndroidPlayerState.PREPARED,
				AndroidPlayerState.PAUSED,
				AndroidPlayerState.PLAYBACK_COMPLETED)) {
			Log.e(TAG,
					"Trying to seek media player, but Android media player state is "
							+ getAndroidPlayerState());
		}
		if (position < 0) {
			mMediaPlayer.seekTo(0);
		}
		else {
			mMediaPlayer.seekTo(position);
		}
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
		 * AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED)
		 */
	}

	@Override
	protected void fireSeekEvent(int position) {
		/*
		 * Pre: true
		 */
		if (!isAndroidPlayerPrepared() || mSeekWhenPrepared < 0) {
			// Don't fire the event for seeks that were queued
			super.fireSeekEvent(position);
		}
	}

	@Override
	public void stop() {
		/*
		 * Pre: true
		 */
		if (mMediaPlayer == null) {
			return;
		}
		resetMediaPlayer();
		setupMediaPlayer();
		super.stop();
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.IDLE,
		 * AndroidPlayerState.END)
		 */
	}

	@Override
	protected int getPosition() {
		/*
		 * Pre: isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
		 * AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED,
		 * AndroidPlayerState.PREPARED)
		 */
		if (mSeekWhenPrepared >= 0) {
			return mSeekWhenPrepared;
		}
		if (isAndroidPlayerStateIn(AndroidPlayerState.PREPARED)) {
			return 0;
		}
		if (!isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
				AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED)) {
			Log.e(TAG,
					"Trying to get the playback position, but Android mediaplayer state is "
							+ getAndroidPlayerState());
		}
		return mMediaPlayer.getCurrentPosition();
	}

	@Override
	protected int getDuration() {
		/*
		 * Pre: isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
		 * AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED)
		 */
		if (isLive()) {
			return -1;
		}
		if (!isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
				AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED)) {
			Log.e(TAG,
					"Trying to get the playback duration, but Android mediaplayer state is "
							+ getAndroidPlayerState());
		}
		int duration = mMediaPlayer.getDuration();
		if (duration > 1000000000) {
			// The duration of a player that is preparing is returned as some
			// large number.
			// Ignore, but log.
			Log.w(TAG, "Invalid duration.");
			return -1;
		}
		return duration;
	}

	@Override
	public boolean canSeek() {
		/**
		 * Pre: true
		 */
		return true;
	}

	// A handler for after the initial buffering is done.
	@Override
	public void onPrepared(MediaPlayer mp) {
		setAndroidPlayerState(AndroidPlayerState.PREPARED);
		setDisplay(mDisplay);
		// We should now know the width and height
		if (mVideoSizeFromStream) {
			int width = mMediaPlayer.getVideoWidth();
			int height = mMediaPlayer.getVideoHeight();
			if (width > 0 && height > 0) {
				setDimensions(width, height);
			}
		}
		if (mSeekWhenPrepared != -1) {
			seek(mSeekWhenPrepared);
			mSeekWhenPrepared = -1;
		}
		mPrevPosition = getPosition();
		mPositionUnchanged = 0;
		// Immediately play, unless the user request a pause.
		if (!mPauseWhenBuffered) {
			play();
		}
		// Fire a single time event, so we know the position and duration as
		// soon as it is available
		// updatePosition();
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
		 * AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED)
		 */
	}

	// Handler for when the video size changes.
	// Makes sure the mediaMeta event is dispatched.
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		if (mVideoSizeFromStream) {
			setDimensions(width, height);
		}
	}

	@Override
	public void setDisplay(SurfaceHolder holder) {
		/*
		 * Pre: true
		 */
		if (mMediaPlayer != null) {
			if (mDisplay == holder) {
				// No change
				return;
			}
			mDisplay = holder;
			// If not playing or if not prepared, setDisplay always seems to
			// work
			if (isAndroidPlayerStateIn(AndroidPlayerState.END,
					AndroidPlayerState.ERROR)) {
				// Ignore. Display will be set on next load.
				return;
			}
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1
					|| isAndroidPlayerStateIn(AndroidPlayerState.IDLE,
							AndroidPlayerState.INITIALIZED,
							AndroidPlayerState.STOPPED,
							AndroidPlayerState.PREPARING,
							AndroidPlayerState.PREPARED)) {
				// Should work directly
				try {
					mMediaPlayer.setDisplay(holder);
				}
				catch (IllegalArgumentException e) {
					// "Surface has been released"
					// This surface can not be used.
					mDisplay = null;
					return;
				}
				mMediaPlayer.setScreenOnWhilePlaying(true);
				return;
			}
			// If all else fails, rebuild the whole thing
			Log.v(TAG,
					"Rebuilding, because display was set while player is in state "
							+ getAndroidPlayerState());
			rebuildMediaPlayer();

			// Prevent output buffer flush timeouts by pausing the video
			if (holder == null) {
				if (isAndroidPlayerStateIn(AndroidPlayerState.STARTED)) {
					mMediaPlayer.pause();
					setAndroidPlayerState(AndroidPlayerState.PAUSED);
				}
			}
			else {
				if (isAndroidPlayerStateIn(AndroidPlayerState.PAUSED)
						&& getState() == PlayerState.PLAYING) {
					mMediaPlayer.start();
				}
			}
		}
		else {
			mDisplay = holder;
		}
		/*
		 * Post: mDisplay == holder
		 */
	}

	/**
	 * Reset the internal mediaplayer.
	 */
	protected void resetMediaPlayer() {
		/*
		 * Pre: true
		 */
		if (mMediaPlayer == null
				|| isAndroidPlayerStateIn(AndroidPlayerState.END)) {
			mMediaPlayer = null;
			return;
		}
		mPositionWorker.stop();
		if (!isAndroidPlayerStateIn(AndroidPlayerState.PREPARING)) {
			Log.v(TAG, "Resetting player.");
			mMediaPlayer.reset();
			setAndroidPlayerState(AndroidPlayerState.IDLE);
		}
		else {
			// Stop and reset are not allowed from preparing state.
			// Just throw out the whole thing.
			Log.v(TAG, "Releasing player instead of resetting.");
			mMediaPlayer.release();
			mMediaPlayer = null;
			setAndroidPlayerState(AndroidPlayerState.END);
		}
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.END,
		 * AndroidPlayerState.IDLE)
		 */
	}

	/**
	 * Reset the media player, create it again and resume playback from the
	 * current position.
	 */
	protected void rebuildMediaPlayer() {
		/*
		 * Pre: true
		 */
		Log.v(TAG, "Rebuilding mediaplayer.");
		int pos = 0;
		if (isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
				AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED)) {
			pos = getPosition();
		}
		boolean wasPaused = getState() == PlayerState.PAUSED;
		if (!wasPaused) {
			setState(PlayerState.BUFFERING);
		}
		resetMediaPlayer();
		setupMediaPlayer();
		setDataSource(mUrl);
		if (mSeekWhenPrepared == -1 && pos > 0) {
			mSeekWhenPrepared = pos;
		}
		prepareMediaPlayer(!wasPaused);
		mPauseWhenBuffered = wasPaused;
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.PREPARING)
		 */
	}

	/**
	 * Dispatch the mediaTime event.
	 */
	protected void updatePosition() {
		/*
		 * Pre: isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
		 * AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED)
		 */
		int pos = getPosition();
		// 200 milliseconds check is a hack to get around the fact that updating
		// a display on Android 4.2 causes a delay in playback
		// Time increments are large and irregular during this period
		// Since no video is shown on screen, pretend we're still buffering
		if (pos != mPrevPosition && Math.abs(pos - mPrevPosition) < 200) {
			if (getState() != JWPlayer.PlayerState.PLAYING) {
				setState(JWPlayer.PlayerState.PLAYING);
			}
			MediaEvent evt = new MediaEvent(MediaEvent.JWPLAYER_MEDIA_TIME);
			evt.setDuration(getDuration());
			evt.setPosition(getPosition());
			dispatchEvent(evt);
			mPositionUnchanged = 0;
		}
		else {
			if (++mPositionUnchanged > POSITION_UNCHANGED_TRIGGER_BUFFERING) {
				if (getState() != JWPlayer.PlayerState.BUFFERING) {
					setState(JWPlayer.PlayerState.BUFFERING);
				}
			}
		}
		mPrevPosition = pos;
	}

	// The listener to the info event, which gives us buffering information.
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			// The internal media player is starting to buffer.
			Log.i(TAG, "Buffering started.");
			setAndroidPlayerState(AndroidPlayerState.BUFFERING);
			mPositionWorker.stop();
			setState(JWPlayer.PlayerState.BUFFERING);
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			// The internal media player has finished buffering and will
			// continue to play.
			Log.i(TAG, "Buffering ended.");
			setAndroidPlayerState(AndroidPlayerState.STARTED);
			MediaEvent evt = new MediaEvent(
					MediaEvent.JWPLAYER_MEDIA_BUFFER_FULL);
			evt.setBufferPercent(extra);
			dispatchEvent(evt);
			if (mPauseWhenBuffered) {
				// mAnroidPlayerState is STARTED, no need to check
				mMediaPlayer.pause();
				setAndroidPlayerState(AndroidPlayerState.PAUSED);
			}
			else {
				mPositionWorker.start();
				setState(JWPlayer.PlayerState.PLAYING);
			}
			break;
		case 703: // MEDIA_INFO_NETWORK_BANDWIDTH
			Log.w(TAG,
					"Got a MEDIA_INFO_NETWORK_BANDWIDTH message (bandwidth = "
							+ extra + ")");
			break;
		default:
			break;
		}
		return true;
	}

	// The listener to the Android buffering update event.
	// Dispatches the mediaBuffer event if necessary.
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		if (mBufferPercent != percent) {
			mBufferPercent = percent;
			MediaEvent evt = new MediaEvent(MediaEvent.JWPLAYER_MEDIA_BUFFER);
			evt.setBufferPercent(percent);
			dispatchEvent(evt);
		}
	}

	// The listener to the Android completion event.
	// Dispatches the mediaComplete event.
	@Override
	public void onCompletion(MediaPlayer mp) {
		setAndroidPlayerState(AndroidPlayerState.PLAYBACK_COMPLETED);
		stop();
		dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_COMPLETE));
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.IDLE,
		 * AndroidPlayerState.END)
		 */
	}

	// The listener to the Android error event.
	// Dispatches the mediaError event.
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			setAndroidPlayerState(AndroidPlayerState.ERROR);
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
					"The Android media server died.");
			break;
		case MediaPlayer.MEDIA_ERROR_IO:
			setAndroidPlayerState(AndroidPlayerState.ERROR);
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
					"An I/O error occurred.");
			break;
		case MediaPlayer.MEDIA_ERROR_MALFORMED:
			setAndroidPlayerState(AndroidPlayerState.ERROR);
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
					"Stream is malformed.");
			break;
		case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
			setAndroidPlayerState(AndroidPlayerState.ERROR);
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
					"Stream uses an unsupported feature.");
			break;
		case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
			setAndroidPlayerState(AndroidPlayerState.ERROR);
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
					"An operation timed out.");
			break;
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			setAndroidPlayerState(AndroidPlayerState.ERROR);
			if (mIgnoreNextPlayerError) {
				// Ignore this error
				mIgnoreNextPlayerError = false;
				break;
			}
			switch (extra) {
			case -110:
				// Seems to happen with some streams.
				// Maybe h264 decoding can not keep up.
				// TODO: We should probably rebuild the mediaplayer here, and
				// continue the stream.
				error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
						"Output buffer underrun.");
				break;
			case -1002:
				error(MediaEvent.Error.ERROR_FILE_NOT_FOUND,
						"Could not connect to host.");
				break;
			case -1004:
				error(MediaEvent.Error.ERROR_FILE_NOT_FOUND,
						"HTTP Error 404: File not found.");
				break;
			default:
				error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
						"Unknown Android MediaPlayer error (extra = " + extra
								+ ")");
			}
			break;
		case -38:
			// Most likely means invalid state, but we can ignore this.
			// Thrown around every once in a while during a seek in Android
			// 4.0+
			// Playback still works.
			Log.v(TAG, "Safely ignoring error -38");
			break;
		default:
			// These are undocumented error messages.
			// It seems like in general, we can safely ignore these.
			// If they are critical, it is believed the will give us the
			// MEDIA_ERROR_UNKNOWN error.
			Log.e(TAG, "Unknown Android MediaPlayer error #" + what
					+ " (extra = " + extra + ")\nIgnoring for now.");
		}
		return true;
	}

	/**
	 * A worker which polls the playback progress as the player is playing. Can
	 * be used by any media provider.
	 * 
	 * Periodically (+/- 100ms intervals) calls the updatePosition method.
	 * 
	 * @author tom
	 */
	protected class PositionWorker extends PollingWorker {
		/**
		 * 
		 */
		public PositionWorker() {
			super(POSITION_WORKER_INTERVAL);
		}

		@Override
		protected void tick() {
			updatePosition();
		}
	}

	@Override
	public void release() {
		/*
		 * Pre: true
		 */
		if (mMediaPlayer != null) {
			Log.i(TAG, "Releasing media player.");
			stop();
		}
		if (mMediaPlayer != null) {
			// A stop call may nullify the media player
			mMediaPlayer.release();
		}
		setAndroidPlayerState(AndroidPlayerState.END);
		mMediaPlayer = null;
		/*
		 * Post: mMediaPlayer == null &&
		 * isAndroidPlayerStateIn(AndroidPlayerState.END)
		 */
	}

	@Override
	public void triggerBuffer() {
		if (getState() == PlayerState.PLAYING) {
			super.triggerBuffer();
			mPrevPosition = getPosition();
			mPositionUnchanged = 0;
		}
	}

	/**
	 * @return Whether the underlying media player reports to be buffering.
	 */
	protected boolean isAndroidPlayerBuffering() {
		return isAndroidPlayerStateIn(AndroidPlayerState.BUFFERING,
				AndroidPlayerState.PREPARING);
	}

	/**
	 * @return Whether the underlying media player is prepared.
	 */
	protected boolean isAndroidPlayerPrepared() {
		return isAndroidPlayerStateIn(AndroidPlayerState.PREPARED,
				AndroidPlayerState.STARTED, AndroidPlayerState.BUFFERING,
				AndroidPlayerState.PAUSED,
				AndroidPlayerState.PLAYBACK_COMPLETED);
	}

	/**
	 * @return The state of the underlying player.
	 */
	protected AndroidPlayerState getAndroidPlayerState() {
		return mAndroidPlayerState;
	}

	/**
	 * @param state
	 *            The state of the underlying player.
	 */
	protected void setAndroidPlayerState(AndroidPlayerState state) {
		Log.v(TAG, "Android player state " + mAndroidPlayerState + " -> "
				+ state);
		mAndroidPlayerState = state;
	}

	/**
	 * @param states
	 *            The states to check.
	 * @return Whether the underlying player is in any of the given states.
	 */
	protected boolean isAndroidPlayerStateIn(AndroidPlayerState... states) {
		for (AndroidPlayerState state : states) {
			if (state == mAndroidPlayerState) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The possible states for the underlying player.
	 * 
	 * @author tom
	 */
	protected enum AndroidPlayerState {
		/**
		 * Not initialized.
		 */
		IDLE,
		/**
		 * Player was given a URL.
		 */
		INITIALIZED,
		/**
		 * Player is going through its first buffering stage.
		 */
		PREPARING,
		/**
		 * Initial buffering is done, and playback is not started yet.
		 */
		PREPARED,
		/**
		 * No official state, but buffering has some annoying implications.
		 */
		BUFFERING,
		/**
		 * Playback has started.
		 */
		STARTED,
		/**
		 * Playback was started, but is now paused.
		 */
		PAUSED,
		/**
		 * Playback is stopped.
		 */
		STOPPED,
		/**
		 * Media finished playing.
		 */
		PLAYBACK_COMPLETED,
		/**
		 * Player was released.
		 */
		END,
		/**
		 * An error occurred.
		 */
		ERROR
	}
}
