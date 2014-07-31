package com.longtailvideo.jwplayer.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.longtailvideo.jwplayer.JWPlayer;
import com.longtailvideo.jwplayer.JWPlayer.PlayerState;
import com.longtailvideo.jwplayer.JWPlayer.QualityLevel;
import com.longtailvideo.jwplayer.events.EventForwarder;
import com.longtailvideo.jwplayer.events.EventListener;
import com.longtailvideo.jwplayer.events.MediaEvent;
import com.longtailvideo.jwplayer.events.PlayerEvent;
import com.longtailvideo.jwplayer.media.HLSMediaProvider;
import com.longtailvideo.jwplayer.media.MediaProvider;
import com.longtailvideo.jwplayer.media.NativeHLSMediaProvider;
import com.longtailvideo.jwplayer.media.VideoMediaProvider;

/**
 * The model of the player.
 * 
 * Events:
 * 
 * playerState: When the media changes state.
 * 
 * mediaLoaded: When a new media URL is loaded.
 * 
 * mediaSeek: When a seek request is received.
 * 
 * mediaError: When an error has occurred in the media playback.
 * 
 * qualityChange: When the quality of the current stream changed.
 * 
 * qualityLevels: When the set of quality levels of the stream is set.
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
 * playerError: In case of a media error.
 * 
 * @author tom
 */
public class Model extends EventForwarder {
	private static final String TAG = "Model";

	// The focused media
	private MediaProvider mCurrentMedia;
	// A map from media source type to the provider for the media sources of
	// that type.
	private final Map<String, MediaProvider> mMediaSources;

	// To be replaced by a playlist at some point:
	private String mUrl;

	// The player of which this model is a part.
	private final JWPlayer mPlayer;

	/**
	 * @param player
	 *            The player of which this model is a part.
	 */
	public Model(JWPlayer player) {
		mPlayer = player;
		mMediaSources = new HashMap<String, MediaProvider>();
		setupMediaSources();
		// Forward media errors as player errors
		addEventListener(MediaEvent.JWPLAYER_MEDIA_ERROR,
				new EventListener<MediaEvent>() {
					@Override
					public void handle(MediaEvent event) {
						dispatchEvent(new PlayerEvent(
								PlayerEvent.JWPLAYER_ERROR, event.getMessage()));
					}
				});
	}

	/**
	 * Load the media at the given URL.
	 * 
	 * @param url
	 *            The URL pointing to the media item to load.
	 * @return Whether the operation was successful.
	 */
	public boolean load(String url) {
		// Validate url to prevent unknown Android errors.
		try {
			String protocol = new URL(url).getProtocol();
			if (!protocol.equals("http") && !protocol.equals("https")) {
				Log.e(TAG, "Protocol '" + protocol + "' is not supported.");
				dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_ERROR,
						MediaEvent.Error.ERROR_FILE_NOT_FOUND));
				return false;
			}
		}
		catch (MalformedURLException e) {
			Log.e(TAG, "URL '" + url + "' is malformed.");
			dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_ERROR,
					MediaEvent.Error.ERROR_FILE_NOT_FOUND));
			return false;
		}
		mUrl = url;

		MediaEvent evt = new MediaEvent(MediaEvent.JWPLAYER_MEDIA_SELECTED);
		evt.setUrl(url);
		dispatchEvent(evt);

		return true;
	}

	/**
	 * Load the next media.
	 */
	public void loadItem() {
		if (mUrl != null) {
			String type = getItemType();
			setActiveMediaProvider(type);
			mCurrentMedia.load(mUrl);
		}
	}

	/**
	 * @return The type of the loaded media.
	 */
	private String getItemType() {
		final String DEFAULT = "video";
		// Currently only based on file extension
		String filename = Uri.parse(mUrl).getLastPathSegment();
		if (filename == null) {
			filename = "";
		}
		int dotPos = filename.lastIndexOf(".");
		if (dotPos == -1 || dotPos == filename.length() - 1) {
			// No extension in the URL
			return DEFAULT;
		}
		String extension = filename.substring(dotPos + 1);
		if ("m3u8".equals(extension)) {
			return "hls";
		}
		else if ("mp4".equals(extension)) {
			return "video";
		}
		// etc... XXX: Maybe use a map for this in the future.
		return DEFAULT;
	}

	/**
	 * Set the current media provider to the provider for the given type.
	 * 
	 * @param type
	 *            The type of the desired provider.
	 */
	private void setActiveMediaProvider(String type) {
		if (mCurrentMedia != null) {
			stopForwardGlobal(mCurrentMedia);
			mCurrentMedia.stop();
		}
		mCurrentMedia = mMediaSources.get(type);
		startForwardGlobal(mCurrentMedia);
	}

	/**
	 * Load the map of media providers.
	 */
	public void setupMediaSources() {
		mMediaSources.put("video", new VideoMediaProvider(mPlayer, this));
		// 4.0.4 devices must use the native HLS functionality
		if ("4.0.4".equals(Build.VERSION.RELEASE)) {
			mMediaSources.put("hls", new NativeHLSMediaProvider(mPlayer, this));
		}
		else {
			mMediaSources.put("hls", new HLSMediaProvider(mPlayer, this));
		}
		mCurrentMedia = new MediaProvider(mPlayer, this);
	}

	/**
	 * @return The current media, or null if no media are being used.
	 */
	public MediaProvider getMedia() {
		return mCurrentMedia;
	}

	/**
	 * @return The state of the player model.
	 */
	public JWPlayer.PlayerState getState() {
		if (mCurrentMedia == null) {
			return JWPlayer.PlayerState.IDLE;
		}
		return mCurrentMedia.getState();
	}

	/**
	 * Release the resources used by the model.
	 */
	public void release() {
		for (MediaProvider p : mMediaSources.values()) {
			p.release();
		}
	}

	/**
	 * @return The set of available quality levels.
	 */
	public QualityLevel[] getQualityLevels() {
		if (getState() == PlayerState.IDLE) {
			return new QualityLevel[0];
		}
		return mCurrentMedia.getQualityManager().getLevels();
	}

	/**
	 * @return The index of the current quality level.
	 */
	public int getCurrentQuality() {
		if (getState() == PlayerState.IDLE) {
			return 0;
		}
		return mCurrentMedia.getQualityManager().getCurrentQualityIndex();
	}

	/**
	 * @param index
	 *            The index of the desired quality level.
	 */
	public void setCurrentQuality(int index) {
		if (getState() == PlayerState.IDLE) {
			return;
		}
		mCurrentMedia.getQualityManager().setCurrentQualityIndex(index);
	}
}
