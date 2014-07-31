package com.longtailvideo.jwplayer;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.longtailvideo.jwplayer.controller.Controller;
import com.longtailvideo.jwplayer.events.EventListener;
import com.longtailvideo.jwplayer.events.MediaEvent;
import com.longtailvideo.jwplayer.events.PlayerEvent;
import com.longtailvideo.jwplayer.events.PlayerStateEvent;
import com.longtailvideo.jwplayer.events.QualityEvent;
import com.longtailvideo.jwplayer.events.ViewEvent;
import com.longtailvideo.jwplayer.model.Model;
import com.longtailvideo.jwplayer.utils.EventLogger;
import com.longtailvideo.jwplayer.view.View;

/**
 * Main player component
 * 
 * @author tom
 */
public class JWPlayer {
	/**
	 * Whether to show controls in the player by default.
	 */
	public static final boolean DEFAULT_ENABLE_CONTROLS = true;

	/**
	 * Model part of the player.
	 * 
	 * Keeps track of the media that are being played.
	 */
	protected Model mModel;
	/**
	 * View part of the player.
	 * 
	 * Controls the way the users see the media.
	 */
	protected View mView;
	/**
	 * Controller of the player.
	 * 
	 * Reacts to user or API input.
	 */
	protected Controller mController;

	/**
	 * The global event logger.
	 * 
	 * All forwarded events in this object are added to the Android log.
	 */
	protected EventLogger mEventLogger;

	//
	// Event handlers
	//

	private OnCompleteListener mOnCompleteListener;
	private OnPlayListener mOnPlayListener;
	private OnPauseListener mOnPauseListener;
	private OnBufferListener mOnBufferListener;
	private OnIdleListener mOnIdleListener;
	private OnSeekListener mOnSeekListener;
	private OnTimeListener mOnTimeListener;
	private OnErrorListener mOnErrorListener;
	private OnFullscreenListener mOnFullscreenListener;
	private OnQualityLevelsListener mOnQualityLevelsListener;
	private OnQualityChangeListener mOnQualityChangeListener;

	/**
	 * @param context
	 *            The context for the view.
	 * @param controls
	 *            Whether to show controls in the player.
	 */
	public JWPlayer(Context context, boolean controls) {
		mModel = new Model(this);
		mController = new Controller(this, mModel);
		mView = new View(mModel, mController, context, controls);

		mEventLogger = new EventLogger(Log.DEBUG);
		// Log some interesting events
		mEventLogger.startForward(PlayerEvent.JWPLAYER_ERROR, mModel);
		mEventLogger.startForward(MediaEvent.JWPLAYER_MEDIA_BUFFER, mModel);
		mEventLogger.startForward(MediaEvent.JWPLAYER_MEDIA_SELECTED, mModel);
		mEventLogger
				.startForward(MediaEvent.JWPLAYER_MEDIA_BUFFER_FULL, mModel);
		mEventLogger.startForward(MediaEvent.JWPLAYER_MEDIA_COMPLETE, mModel);
		mEventLogger.startForward(MediaEvent.JWPLAYER_MEDIA_LOADED, mModel);
		mEventLogger.startForward(MediaEvent.JWPLAYER_MEDIA_SEEK, mModel);
		mEventLogger.startForward(MediaEvent.JWPLAYER_MEDIA_TIME, mModel);
		mEventLogger.startForward(MediaEvent.JWPLAYER_MEDIA_META, mModel);
		mEventLogger.startForward(PlayerStateEvent.JWPLAYER_PLAYER_STATE,
				mModel);
		mEventLogger.startForward(ViewEvent.JWPLAYER_VIEW_FULLSCREEN, mView);
		mEventLogger.startForward(QualityEvent.JWPLAYER_QUALITY_LEVELS, mModel);
		mEventLogger.startForward(QualityEvent.JWPLAYER_QUALITY_CHANGE, mModel);

		// Forward the mediaComplete event as onComplete
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_COMPLETE,
				new EventListener<MediaEvent>() {
					@Override
					public void handle(MediaEvent event) {
						if (mOnCompleteListener != null) {
							mOnCompleteListener.onComplete();
						}
					}
				});

		// Forward the playerState event as one of onPlay, onPause, onBuffer or
		// onIdle.
		mModel.addEventListener(PlayerStateEvent.JWPLAYER_PLAYER_STATE,
				new EventListener<PlayerStateEvent>() {
					@Override
					public void handle(PlayerStateEvent event) {
						switch (event.getNewState()) {
						case PLAYING:
							if (mOnPlayListener != null) {
								mOnPlayListener.onPlay();
							}
							break;
						case PAUSED:
							if (mOnPauseListener != null) {
								mOnPauseListener.onPause();
							}
							break;
						case BUFFERING:
							if (mOnBufferListener != null) {
								mOnBufferListener.onBuffer();
							}
							break;
						case IDLE:
							if (mOnIdleListener != null) {
								mOnIdleListener.onIdle();
							}
							break;
						default:
							break;
						}
					}
				});

		// Forward the mediaSeek event as onSeek
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_SEEK,
				new EventListener<MediaEvent>() {
					@Override
					public void handle(MediaEvent event) {
						if (mOnSeekListener != null) {
							mOnSeekListener.onSeek(event.getPosition(),
									event.getOffset());
						}
					}
				});

		// Forward the mediaTime event as onTime
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_TIME,
				new EventListener<MediaEvent>() {
					@Override
					public void handle(MediaEvent event) {
						// Do not forward for livestreams
						if (mOnTimeListener != null
								&& !mModel.getMedia().isLive()) {
							mOnTimeListener.onTime(event.getPosition(),
									event.getDuration());
						}
					}
				});

		// Forward the playerError event as onError
		mModel.addEventListener(PlayerEvent.JWPLAYER_ERROR,
				new EventListener<PlayerEvent>() {
					@Override
					public void handle(PlayerEvent event) {
						if (mOnErrorListener != null) {
							mOnErrorListener.onError(event.getMessage());
						}
					}
				});

		// Forward the viewFullscreen event as onFullscreen
		mView.addEventListener(ViewEvent.JWPLAYER_VIEW_FULLSCREEN,
				new EventListener<ViewEvent>() {
					@Override
					public void handle(ViewEvent event) {
						if (mOnFullscreenListener != null) {
							mOnFullscreenListener.onFullscreen(event
									.isEnabled());
						}
					}
				});

		// Forward the qualityLevels event as onQualityLevels
		mModel.addEventListener(QualityEvent.JWPLAYER_QUALITY_LEVELS,
				new EventListener<QualityEvent>() {
					@Override
					public void handle(QualityEvent event) {
						if (mOnQualityLevelsListener != null) {
							mOnQualityLevelsListener.onQualityLevels(event
									.getLevels());
						}
					}
				});

		// Forward the qualityChange event as onQualityChange
		mModel.addEventListener(QualityEvent.JWPLAYER_QUALITY_CHANGE,
				new EventListener<QualityEvent>() {
					@Override
					public void handle(QualityEvent event) {
						if (mOnQualityChangeListener != null) {
							mOnQualityChangeListener.onQualityChange(event
									.getCurrentLevel());
						}
					}
				});
	}

	/**
	 * @param context
	 *            The context for the view.
	 */
	public JWPlayer(Context context) {
		// Use the default setting for controls.
		this(context, DEFAULT_ENABLE_CONTROLS);
	}

	//
	// Playlist API
	//

	/**
	 * Load media.
	 * 
	 * @param url
	 *            The URL which points to the media to be played.
	 * @return Whether the preparations for loading were completed successfully.
	 *         Usually, the loading itself is done asynchronously, and will
	 *         result in a mediaError event.
	 */
	public boolean load(String url) {
		return mController.load(url);
	}

	/**
	 * Event listener for onComplete.
	 * 
	 * @author tom
	 */
	public interface OnCompleteListener {
		/**
		 * 
		 */
		public void onComplete();
	}

	/**
	 * @param listener
	 *            The listener for the onComplete event.
	 */
	public void setOnCompleteListener(OnCompleteListener listener) {
		mOnCompleteListener = listener;
	}

	//
	// Playback API
	//

	/**
	 * Possible states for media and the player in general.
	 * 
	 * @author tom
	 */
	public static enum PlayerState {
		/**
		 * No media are playing.
		 */
		IDLE,
		/**
		 * Temporarily paused while the buffer is being filled.
		 */
		BUFFERING,
		/**
		 * Media playback.
		 */
		PLAYING,
		/**
		 * Paused until manually resumed.
		 */
		PAUSED,
	}

	/**
	 * @return The current state of the media being played.
	 */
	public PlayerState getState() {
		return mModel.getState();
	}

	/**
	 * Toggle playing state of a video. If playing, pauses. If paused, resumes.
	 * If idle, starts.
	 * 
	 * @return whether the operation was successful.
	 */
	public boolean play() {
		assertUIThread();
		return mController.play();
	}

	/**
	 * Start or suspend playback.
	 * 
	 * @param state
	 *            If true, start playback. If false, pause.
	 * @return whether the operation was successful.
	 */
	public boolean play(boolean state) {
		assertUIThread();
		return mController.play(state);
	}

	/**
	 * Toggle playing state of a video. If playing, pauses. If paused, resumes.
	 * If idle, starts.
	 * 
	 * @return whether the operation was successful.
	 */
	public boolean pause() {
		assertUIThread();
		return mController.pause();
	}

	/**
	 * Suspend or resume playback.
	 * 
	 * @param state
	 *            If true, pause playback. If false, resume.
	 * @return whether the operation was successful.
	 */
	public boolean pause(boolean state) {
		assertUIThread();
		return mController.pause(state);
	}

	/**
	 * Stop the currently playing media.
	 * 
	 * @return Whether media were playing when before the operation.
	 */
	public boolean stop() {
		return mController.stop();
	}

	/**
	 * Event listener for onPlay.
	 * 
	 * @author tom
	 */
	public interface OnPlayListener {
		/**
		 * 
		 */
		public void onPlay();
	}

	/**
	 * Event listener for onPause.
	 * 
	 * @author tom
	 */
	public interface OnPauseListener {
		/**
		 * 
		 */
		public void onPause();
	}

	/**
	 * Event listener for onBuffer.
	 * 
	 * @author tom
	 */
	public interface OnBufferListener {
		/**
		 * 
		 */
		public void onBuffer();
	}

	/**
	 * Event listener for onIdle.
	 * 
	 * @author tom
	 */
	public interface OnIdleListener {
		/**
		 * 
		 */
		public void onIdle();
	}

	/**
	 * @param listener
	 *            The listener for the onPlay event.
	 */
	public void setOnPlayListener(OnPlayListener listener) {
		mOnPlayListener = listener;
	}

	/**
	 * @param listener
	 *            The listener for the onPause event.
	 */
	public void setOnPauseListener(OnPauseListener listener) {
		mOnPauseListener = listener;
	}

	/**
	 * @param listener
	 *            The listener for the onIdle event.
	 */
	public void setOnBufferListener(OnBufferListener listener) {
		mOnBufferListener = listener;
	}

	/**
	 * @param listener
	 *            The listener for the onIdle event.
	 */
	public void setOnIdleListener(OnIdleListener listener) {
		mOnIdleListener = listener;
	}

	//
	// Seek API
	//

	/**
	 * Seek the currently playing media to a specific position and start
	 * playback.
	 * 
	 * @param position
	 *            The position to seek to in milliseconds from the start.
	 * @return Whether the operation was successful.
	 */
	public boolean seek(int position) {
		return mController.seek(position);
	}

	/**
	 * Event listener for onSeek.
	 * 
	 * @author tom
	 */
	public interface OnSeekListener {
		/**
		 * @param position
		 *            The current position in milliseconds.
		 * @param offset
		 *            The target position in milliseconds.
		 */
		public void onSeek(int position, int offset);
	}

	/**
	 * Event listener for onTime.
	 * 
	 * @author tom
	 */
	public interface OnTimeListener {
		/**
		 * @param position
		 *            The current playback position in milliseconds.
		 * @param duration
		 *            The duration of the current media in milliseconds.
		 */
		public void onTime(int position, int duration);
	}

	/**
	 * @param listener
	 *            The listener for the onTime event.
	 */
	public void setOnSeekListener(OnSeekListener listener) {
		mOnSeekListener = listener;
	}

	/**
	 * @param listener
	 *            The listener for the onTime event.
	 */
	public void setOnTimeListener(OnTimeListener listener) {
		mOnTimeListener = listener;
	}

	//
	// Error API
	//

	/**
	 * Event listener for onError.
	 * 
	 * @author tom
	 */
	public interface OnErrorListener {
		/**
		 * @param message
		 *            A string representation of the error.
		 */
		public void onError(String message);
	}

	/**
	 * @param listener
	 *            The listener for the onError event.
	 */
	public void setOnErrorListener(OnErrorListener listener) {
		mOnErrorListener = listener;
	}

	//
	// Resize API
	//

	/**
	 * @return Whether the video player is running in full screen mode.
	 */
	@SuppressWarnings("static-method")
	public boolean getFullscreen() {
		return false;
	}

	/**
	 * @param state
	 *            Whether the video player should run in full screen mode.
	 */
	public void setFullscreen(boolean state) {
		mView.fullScreen(state);
	}

	/**
	 * Event listener for onFullscreen.
	 * 
	 * @author tom
	 */
	public interface OnFullscreenListener {
		/**
		 * @param state
		 *            Whether the property was enabled.
		 */
		public void onFullscreen(boolean state);
	}

	/**
	 * @param listener
	 *            The listener for the onFullscreen event.
	 */
	public void setOnFullscreenListener(OnFullscreenListener listener) {
		mOnFullscreenListener = listener;
	}

	//
	// Quality API
	//

	/**
	 * A quality level of a multibitrate stream.
	 * 
	 * @author tom
	 */
	public static class QualityLevel implements Comparable<QualityLevel> {
		private String mName = null;
		private int mBitrate = -1;
		private int mWidth = -1;
		private int mHeight = -1;
		private boolean mAudio = false;

		/**
		 * Create the "Auto" quality level.
		 */
		public QualityLevel() {

		}

		/**
		 * @param bitrate
		 *            The bitrate for the quality level.
		 */
		public QualityLevel(int bitrate) {
			mBitrate = bitrate;
		}

		/**
		 * @param name
		 *            The name of the quality level.
		 * @param bitrate
		 *            The bitrate for the quality level.
		 */
		public QualityLevel(String name, int bitrate) {
			mName = name;
			mBitrate = bitrate;
		}

		/**
		 * @return The width of the stream represented by the level, or -1 if
		 *         unknown.
		 */
		public int getWidth() {
			return mWidth;
		}

		/**
		 * @param width
		 *            The width of the stream represented by the level, or -1 if
		 *            unknown.
		 */
		public void setWidth(int width) {
			mWidth = width;
		}

		/**
		 * @return The height of the stream represented by the level, or -1 if
		 *         unknown.
		 */
		public int getHeight() {
			return mHeight;
		}

		/**
		 * @param height
		 *            The height of the stream represented by the level, or -1
		 *            if unknown.
		 */
		public void setHeight(int height) {
			mHeight = height;
		}

		@Override
		public String toString() {
			if (mName == null && mBitrate < 0) {
				return "Auto";
			}
			else if (mName != null) {
				return mName;
			}
			else if (mHeight > 0) {
				return mHeight + "p";
			}
			else {
				return mBitrate / 1000 + "kbps";
			}
		}

		@Override
		public int compareTo(QualityLevel another) {
			return Integer.valueOf(mBitrate).compareTo(
					Integer.valueOf(another.mBitrate));
		}

		@Override
		public int hashCode() {
			// Equality is based on equal labels
			return toString().hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof QualityLevel) {
				// Equality is based on equal labels
				return o.toString().equals(toString());
			}
			return super.equals(o);
		}

		@Override
		public Object clone() {
			QualityLevel l;
			if (mBitrate < 0) {
				l = new QualityLevel();
			}
			else if (mName == null) {
				l = new QualityLevel(mBitrate);
			}
			else {
				l = new QualityLevel(mName, mBitrate);
			}
			l.setWidth(mWidth);
			l.setHeight(mHeight);
			return l;
		}

		/**
		 * @return Whether the level represents the auto-switching level.
		 */
		public boolean isAuto() {
			return mBitrate < 0;
		}

		/**
		 * @return The stream's bitrate.
		 */
		public int getBitrate() {
			return mBitrate;
		}

		/**
		 * @param audio
		 *            Whether the level only contains audio.
		 */
		public void setAudio(boolean audio) {
			mAudio = audio;
		}

		/**
		 * @return Whether the level only contains audio.
		 */
		public boolean isAudio() {
			return mAudio;
		}
	}

	/**
	 * @return The array of available quality levels.
	 */
	public QualityLevel[] getQualityLevels() {
		return mModel.getQualityLevels();
	}

	/**
	 * @return The index of the current quality level.
	 */
	public int getCurrentQuality() {
		return mModel.getCurrentQuality();
	}

	/**
	 * @param index
	 *            The index of the desired quality level.
	 */
	public void setCurrentQuality(int index) {
		mModel.setCurrentQuality(index);
	}

	/**
	 * Event listener for onQualityLevels.
	 * 
	 * @author tom
	 */
	public interface OnQualityLevelsListener {
		/**
		 * @param levels
		 *            The new set of quality levels.
		 */
		public void onQualityLevels(QualityLevel[] levels);
	}

	/**
	 * Event listener for onQualityLevels.
	 * 
	 * @author tom
	 */
	public interface OnQualityChangeListener {
		/**
		 * @param currentQuality
		 *            The new quality level.
		 */
		public void onQualityChange(QualityLevel currentQuality);
	}

	/**
	 * @param listener
	 *            The listener for the qualityLevels event.
	 */
	public void setOnQualityLevelsListener(OnQualityLevelsListener listener) {
		mOnQualityLevelsListener = listener;
	}

	/**
	 * @param listener
	 *            The listener for the qualityChange event.
	 */
	public void setOnQualityChangeListener(OnQualityChangeListener listener) {
		mOnQualityChangeListener = listener;
	}

	//
	// Android API
	//

	/**
	 * Release the resources that are in use by the player.
	 */
	public void release() {
		mModel.release();
	}

	//
	// Internal
	//

	/**
	 * @return The Android view containing the player.
	 */
	View getView() {
		return mView;
	}

	/**
	 * Add a runnable to the message queue of the view. The runnable gets
	 * executed on the UI thread.
	 * 
	 * @param runnable
	 *            The runnable to post to the UI thread.
	 */
	public void post(Runnable runnable) {
		mView.post(runnable);
	}

	/**
	 * Assert that the current thread is the UI thread. Used for preventing an
	 * infinite buffering problem when play or pause is called from a non-UI
	 * thread.
	 */
	private static void assertUIThread() {
		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new RuntimeException("Call must be run from UI thread.");
		}
	}

	/**
	 * Metadata about media.
	 * 
	 * @author tom
	 */
	public static class Metadata {
		/**
		 * Dimension metadata. Describes the width and height of a video.
		 * 
		 * @author tom
		 */
		public static class Dimensions extends Metadata {
			int mWidth, mHeight;

			/**
			 * @param width
			 *            The width of the video.
			 * @param height
			 *            The height of the video.
			 */
			public Dimensions(int width, int height) {
				mWidth = width;
				mHeight = height;
			}

			/**
			 * @return The width of the video.
			 */
			public int getWidth() {
				return mWidth;
			}

			/**
			 * @return The height of the video.
			 */
			public int getHeight() {
				return mHeight;
			}

			@Override
			public String toString() {
				return "<dimensions width=" + mWidth + " height=" + mHeight
						+ ">";
			}
		}

		/**
		 * Dimension metadata. Describes the width and height of a video.
		 * 
		 * @author tom
		 */
		public static class Stream extends Metadata {
			int mDuration;
			boolean mIsLive;

			/**
			 * Non-live stream.
			 * 
			 * @param duration
			 *            The duration of the stream.
			 */
			public Stream(int duration) {
				mDuration = duration;
				mIsLive = false;
			}

			/**
			 * Stream with no known duration.
			 * 
			 * @param live
			 *            Whether the stream is a live stream.
			 */
			public Stream(boolean live) {
				mDuration = -1;
				mIsLive = live;
			}

			/**
			 * @return The duration of the stream, or -1 if unknown.
			 */
			public int getDuration() {
				return mDuration;
			}

			/**
			 * @return Whether the stream is a livestream.
			 */
			public boolean isLive() {
				return mIsLive;
			}

			@Override
			public String toString() {
				String str = "<stream live=" + mIsLive;
				if (mDuration >= 0) {
					str += " duration=" + mDuration;
				}
				str += ">";
				return str;
			}
		}

		@Override
		public String toString() {
			return "<unknown>";
		}
	}
}
