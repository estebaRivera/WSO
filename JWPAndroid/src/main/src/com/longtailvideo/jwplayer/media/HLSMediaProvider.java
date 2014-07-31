package com.longtailvideo.jwplayer.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import com.longtailvideo.jwplayer.JWPlayer;
import com.longtailvideo.jwplayer.JWPlayer.PlayerState;
import com.longtailvideo.jwplayer.JWPlayer.QualityLevel;
import com.longtailvideo.jwplayer.events.Event;
import com.longtailvideo.jwplayer.events.EventListener;
import com.longtailvideo.jwplayer.events.MediaEvent;
import com.longtailvideo.jwplayer.events.PlayerStateEvent;
import com.longtailvideo.jwplayer.events.QualityEvent;
import com.longtailvideo.jwplayer.media.adaptive.FragmentProvider;
import com.longtailvideo.jwplayer.media.adaptive.HLSQualityManager;
import com.longtailvideo.jwplayer.media.adaptive.manifest.Frag;
import com.longtailvideo.jwplayer.media.adaptive.manifest.Level;
import com.longtailvideo.jwplayer.media.adaptive.manifest.ManifestParser;
import com.longtailvideo.jwplayer.media.streaming.MediaStreamer;
import com.longtailvideo.jwplayer.model.Model;
import com.longtailvideo.jwplayer.utils.UrlUtils;

/**
 * A media provider for http live streams.
 * 
 * Events: See VideoMediaProvider
 * 
 * @author tom
 */
public class HLSMediaProvider extends VideoMediaProvider implements
		MediaStreamer.OnConnectHandler {
	private static final String TAG = "HLSMediaProvider";

	// The cache containing the keys that were downloaded
	// Keys to the map are the URLs of the keyfiles and values are the contents
	// of the keyfiles
	private static Map<String, byte[]> mKeyCache = new HashMap<String, byte[]>();

	/**
	 * The streaming server delivering the data to the media player.
	 */
	protected MediaStreamer mMediaStreamer;

	/**
	 * The provider which keeps track of what fragment should be played next.
	 */
	protected FragmentProvider mFragmentProvider;

	// Whether the manifest is loaded yet.
	// Used for synchronization of the main thread with the streaming thread.
	private boolean mManifestLoaded;

	// Whether the current stream is a live stream
	private boolean mIsLive;

	// Set when the last fragment was a 404
	private boolean mLastFrag404 = false;
	
	// The URL to the manifest
	// Used for completely reloading the manifest
	private String mManifestUrl;

	// The http client used to fetch the fragments
	private HttpClient mFragmentHttp;

	// Maximum number of bytes to send to the player at a time (transfer buffer
	// size)
	private static final int MAX_SEGMENT_SIZE = 100 * 1024;

	private static final int CHECK_BITRATE_TIME = 3000;

	/**
	 * @param player
	 *            The player of which this media provider is a part.
	 * @param model
	 *            The model to which this provider belongs.
	 */
	public HLSMediaProvider(JWPlayer player, Model model) {
		super(player, model);
		mMediaStreamer = new MediaStreamer("application/octet-stream",
				"stream.ts");
		mMediaStreamer.setOnConnectHandler(this);
	}

	@Override
	protected void setupQualityManager() {
		mFragmentProvider = new FragmentProvider();
		mQualityManager = new HLSQualityManager(mFragmentProvider);
		mQualityManager.addGlobalEventListener(new EventListener<Event>() {
			@Override
			public void handle(Event event) {
				HLSMediaProvider.this.dispatchEvent(event);
			}
		});
		mQualityManager.addEventListener(QualityEvent.JWPLAYER_QUALITY_CHANGE,
				new EventListener<QualityEvent>() {
					@Override
					public void handle(QualityEvent event) {
						// The fragment provider already knows of the new level
						// We just need to refresh the image
						Log.v(TAG,
								"Quality changed by user request. Rebuild the player.");
						rebuildMediaPlayer();
					}
				});
	}

	@Override
	public void load(String url) {
		/*
		 * Pre: true
		 */
		stop();
		setupMediaPlayer();

		mMediaStreamer.start();
		setDataSource(mMediaStreamer.getUrl());

		mFragmentHttp = new DefaultHttpClient();
		prepareManifest(url);
		prepareMediaPlayer(true);
		dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_LOADED));
		mLoaded = true;
		/*
		 * Post: mMediaPlayer != null && mAndroidPlayerState ==
		 * AndroidPlayerState.PREPARING && mLoaded
		 */
	}

	@Override
	public void play() {
		/*
		 * Pre: true
		 */

		if (!mLoaded) {
			super.play();
		}

		// Do not pause when buffering is done
		mPauseWhenBuffered = false;

		// Treat livestreams separately.
		if (isAndroidPlayerPrepared() && isLive()
				&& getState() == JWPlayer.PlayerState.PAUSED) {
			// Resuming a livestream restarts it
			mPositionWorker.stop();
			// Reset call is valid, because the player is prepared
			mMediaPlayer.reset();
			setAndroidPlayerState(AndroidPlayerState.IDLE);
			setState(JWPlayer.PlayerState.BUFFERING);

			mFragmentProvider.reset();
			mManifestLoaded = false;
			setDataSource(mMediaStreamer.getUrl());
			if (mDisplay != null) {
				setDisplay(mDisplay);
			}
			prepareMediaPlayer(true);
			// TODO: Reload the levels instead of the manifest
			prepareManifest(mManifestUrl);
		}
		else {
			super.play();
		}

		/*
		 * Post: !mLoaded || androidPlayerIsBuffering() ||
		 * isAndroidPlayerStateIn(AndroidPlayerState.STARTED)
		 */
	}

	@Override
	public void seek(int position) {
		if (mIsLive) {
			// Cannot seek in a livestream
			return;
		}

		fireSeekEvent(position);

		if (!isAndroidPlayerPrepared()) {
			// Delay seeking until the stream is prepared
			mSeekWhenPrepared = position;
			return;
		}

		if (position >= getDuration()) {
			// Seeking after the end of the stream
			dispatchEvent(new MediaEvent(MediaEvent.JWPLAYER_MEDIA_COMPLETE));
			stop();
			return;
		}

		// Pretend we're already preparing / buffering, to stop the stream from
		// playing
		mPositionWorker.stop();
		if (getState() != JWPlayer.PlayerState.BUFFERING) {
			setState(JWPlayer.PlayerState.BUFFERING);
		}
		// Reset call is valid, because the player is not preparing
		mMediaPlayer.reset();
		setAndroidPlayerState(AndroidPlayerState.IDLE);
		// Get the correct fragment set up
		mFragmentProvider.seek(position);

		// Sometimes, the connection breaks unexpectedly.
		// This might be some race condition in the communication with the
		// media player backend.
		// Sleeping for a short while seems to help.
		// This behavior has only been seen on Android 2.3.5 and not on 2.3.7,
		// but because these are different devices, it might not be a version
		// problem.
		// Also, version detection between 2.3.5 and 2.3.7 can not be done
		// nicely; for now, sleep on all SDK 10 or lower devices.
		// TODO: Find out the exact problem.
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
			try {
				Thread.sleep(200);
			}
			catch (InterruptedException e) {
				// Interruption here is not very important.
			}
		}

		// Reset the stream immediately
		setDataSource(mMediaStreamer.getUrl());
		prepareMediaPlayer(true);
	}

	@Override
	public void stop() {
		super.stop();
		mMediaStreamer.stop();
		mFragmentProvider.reset();
	}

	@Override
	protected int getPosition() {
		// The position in the global stream is the position in the local stream
		// + the offset of the local stream, which is the position which
		// resulted from the last seek operation.
		return mFragmentProvider.getLastSeekPosition() + super.getPosition();
	}

	@Override
	protected int getDuration() {
		if (mIsLive) {
			// Livestreams have no known duration
			return -1;
		}
		return mFragmentProvider.getDuration();
	}

	@Override
	public boolean isLive() {
		return mIsLive;
	}

	@Override
	public boolean canSeek() {
		return !isLive();
	}

	@Override
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
		setDataSource(mMediaStreamer.getUrl());
		mFragmentProvider.seek(pos);
		prepareMediaPlayer(!wasPaused);
		mPauseWhenBuffered = wasPaused;
		/*
		 * Post: isAndroidPlayerStateIn(AndroidPlayerState.PREPARING)
		 */
	}

	/**
	 * Read the manifest and prepare the fragment provider.
	 * 
	 * @param url
	 *            The url of the requested manifest.
	 */
	private void prepareManifest(final String url) {
		mManifestLoaded = false;
		mManifestUrl = url;
		mFragmentProvider.reset();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
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
							if (!parser.isFinal()) {
								// Get the duration of the N-2th fragment
								int prevDur = 0, dur = 0;
								for (Frag f : parser.getFragments()) {
									prevDur = dur;
									dur = f.getDuration();
								}
								// Start a level updater
								LevelUpdater updater = new LevelUpdater(l,
										prevDur);
								mModel.addEventListener(
										PlayerStateEvent.JWPLAYER_PLAYER_STATE,
										updater);
								updater.start();
							}
						}
						((HLSQualityManager) mQualityManager)
								.setManifestLevels(levels);
					}
					else {
						// If this is a single bitrate stream, load as a single
						// level
						mFragmentProvider.loadManifest(Level.DEFAULT, parser);
						ArrayList<Level> levels = new ArrayList<Level>();
						levels.add(Level.DEFAULT);
						((HLSQualityManager) mQualityManager)
								.setManifestLevels(levels);
					}

					if (mQualityManager.getLevels().length == 0) {
						error(MediaEvent.Error.ERROR_NO_PLAYABLE_STREAMS,
								"None of the quality levels is playable.");
						return;
					}

					mIsLive = !mFragmentProvider.isFinalized();
					Log.i(TAG, "Live stream: " + mIsLive);

					MediaEvent evt = new MediaEvent(
							MediaEvent.JWPLAYER_MEDIA_META);
					if (mIsLive) {
						evt.setMetadata(new JWPlayer.Metadata.Stream(true));
					}
					else {
						evt.setMetadata(new JWPlayer.Metadata.Stream(
								mFragmentProvider.getDuration()));
					}
					dispatchEvent(evt);

					final QualityLevel activeLevel = mQualityManager
							.getCurrentQuality();
					if (activeLevel != null) {
						mVideoSizeFromStream = activeLevel.getWidth() < 1
								|| activeLevel.getHeight() < 1;
						if (!mVideoSizeFromStream) {
							mPlayer.post(new Runnable() {
								@Override
								public void run() {
									setDimensions(activeLevel.getWidth(),
											activeLevel.getHeight());
								}
							});
						}
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
					e.printStackTrace();
					if (e.getLocalizedMessage() != null) {
						error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
								"Could not load manifest: "
										+ e.getLocalizedMessage());
					}
					else {
						error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
								"Could not load manifest.");
					}
					return;
				}
				finally {
					mManifestLoaded = true;
					synchronized (HLSMediaProvider.this) {
						HLSMediaProvider.this.notifyAll();
					}
				}
			}
		}).start();
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// Ignore, because the player doesn't know the video length
	}

	/**
	 * Dispatch a buffer event.
	 * 
	 * Can be used for debugging.
	 */
	@SuppressWarnings("unused")
	private void updateBuffer() {
		if (!isLive()) {
			MediaEvent evt = new MediaEvent(MediaEvent.JWPLAYER_MEDIA_BUFFER);
			evt.setBufferPercent((int) (mFragmentProvider.getPositionRatio() * 100));
			dispatchEvent(evt);
		}
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

		streamFragments(stream);
	}

	private void streamFragments(OutputStream stream) {
		// While there are more fragments to play or playlists to load
		while (!mFragmentProvider.isFinished()) {
			Frag fragment;
			// Take the next fragment, if available
			while ((fragment = mFragmentProvider.getNextFragment()) != null && !mLastFrag404) {
				Log.v(TAG, "Streaming fragment " + fragment);
				try {
					mLastFrag404 = false;
					streamFragment(stream, fragment);
					// No buffer for HLS
					// updateBuffer();
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
				catch (IOException e) {
					if ("pad block corrupted".equals(e.getMessage())) {
						error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
								e);
						return;
					}
					else {
						Log.i(TAG, "Couldn't load fragment");
						if (mIsLive) {
							// Only track this for live streams; otherwise just move on to the next fragment
							mLastFrag404 = true;
						}
					}
				}
				catch (InvalidKeyException e) {
					error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
					return;
				}

				if (mFragmentProvider.isDiscontinuous()) {
					// Stream is discontinuous here.
					// TODO: Do something with this
					Log.i(TAG, "Discontinuity encountered.");
				}
			}
			synchronized (this) {
				if (!mFragmentProvider.isFinished()) {
					// Wait for a playlist refresh
					try {
						wait(5000);
					}
					catch (InterruptedException e) {
						return;
					}
				}
			}
		}
		Log.v(TAG, "All fragments were served. Closing the connection.");
	}

	/**
	 * Send a number of null packets into the transport stream. Used to fix a
	 * problem where the Android MediaPlayer would stop a stream too early.
	 * 
	 * @param stream
	 *            The stream to which to send the null packets.
	 * @param num
	 *            The number of null packets to send.
	 * @throws IOException
	 *             if an error occurred when sending the packets.
	 */
	private static void streamNullPackets(OutputStream stream, int num)
			throws IOException {
		byte[] nullPacket = new byte[188];
		nullPacket[0] = 0x47; // Sync byte
		nullPacket[1] = 0x1f;
		nullPacket[2] = (byte) 0xff; // 0, 0, 0, 1111111111111
		for (int x = 0; x < num; ++x) {
			stream.write(nullPacket);
			stream.flush();
		}
	}

	/**
	 * Fetch a single fragment and send it into the stream.
	 * 
	 * @param stream
	 *            The stream to which to send the fragment.
	 * @param fragment
	 *            The fragment to fetch
	 * @throws ClientProtocolException
	 *             if an error occurs in the http connection which is used to
	 *             fetch a segment.
	 * @throws IOException
	 *             if a transfer error occurred while fetching the fragment or
	 *             sending the fragment into the stream.
	 * @throws InvalidKeyException
	 */
	@SuppressWarnings("resource")
	private void streamFragment(OutputStream stream, Frag fragment)
			throws IOException, InvalidKeyException {

		// Get the encryption key for the fragment
		byte[] key = null;
		String keyUrl = fragment.getKeyUrl();
		if (keyUrl != null) {
			key = getKey(keyUrl);
		}
		// (key == null) iff the fragment is not encrypted

		// Open an http connection to the fragment data
		HttpResponse resp = null;
		HttpGet request = new HttpGet(fragment.getUrl());
		resp = mFragmentHttp.execute(request);
		synchronized (stream) {
			InputStream is = resp.getEntity().getContent();

			try {
				// Handle encryption by transforming the input stream
				if (key != null) {
					final String transformation = "AES/CBC/PKCS7Padding";

					Log.v(TAG, "Fragment is encrypted.");

					// Encrypted stream
					SecretKeySpec k = new SecretKeySpec(key, transformation);
					Cipher c;
					try {
						c = Cipher.getInstance(transformation);
						c.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(
								fragment.getIv()));
						is = new CipherInputStream(is, c);
					}
					catch (InvalidAlgorithmParameterException e) {
						error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
								e);
						request.abort();
						return;
					}
					catch (NoSuchAlgorithmException e) {
						error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
								e);
						request.abort();
						return;
					}
					catch (NoSuchPaddingException e) {
						error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED,
								e);
						request.abort();
						return;
					}
					catch (InvalidKeyException e) {
						request.abort();
						throw e;
					}
				}

				// First fragment in a multibitrate stream with auto level:
				// Give the quality manager some bitrate info
				// - Download enough of the first fragment (highest quality) to
				// determine bitrate in 3 seconds
				if (mQualityManager.requiresBitrateMeasurement()) {
					Log.i(TAG, "Starting bitrate measurement.");
					long startTime = System.currentTimeMillis();
					Log.v(TAG,
							"Using initial bitrate "
									+ mQualityManager.getCurrentBitrate());
					Log.v(TAG,
							"Buffer size is "
									+ mQualityManager.getCurrentBitrate()
									/ 1000 * CHECK_BITRATE_TIME + " bytes");
					byte[] bitrateBuffer = new byte[mQualityManager
							.getCurrentBitrate() / 1000 * CHECK_BITRATE_TIME];
					int l;
					int c = 0;
					while ((l = is.read(bitrateBuffer, c, MAX_SEGMENT_SIZE)) > 0) {
						c += l;
						if (System.currentTimeMillis() > startTime
								+ CHECK_BITRATE_TIME) {
							break;
						}
					}
					long timeDiff = System.currentTimeMillis() - startTime;
					int bps = (int) (c * timeDiff / 1000);
					Log.i(TAG, "Downloaded " + c + " bytes in " + timeDiff
							+ " milliseconds (" + bps / 1024 + "kbps)");

					// bps is bytes per second -> Use bits per second
					boolean change = mQualityManager
							.setMeasuredBitrate(bps * 8);
					if (change) {
						// Rewind a single fragment
						Log.v(TAG, "Bitrate changed. Rewinding.");
						mFragmentProvider.rewindFragment();
						request.abort();
						return;
					}
					// If we're not changing, feed the read bytes into the media
					// player
					stream.write(bitrateBuffer, 0, c);
					stream.flush();
				}

				// Read segments of 100 KiB at a time into the output stream
				Log.v(TAG, "Sending fragment.");
				byte[] buffer = new byte[MAX_SEGMENT_SIZE];
				int l;
				while ((l = is.read(buffer)) >= 0) {
					stream.write(buffer, 0, l);
					stream.flush();
				}
				Log.v(TAG, "Finished sending fragment.");
			}
			catch (IOException e) {
				request.abort();
				throw e;
			}
			finally {
				try {
					if (!request.isAborted()) {
						is.close();
					}
				}
				catch (SocketException e) {
					Log.w(TAG, "Could not close http stream.");
				}
			}
		}
		stream.flush();
		// For some reason, some HLS streams stop about .6 second
		// early on some android devices (observed on HTC Desire
		// with Cyanogenmod Android 2.3.7).
		// Injecting a stream of null packets resolves this.
		// The desired amount of null packets was determined
		// experimentally.
		// This does not seem to cause any adverse effects anywhere.
		streamNullPackets(stream, 2000);
	}

	/**
	 * Get the AES-128 key from a URL. This is done synchronously.
	 * 
	 * @param keyUrl
	 *            The URL to the keyfile.
	 * @return The key as a byte array.
	 * @throws ClientProtocolException
	 *             if an HTTP error occurred.
	 * @throws IOException
	 *             if an error occurred while communicating with the remote
	 *             server.
	 * @throws InvalidKeyException
	 *             if the keyfile is invalid.
	 */
	private byte[] getKey(String keyUrl) throws ClientProtocolException,
			IOException, InvalidKeyException {
		// First try to get the key from the cache
		byte[] key = mKeyCache.get(keyUrl);
		if (key != null) {
			return key;
		}
		HttpResponse resp = mFragmentHttp.execute(new HttpGet(keyUrl));

		key = new byte[16];
		InputStream content = resp.getEntity().getContent();
		try {
			// Sanity check on key
			int c = content.read(key);
			if (c != 16) {
				throw new InvalidKeyException("Key is shorter than 16 bytes.");
			}
			if (content.available() > 0) {
				throw new InvalidKeyException("Key is longer than 16 bytes.");
			}
		}
		finally {
			content.close();
		}

		mKeyCache.put(keyUrl, key);
		return key;
	}

	@Override
	public void resetMediaPlayer() {
		super.resetMediaPlayer();
		mLastFrag404 = false;
	}
	

	/**
	 * Worker which periodically updates the playlist for a certain level.
	 * 
	 * @author tom
	 */
	private class LevelUpdater extends Thread implements
			EventListener<PlayerStateEvent> {
		// The level for which to update the playlist
		private final Level mLevel;
		private final HttpClient mHttp = new DefaultHttpClient();
		private int mDelay;

		public LevelUpdater(Level level, int initialDelay) {
			mLevel = level;
			mDelay = initialDelay;
		}

		@Override
		public void handle(PlayerStateEvent event) {
			if (event.getNewState() == PlayerState.BUFFERING
					|| event.getNewState() == PlayerState.PLAYING) {
				return;
			}
			removeEventListener(PlayerStateEvent.JWPLAYER_PLAYER_STATE, this);
			interrupt();
		}

		
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(mDelay);

					if (mLastFrag404) {
						rebuildMediaPlayer();
						//mFragmentProvider.rewindFragment();
					}
					
					HttpResponse response = mHttp.execute(new HttpGet(mLevel
							.getUrl()));
					synchronized (mFragmentProvider) {

						ManifestParser parser = new ManifestParser();
						parser.setBaseUrl(UrlUtils.getBaseUrl(mLevel.getUrl()));
						parser.parse(response.getEntity().getContent());
						mDelay = mFragmentProvider.updateFromManifest(mLevel,
								parser);

						if (mDelay < 1000) {
							// Wait at least a second
							mDelay = 1000;
						}
						synchronized (HLSMediaProvider.this) {
							HLSMediaProvider.this.notify();
						}
						if (parser.isFinal()) {
							removeEventListener(
									PlayerStateEvent.JWPLAYER_PLAYER_STATE,
									this);
							return;
						}
					}
				}
				catch (ClosedByInterruptException e) {
					Log.v(TAG, "Stopping level updater for level " + mLevel);
					return;
				}
				catch (InterruptedException e) {
					Log.v(TAG, "Stopping level updater for level " + mLevel);
					return;
				}
				catch (ClientProtocolException e) {
					error(MediaEvent.Error.ERROR_FILE_NOT_FOUND, e);
				}
				catch (IOException e) {
					error(MediaEvent.Error.ERROR_FILE_NOT_FOUND, e);
				}
				catch (IllegalStateException e) {
					error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
				}
				catch (ParseException e) {
					error(MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED, e);
				}
			}
		}
	}
}
