package com.longtailvideo.jwplayer.media.adaptive.manifest;

// Android Lint complains when we call this a fragment, hence the odd name.

/**
 * A single fragment in a stream.
 * 
 * @author tom
 */
public class Frag implements Comparable<Frag> {
	// The discontinuity level of the fragment
	// When switching from one segment (A) to another (B), the stream is defined
	// to be discontinuous if A's discontinuity level is different from B's.
	private int mDiscontinuity;
	// The approximate playback duration of the fragment
	private final double mDuration;
	// The URL pointing to the fragment data
	private final String mUrl;
	// The URL to the keyfile, or null if the fragment is not encrypted
	private String mKeyUrl;
	// The IV for the AES-128 encryption, or null if the fragment is not
	// encrypted
	private byte[] mIv;
	// The sequence number of the fragment
	private final int mSequence;

	/**
	 * @param sequence
	 *            The sequence number for the fragment
	 * @param duration
	 *            The playback duration of the fragment in seconds
	 * @param url
	 *            The URL pointing to the fragment data
	 * @param discontinuity
	 *            The discontinuity level of the fragment
	 * 
	 *            When switching from one segment (A) to another (B), the stream
	 *            is defined to be discontinuous if A's discontinuity level is
	 *            different from B's.
	 */
	public Frag(int sequence, double duration, String url, int discontinuity) {
		mDuration = duration;
		mUrl = url;
		mDiscontinuity = discontinuity;
		mSequence = sequence;
	}

	/**
	 * Create an encrypted fragment.
	 * 
	 * @param sequence
	 *            The sequence number for the fragment
	 * @param duration
	 *            The playback duration of the fragment in seconds
	 * @param url
	 *            The URL pointing to the fragment data
	 * @param discontinuity
	 *            The discontinuity level of the fragment
	 * 
	 *            When switching from one segment (A) to another (B), the stream
	 *            is defined to be discontinuous if A's discontinuity level is
	 *            different from B's.
	 * @param keyUrl
	 *            The URL pointing to the keyfile
	 * @param iv
	 *            The initialization vector; if null, the IV is generated from
	 *            the sequence number
	 */
	public Frag(int sequence, double duration, String url, int discontinuity,
			String keyUrl, byte[] iv) {
		this(sequence, duration, url, discontinuity);
		mKeyUrl = keyUrl;
		mIv = iv;
	}

	/**
	 * @return The sequence number for this fragment.
	 */
	public int getSequence() {
		return mSequence;
	}

	/**
	 * @return The discontinuity level of the fragment
	 * 
	 *         When switching from one segment (A) to another (B), the stream is
	 *         defined to be discontinuous if A's discontinuity level is
	 *         different from B's.
	 */
	public int getDiscontinuity() {
		return mDiscontinuity;
	}

	/**
	 * @param discontinuity
	 *            The discontinuity level of the fragment
	 * 
	 *            When switching from one segment (A) to another (B), the stream
	 *            is defined to be discontinuous if A's discontinuity level is
	 *            different from B's.
	 */
	public void setDiscontinuity(int discontinuity) {
		mDiscontinuity = discontinuity;
	}

	/**
	 * @return The playback duration of the fragment in milliseconds
	 */
	public int getDuration() {
		return (int) Math.round(mDuration * 1000);
	}

	/**
	 * @return The URL pointing to the fragment data
	 */
	public String getUrl() {
		return mUrl;
	}

	/**
	 * @return The URL to the keyfile for the encryption, or null if this
	 *         fragment is not encrypted.
	 */
	public String getKeyUrl() {
		return mKeyUrl;
	}

	/**
	 * @return The initialization vector for the AES-128 encryption, or null if
	 *         this fragment is not encrypted.
	 */
	public byte[] getIv() {
		// Return null if no key is given (fragment is not encrypted)
		if (mKeyUrl == null) {
			return null;
		}
		// IV was read from the manifest
		if (mIv != null) {
			return mIv;
		}
		// IV must be generated from the sequence number
		byte[] iv = new byte[16];
		iv[12] = (byte) (getSequence() >> 24);
		iv[13] = (byte) (getSequence() >> 16);
		iv[14] = (byte) (getSequence() >> 8);
		iv[15] = (byte) getSequence();
		return iv;
	}

	/**
	 * @return Whether the IV for the encryption is custom.
	 */
	public boolean ivIsCustom() {
		return mIv != null;
	}

	/**
	 * @return Whether this fragment is AES-128 encrypted.
	 */
	public boolean isEncrypted() {
		return mKeyUrl != null;
	}

	@Override
	public int compareTo(Frag another) {
		// Compare by sequence number
		return Integer.valueOf(getSequence()).compareTo(
				Integer.valueOf(another.getSequence()));
	}

	@Override
	public boolean equals(Object o) {
		// Compare by sequence number
		if (!(o instanceof Frag)) {
			return false;
		}
		return ((Frag) o).getSequence() == getSequence();
	}

	@Override
	public int hashCode() {
		// Compare by sequence number
		return Integer.valueOf(mSequence).hashCode();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("[fragment url = ").append(mUrl);
		b.append(" duration = ").append(mDuration);
		b.append(" discontinuity = ").append(mDiscontinuity);
		b.append(" sequence = ").append(mSequence);
		b.append(" encrypted = ").append(isEncrypted()).append(']');
		return b.toString();
	}
}
