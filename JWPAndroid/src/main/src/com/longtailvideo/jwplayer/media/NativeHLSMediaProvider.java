package com.longtailvideo.jwplayer.media;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.text.ParseException;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;

import com.longtailvideo.jwplayer.JWPlayer;
import com.longtailvideo.jwplayer.JWPlayer.PlayerState;
import com.longtailvideo.jwplayer.JWPlayer.QualityLevel;
import com.longtailvideo.jwplayer.events.Event;
import com.longtailvideo.jwplayer.events.EventListener;
import com.longtailvideo.jwplayer.events.MediaEvent;
import com.longtailvideo.jwplayer.events.QualityEvent;
import com.longtailvideo.jwplayer.media.adaptive.FragmentProvider;
import com.longtailvideo.jwplayer.media.adaptive.HLSQualityManager;
import com.longtailvideo.jwplayer.media.adaptive.manifest.Level;
import com.longtailvideo.jwplayer.media.adaptive.manifest.ManifestParser;
import com.longtailvideo.jwplayer.media.streaming.MediaStreamer;
import com.longtailvideo.jwplayer.media.streaming.MediaStreamer.OnConnectHandler;
import com.longtailvideo.jwplayer.model.Model;
import com.longtailvideo.jwplayer.utils.UrlUtils;

/**
 * A media provider supporting HLS streams using Android's native HLS playback.
 * Does not work with Android 2.3.
 * 
 * Events: See VideoMediaProvider.
 * 
 * @author tom
 */
public class NativeHLSMediaProvider extends VideoMediaProvider implements
		OnConnectHandler {
	private static final String TAG = "NativeHLSMediaProvider";

	/**
	 * The server delivering the manifest to the media player.
	 */
	protected MediaStreamer mMediaStreamer;

	// Whether the manifest is loaded yet.
	// Used for synchronization of the main thread with the streaming thread.
	private boolean mManifestLoaded;

	// The provider which keeps track of what fragment should be played next.
	protected FragmentProvider mFragmentProvider;

	// The URL of the manifest
	private String mManifestUrl = null;

	// Whether to ignore time updates
	// Used for preventing PLAYING state
	private boolean mIgnoreTimeUpdate = false;

	/**
	 * @param player
	 *            The player of which this media provider is a part.
	 * @param model
	 *            The model to which this provider belongs.
	 */
	public NativeHLSMediaProvider(JWPlayer player, Model model) {
		super(player, model);
		mMediaStreamer = new MediaStreamer("audio/x-mpegurl", "manifest.m3u8");
		mMediaStreamer.setOnConnectHandler(this);
	}

	@Override
	public void load(String url) {
		setupMediaPlayer();

		mMediaStreamer.start();
		setDataSource(mMediaStreamer.getUrl());

		mQualityManager.setDefault();

		prepareManifest(url);
		prepareMediaPlayer(true);
		dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_LOADED));
		mLoaded = true;
	}

	/**
	 * Read the manifest and prepare the player.
	 * 
	 * @param url
	 *            The url of the requested manifest.
	 */
	private void prepareManifest(final String url) {
		mManifestLoaded = false;
		mFragmentProvider.reset();

		// Manifest is parsed and proxied.
		// Android's own manifest parsing has some issues.
		// For example, the last line of a manifest is not read unless it is
		// empty.
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					mManifestUrl = url;
					// Download the manifest
					ManifestParser parser = new ManifestParser();
					HttpClient http = new DefaultHttpClient();
					HttpResponse response = http.execute(new HttpGet(url));
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode != 200) {
						mIgnoreNextPlayerError = true;
						error(MediaEvent.Error.ERROR_FILE_NOT_FOUND,
								"HTTP status code is invalid. Expected 200, got "
										+ statusCode);
						return;
					}
					// Parse the manifest
					parser.setBaseUrl(UrlUtils.getBaseUrl(url));
					parser.parse(response.getEntity().getContent());

					if (parser.hasLevels()) {
						// If this is a multibitrate stream, load all levels
						Iterable<Level> levels = parser.getLevels();
						for (Level l : levels) {
							response = http.execute(new HttpGet(l.getUrl()));

							parser.setBaseUrl(UrlUtils.getBaseUrl(l.getUrl()));
							parser.parse(response.getEntity().getContent());
							mFragmentProvider.loadManifest(l, parser);
						}
						((HLSQualityManager) mQualityManager)
								.setManifestLevels(levels);
						QualityLevel qLevel = mQualityManager.getCurrentQuality();
						Level activeLevel = null;
						if (qLevel.isAuto()) {
							Set<Level> levelSet = mFragmentProvider.getLevels();
							// Take the lowest level without just sound for now
							for (Level l : levelSet) {
								if (!l.isAudio() && l.getUrl().length() > 0) {
									Log.i(TAG, "Using level " + l);
									activeLevel = l;
									break;
								}
							}
						} else {
							activeLevel = ((HLSQualityManager) mQualityManager).getCurrentManifestLevel();
						}

						mManifestUrl = activeLevel.getUrl();
					}

				}
				catch (IllegalStateException e) {
					mIgnoreNextPlayerError = true;
					error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
					return;
				}
				catch (UnknownHostException e) {
					mIgnoreNextPlayerError = true;
					error(MediaEvent.Error.ERROR_FILE_NOT_FOUND,
							"Could not connect to host.");
					return;
				}
				catch (Exception e) {
					// IOException | ParseException
					mIgnoreNextPlayerError = true;
					error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
							"Could not load manifest: "
									+ e.getLocalizedMessage());
					return;
				}
				finally {
					mManifestLoaded = true;
					synchronized (NativeHLSMediaProvider.this) {
						NativeHLSMediaProvider.this.notifyAll();
					}
				}
			}
		}).start();
	}
	
	@Override
	protected void setupQualityManager() {
		mFragmentProvider = new FragmentProvider();
		mQualityManager = new HLSQualityManager(mFragmentProvider);
		mQualityManager.addGlobalEventListener(new EventListener<Event>() {
			@Override
			public void handle(Event event) {
				NativeHLSMediaProvider.this.dispatchEvent(event);
			}
		});
		mQualityManager.addEventListener(QualityEvent.JWPLAYER_QUALITY_CHANGE,
			new EventListener<QualityEvent>() {
				@Override
				public void handle(QualityEvent event) {
					// The fragment provider already knows of the new level
					// We just need to refresh the image
					Log.v(TAG, "Quality changed by user request. Rebuild the player.");
					rebuildMediaPlayer();
				}
			}
		);
	}

	@Override
	public void onConnect(OutputStream stream) {
		// Wait until the manifest is loaded
		synchronized (this) {
			while (!mManifestLoaded) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					return;
				}
			}
		}
		Log.i(TAG, "Manifest was loaded. Resuming output.");

		// Download the manifest
		ManifestParser parser = new ManifestParser();
		HttpClient http = new DefaultHttpClient();
		HttpResponse response;
		try {
			if (!mQualityManager.getCurrentQuality().isAuto()) {
				mManifestUrl = ((HLSQualityManager) mQualityManager).getCurrentManifestLevel().getUrl();
			}
			
			response = http.execute(new HttpGet(mManifestUrl));
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				mIgnoreNextPlayerError = true;
				error(MediaEvent.Error.ERROR_FILE_NOT_FOUND,
						"HTTP status code is invalid. Expected 200, got "
								+ statusCode);
				return;
			}
			
			
			// Parse the manifest
			parser.setBaseUrl(UrlUtils.getBaseUrl(mManifestUrl));
			parser.parse(response.getEntity().getContent());
			
			// Live stream?
			if (!parser.isFinal()) {
				MediaEvent evt = new MediaEvent(MediaEvent.JWPLAYER_MEDIA_META);
				evt.setMetadata(new JWPlayer.Metadata.Stream(true));
				dispatchEvent(evt);
			}
			
			
			// Stream the manifest contents
			stream.write(parser.toString().getBytes());
		}
		catch (ClosedByInterruptException e) {
			// If the thread was interrupted, stop streaming.
			return;
		}
		catch (InterruptedIOException e) {
			// If the thread was interrupted, stop streaming.
			return;
		}
		catch (SocketException e) {
			// Connection was broken
			// The player was probably reset.
			Log.v(TAG, "Connection to player was broken.");
			return;
		}
		catch (ParseException e) {
			error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
			return;
		}
		catch (IOException e) {
			error(MediaEvent.Error.ERROR_FILE_NOT_FOUND, e);
			return;
		}
	}

	@Override
	public void setDisplay(SurfaceHolder holder) {
		/*
		 * Pre: true
		 */
		if (holder == null) {
			mIgnoreTimeUpdate = true;
			mDisplay = null;
			return;
		}
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
			if (isAndroidPlayerStateIn(AndroidPlayerState.IDLE,
					AndroidPlayerState.INITIALIZED, AndroidPlayerState.STOPPED,
					AndroidPlayerState.PREPARING, AndroidPlayerState.PREPARED)) {
				// Should work directly
				Log.v(TAG, "Setting display directly.");
				mMediaPlayer.setDisplay(holder);
				mMediaPlayer.setScreenOnWhilePlaying(true);
				return;
			}
			// If all else fails, rebuild the whole thing
			Log.v(TAG,
					"Rebuilding, because display was set while player is in state "
							+ getAndroidPlayerState());
			rebuildMediaPlayer();

			// Prevent output buffer flush timeouts by pausing the video
			if (isAndroidPlayerStateIn(AndroidPlayerState.PAUSED)
					&& getState() == PlayerState.PLAYING) {
				mMediaPlayer.start();
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
	@Override
	protected void resetMediaPlayer() {
		/*
		 * Pre: true
		 */
		if (mMediaPlayer == null
				|| isAndroidPlayerStateIn(AndroidPlayerState.END)) {
			mMediaPlayer = null;
			return;
		}

		mFragmentProvider.reset();
		
		mPositionWorker.stop();
		mIgnoreTimeUpdate = false;

		// OnPrepared callback is broken after reset, so always use release.
		Log.v(TAG, "Releasing player instead of resetting.");
		mMediaPlayer.release();
		Log.v(TAG, "Released");
		mMediaPlayer = null;
		setAndroidPlayerState(AndroidPlayerState.END);
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.END)
		 */
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// Prepared handler is fired almost immediately after prepareAsync.
		// Dimensions etc. are still unknown.
		setAndroidPlayerState(AndroidPlayerState.PREPARED);
		setDisplay(mDisplay);
		mPrevPosition = getPosition();
		mPositionUnchanged = 0;
		// Immediately play, unless the user request a pause.
		if (!mPauseWhenBuffered) {
			play(false);
		}
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.STARTED,
		 * AndroidPlayerState.BUFFERING, AndroidPlayerState.PAUSED)
		 */
	}

	@Override
	protected void updatePosition() {
		if (!mIgnoreTimeUpdate) {
			// Seek does not work from onPrepared, so we do it after the first
			// time update, without leaving BUFFERING state.
			if (isAndroidPlayerStateIn(AndroidPlayerState.STARTED)
					&& getPosition() > 0 && mSeekWhenPrepared >= 0) {
				Log.e(TAG, "Position: " + getPosition());
				mMediaPlayer.seekTo(mSeekWhenPrepared);
				mSeekWhenPrepared = -1;
			}

			super.updatePosition();
		}
	}
}
