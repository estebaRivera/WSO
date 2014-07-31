package com.longtailvideo.jwplayer;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * The widget containing the JW Player.
 * 
 * @author tom
 */
public class JWPlayerView extends FrameLayout {
	// The contained JW Player
	private JWPlayer mJWPlayer;

	// The last known position of the currently playing media
	// If unknown, equal to -1
	private int mPosition = -1;
	// The last known duration of the currently playing media
	// If unknown, equal to -1
	private int mDuration = -1;

	// The listener for the onTime event
	private JWPlayer.OnTimeListener mOnTimeListener;

	// The listener for the onSeek event
	private JWPlayer.OnSeekListener mOnSeekListener;

	// The listener for the onIdle event
	private JWPlayer.OnIdleListener mOnIdleListener;

	/**
	 * @param context
	 *            The view context.
	 */
	public JWPlayerView(Context context) {
		super(context);
		// Use the default value for controls.
		setupJWPlayerView(JWPlayer.DEFAULT_ENABLE_CONTROLS);
	}

	/**
	 * @param context
	 *            The view context.
	 * @param attrs
	 *            The attributes to the view.
	 */
	public JWPlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.JWPlayerView);
		setupJWPlayerView(a.getBoolean(R.styleable.JWPlayerView_controls, true));
		String file = a.getString(R.styleable.JWPlayerView_file);
		if (file != null && !"".equals(file)) {
			load(file);
		}
		a.recycle();
	}

	/**
	 * @param controls
	 *            Whether controls should be enabled in the player.
	 */
	private void setupJWPlayerView(boolean controls) {
		// Create the player
		mJWPlayer = new JWPlayer(getContext(), controls);

		// Add the actual view as a child
		addView(mJWPlayer.getView(),
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		// Event listeners
		mJWPlayer.setOnTimeListener(new JWPlayer.OnTimeListener() {
			@Override
			public void onTime(int position, int duration) {
				mPosition = position;
				mDuration = duration;
				if (mOnTimeListener != null) {
					mOnTimeListener.onTime(position, duration);
				}
			}
		});

		mJWPlayer.setOnSeekListener(new JWPlayer.OnSeekListener() {
			@Override
			public void onSeek(int position, int offset) {
				mPosition = position;
				if (mOnSeekListener != null) {
					mOnSeekListener.onSeek(position, offset);
				}
			}
		});

		mJWPlayer.setOnIdleListener(new JWPlayer.OnIdleListener() {
			@Override
			public void onIdle() {
				// Reset the position
				mPosition = -1;
				if (mOnIdleListener != null) {
					mOnIdleListener.onIdle();
				}
			}
		});
	}

	//
	// Playlist API
	//

	/**
	 * Load media from the given URL.
	 * 
	 * @param url
	 *            The URL pointing toward media of a playable type.
	 * @return Whether the operation has completed successfully.
	 */
	public boolean load(String url) {
		mPosition = -1;
		mDuration = -1;
		return mJWPlayer.load(url);
	}

	/**
	 * @param listener
	 *            The listener to the onComplete event.
	 */
	public void setOnCompleteListener(JWPlayer.OnCompleteListener listener) {
		mJWPlayer.setOnCompleteListener(listener);
	}

	//
	// Playback API
	//

	/**
	 * @return The current state of the player.
	 */
	public JWPlayer.PlayerState getState() {
		return mJWPlayer.getState();
	}

	/**
	 * Toggle playing state of a video. If playing, pauses. If paused, resumes.
	 * If idle, starts.
	 * 
	 * @return whether the operation was successful.
	 */
	public boolean play() {
		return mJWPlayer.play();
	}

	/**
	 * Start or suspend playback.
	 * 
	 * @param state
	 *            If true, start playback. If false, pause.
	 * @return whether the operation was successful.
	 */
	public boolean play(boolean state) {
		return mJWPlayer.play(state);
	}

	/**
	 * Toggle playing state of a video. If playing, pauses. If paused, resumes.
	 * If idle, starts.
	 * 
	 * @return whether the operation was successful.
	 */
	public boolean pause() {
		return mJWPlayer.pause();
	}

	/**
	 * Suspend or resume playback.
	 * 
	 * @param state
	 *            If true, pause playback. If false, resume.
	 * @return whether the operation was successful.
	 */
	public boolean pause(boolean state) {
		return mJWPlayer.pause(state);
	}

	/**
	 * Stop the currently playing media.
	 * 
	 * @return Whether the player was stopped.
	 */
	public boolean stop() {
		return mJWPlayer.stop();
	}

	/**
	 * @param listener
	 *            The listener for the onPlay event.
	 */
	public void setOnPlayListener(JWPlayer.OnPlayListener listener) {
		mJWPlayer.setOnPlayListener(listener);
	}

	/**
	 * @param listener
	 *            The listener for the onPause event.
	 */
	public void setOnPauseListener(JWPlayer.OnPauseListener listener) {
		mJWPlayer.setOnPauseListener(listener);
	}

	/**
	 * @param listener
	 *            The listener for the onBuffer event.
	 */
	public void setOnBufferListener(JWPlayer.OnBufferListener listener) {
		mJWPlayer.setOnBufferListener(listener);
	}

	/**
	 * @param listener
	 *            The listener for the onIdle event.
	 */
	public void setOnIdleListener(JWPlayer.OnIdleListener listener) {
		mOnIdleListener = listener;
	}

	//
	// Seek API
	//

	/**
	 * @return The current playback position in milliseconds, or -1 if not
	 *         available.
	 */
	public int getPosition() {
		return mPosition;
	}

	/**
	 * @return The current media duration in milliseconds, or -1 if not
	 *         available.
	 */
	public int getDuration() {
		return mDuration;
	}

	/**
	 * Seek the currently playing media to the specified position.
	 * 
	 * @param position
	 *            The position to seek to in milliseconds from the beginning.
	 * @return Whether the operation has completed successfully.
	 */
	public boolean seek(int position) {
		boolean success = mJWPlayer.seek(position);
		if (success) {
			mPosition = position;
		}
		return success;
	}

	/**
	 * @param listener
	 *            The listener for the onSeek event.
	 */
	public void setOnSeekListener(JWPlayer.OnSeekListener listener) {
		mOnSeekListener = listener;
	}

	/**
	 * @param listener
	 *            The listener for the onTime event.
	 */
	public void setOnTimeListener(JWPlayer.OnTimeListener listener) {
		mOnTimeListener = listener;
	}

	//
	// Error API
	//

	/**
	 * @param listener
	 *            The listener to the onError event.
	 */
	public void setOnErrorListener(JWPlayer.OnErrorListener listener) {
		mJWPlayer.setOnErrorListener(listener);
	}

	//
	// Resize API
	//

	/**
	 * @return Whether the video player is running in full screen mode.
	 */
	public boolean getFullscreen() {
		return mJWPlayer.getFullscreen();
	}

	/**
	 * @param state
	 *            Whether the video player should run in full screen mode.
	 */
	public void setFullscreen(boolean state) {
		mJWPlayer.setFullscreen(state);
	}

	/**
	 * @param listener
	 *            The listener for the onFullscreen event.
	 */
	public void setOnFullscreenListener(JWPlayer.OnFullscreenListener listener) {
		mJWPlayer.setOnFullscreenListener(listener);
	}

	//
	// Quality API
	//

	/**
	 * @return The array of available quality levels.
	 */
	public JWPlayer.QualityLevel[] getQualityLevels() {
		return mJWPlayer.getQualityLevels();
	}

	/**
	 * @return The index of the current quality level.
	 */
	public int getCurrentQuality() {
		return mJWPlayer.getCurrentQuality();
	}

	/**
	 * @param index
	 *            The index of the desired quality level.
	 */
	public void setCurrentQuality(int index) {
		mJWPlayer.setCurrentQuality(index);
	}

	/**
	 * @param listener
	 *            The listener for the qualityLevels event.
	 */
	public void setOnQualityLevelsListener(
			JWPlayer.OnQualityLevelsListener listener) {
		mJWPlayer.setOnQualityLevelsListener(listener);
	}

	/**
	 * @param listener
	 *            The listener for the qualityChange event.
	 */
	public void setOnQualityChangeListener(
			JWPlayer.OnQualityChangeListener listener) {
		mJWPlayer.setOnQualityChangeListener(listener);
	}

	//
	// Android API
	//

	/**
	 * Release the resources that are in use by the player.
	 */
	public void release() {
		mJWPlayer.release();
	}
}
