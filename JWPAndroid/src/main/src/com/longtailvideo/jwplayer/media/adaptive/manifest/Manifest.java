package com.longtailvideo.jwplayer.media.adaptive.manifest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An HLS manifest. May contain data about levels or fragments.
 * 
 * @author tom
 */
public class Manifest {
	// The fragments in the manifest, in parsing order
	private List<Frag> mFragments;
	// The levels in the manifest, in parsing order
	private List<Level> mLevels;

	/**
	 * Whether the manifest is completely loaded (VOD or last update for
	 * livestream).
	 */
	protected boolean mFinal = false;

	/**
	 * 
	 */
	public Manifest() {
		clear();
	}

	/**
	 * Clear the level and fragment sets.
	 */
	protected void clear() {
		// Not using the collections' clear function in order to prevent
		// concurrent modifications.
		mFragments = new ArrayList<Frag>();
		mLevels = new ArrayList<Level>();
	}

	/**
	 * Add a fragment to the fragment set.
	 * 
	 * @param fragment
	 *            The fragment to add.
	 */
	protected void addFragment(Frag fragment) {
		mFragments.add(fragment);
	}

	/**
	 * Add a level to the level set.
	 * 
	 * @param level
	 *            The level to add.
	 */
	protected void addLevel(Level level) {
		mLevels.add(level);
	}

	/**
	 * @return The collection of fragments in the manifest.
	 */
	public Iterable<Frag> getFragments() {
		return mFragments;
	}

	/**
	 * @return The collection of levels in the manifest.
	 */
	public Iterable<Level> getLevels() {
		return mLevels;
	}

	/**
	 * @return Whether this manifest contains level information.
	 */
	public boolean hasLevels() {
		return !mLevels.isEmpty();
	}

	/**
	 * @return Whether this manifest contains a fragment list.
	 */
	public boolean hasFragments() {
		return !mFragments.isEmpty();
	}

	/**
	 * @return Whether the manifest contains the end tag.
	 * 
	 *         If this tag is not present, the manifest is part of a sliding
	 *         window.
	 */
	public boolean isFinal() {
		return mFinal;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");
		if (hasFragments()) {
			int maxDuration = 0;
			for (Frag f : mFragments) {
				if (f.getDuration() > maxDuration) {
					maxDuration = f.getDuration();
				}
			}
			s.append("#EXT-X-TARGETDURATION:")
					.append((int) Math.ceil(maxDuration / 1000f)).append("\n");
			s.append("#EXT-X-MEDIA-SEQUENCE:")
					.append(mFragments.get(0).getSequence()).append('\n');
			for (Frag f : mFragments) {
				if (f.isEncrypted()) {
					s.append("#EXT-X-KEY:METHOD=AES-128,URI=\"")
							.append(f.getKeyUrl()).append('"');
					if (f.ivIsCustom()) {
						s.append(",IV=0x");
						for (byte b : f.getIv()) {
							s.append(String.format(Locale.US, "%02x",
									Byte.valueOf(b)));
						}
					}
					s.append('\n');
				}
				s.append("#EXTINF:").append(f.getDuration() / 1000f)
						.append(", no desc\n");
				s.append(f.getUrl()).append('\n');
			}
			if (isFinal()) {
				s.append("#EXT-X-ENDLIST\n");
			}
		}
		if (hasLevels()) {
			for (Level l : mLevels) {
				s.append("#EXT-X-STREAM-INF:");
				s.append("BANDWIDTH=").append(l.getBandwidth());
				s.append(",CODECS=").append("\"mp4a.40.2");
				if (!l.isAudio()) {
					s.append(", avc1.77.21");
				}
				s.append('"');
				if (l.getWidth() >= 0 && l.getHeight() >= 0) {
					s.append(",RESOLUTION=").append(l.getWidth()).append('x')
							.append(l.getHeight());
				}
				s.append('\n');
				s.append(l.getUrl()).append('\n');
			}
		}
		return s.toString();
	}
}
