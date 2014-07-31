package com.longtailvideo.jwplayer.media.adaptive.manifest;

/**
 * A single quality level in a stream.
 * 
 * Note that this object does not hold fragment information.
 * 
 * @author tom
 */
public class Level implements Comparable<Level> {
	/**
	 * The default level, to be used when no level information was provided.
	 */
	public static final Level DEFAULT = new Level(150000, 160, 0, false,
			"Default", "");

	// Whether the level contains only audio
	private final boolean mAudio;
	// The bandwidth of the stream
	private final int mBandwidth;
	// The width and height of the stream
	private final int mHeight, mWidth;
	// The URL of the manifest describing the level
	private final String mUrl;
	// The name of the level, or null if unknown
	private final String mName;

	/**
	 * @param bandwidth
	 *            The bandwidth of the level
	 * @param width
	 *            The width of the video stream in the level, or -1 if unknown
	 * @param height
	 *            The height of the video stream in the level, or -1 if unknown
	 * @param audio
	 *            Whether the level is audio-only
	 * @param name
	 *            The name of the level, or null if unknown
	 * @param url
	 *            The absolute URL to the manifest which contains the fragments
	 *            for this level
	 */
	public Level(int bandwidth, int width, int height, boolean audio,
			String name, String url) {
		mBandwidth = bandwidth;
		mWidth = width;
		mHeight = height;
		mAudio = audio;
		mName = name;
		mUrl = url;
	}

	/**
	 * @return The absolute URL to the manifest which contains the fragments for
	 *         this level
	 */
	public String getUrl() {
		return mUrl;
	}

	/**
	 * @return The bandwidth for this level.
	 */
	public int getBandwidth() {
		return mBandwidth;
	}

	/**
	 * @return The width of the video stream in this level, or -1 if unknown.
	 */
	public int getWidth() {
		return mWidth;
	}

	/**
	 * @return The height of the video stream in this level, or -1 if unknown.
	 */
	public int getHeight() {
		return mHeight;
	}

	/**
	 * @return Whether this is an audio-only level.
	 */
	public boolean isAudio() {
		return mAudio;
	}

	@Override
	public int compareTo(Level other) {
		// Compare two levels.
		// Prefer the ones with video, and prefer higher bitrates.
		// Can be used for sorting.
		if (other.mAudio && !mAudio) {
			return 1;
		}
		return Integer.valueOf(mBandwidth).compareTo(
				Integer.valueOf(other.mBandwidth));
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("[level url = ").append(mUrl);
		b.append(" bandwidth = ").append(mBandwidth);
		b.append(" width = ").append(mWidth);
		b.append(" height = ").append(mHeight);
		b.append(" audio = ").append(mAudio).append(']');
		return b.toString();
	}

	/**
	 * @return The name of the level, or null if unknown.
	 */
	public String getName() {
		return mName;
	}
}
