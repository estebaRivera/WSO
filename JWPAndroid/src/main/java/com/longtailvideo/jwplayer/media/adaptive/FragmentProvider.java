package com.longtailvideo.jwplayer.media.adaptive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.longtailvideo.jwplayer.media.adaptive.HLSQualityManager.LevelSwitcher;
import com.longtailvideo.jwplayer.media.adaptive.manifest.Frag;
import com.longtailvideo.jwplayer.media.adaptive.manifest.Level;
import com.longtailvideo.jwplayer.media.adaptive.manifest.Manifest;

/**
 * A class which keeps track of levels and fragments, and delivers the correct
 * fragment information for the stream.
 * 
 * Also keeps track of stream continuity.
 * 
 * @author tom
 */
public class FragmentProvider implements LevelSwitcher {
	private static final String TAG = "FragmentProvider";

	// The list of fragments in the current level
	private LevelContents mCurrentFragments;
	// The map from level to fragments
	// Used because Level objects do not contain fragment information
	private final Map<Level, LevelContents> mFragmentMap;
	// The index of the fragment which should be played next
	private int mFragmentIndex = -1;
	// The current discontinuity level
	private int mDiscontinuity = -1;
	// The last position (in milliseconds) which was seeked to.
	// This is not necessarily equal to the requested seek position.
	private int mLastSeekPosition;

	/**
	 * 
	 */
	public FragmentProvider() {
		mFragmentMap = new HashMap<Level, LevelContents>();
		mCurrentFragments = new LevelContents();
	}

	/**
	 * Reset the fragment provider. Removes all loaded fragments and seeks to
	 * position 0.
	 */
	public void reset() {
		mCurrentFragments = new LevelContents();
		mFragmentMap.clear();
		mFragmentIndex = -1;
		mDiscontinuity = -1;
		mLastSeekPosition = 0;
	}

	/**
	 * Load fragment data from a manifest for a certain quality level.
	 * 
	 * @param level
	 *            The quality level
	 * @param manifest
	 *            The manifest containing the fragment data for the specified
	 *            level
	 */
	public synchronized void loadManifest(Level level, Manifest manifest) {
		LevelContents fragments = new LevelContents();
		if (manifest.isFinal()) {
			fragments.setFinished(true);
		}
		for (Frag f : manifest.getFragments()) {
			fragments.add(f);
		}
		mFragmentMap.put(level, fragments);
	}

	/**
	 * Update the fragments from a refreshed manifest.
	 * 
	 * @param level
	 *            The level to update.
	 * @param manifest
	 *            The refreshed manifest.
	 * @return The duration of the first new fragment. Can be used for timing
	 *         the next refresh. If no fragment was added, equal to 0.
	 */
	public synchronized int updateFromManifest(Level level, Manifest manifest) {
		LevelContents fragments = mFragmentMap.get(level);
		// If the level did not exist yet, load it.
		if (fragments == null) {
			loadManifest(level, manifest);
			LevelContents frags = mFragmentMap.get(level);
			if (frags.size() < 1) {
				return 0;
			}
			return frags.get(0).getDuration();
		}
		// If this manifest VOD or the last of a live stream, note it.
		if (manifest.isFinal()) {
			fragments.setFinished(true);
		}
		// Merge the old and new lists of fragments
		Iterable<Frag> newFragments = manifest.getFragments();
		// Whether we are inserting items
		boolean atEnd = false;
		int prevDiscontinuity = 0;
		int delay = 0;
		for (Frag f : newFragments) {
			if (!atEnd) {
				if (!fragments.contains(f)) {
					atEnd = true;
					delay = f.getDuration();
				}
			}
			if (atEnd) {
				Frag lastFragment = fragments.get(fragments.size() - 1);
				int newDiscontinuity = lastFragment.getDiscontinuity();
				if (f.getDiscontinuity() != prevDiscontinuity) {
					++newDiscontinuity;
				}
				f.setDiscontinuity(newDiscontinuity);
				fragments.add(f);
				Log.v(TAG, "Fragment " + f + " was added.");
			}
			prevDiscontinuity = f.getDiscontinuity();
		}
		return delay;
	}

	/**
	 * @return The set of levels for which fragments can be provided.
	 */
	public synchronized Set<Level> getLevels() {
		return mFragmentMap.keySet();
	}

	/**
	 * @param level
	 *            The quality level to switch to
	 */
	@Override
	public void setLevel(Level level) {
		Log.i(TAG, "Setting level to " + level);
		mCurrentFragments = mFragmentMap.get(level);
		// TODO: Get the correct index
		// Mark stream as discontinuous.
		// Switching levels probably requires a complete stream restart in most
		// cases though.
		mDiscontinuity = -1;
	}

	/**
	 * @return Whether the stream is discontinuous between the previous and next
	 *         segment.
	 */
	public synchronized boolean isDiscontinuous() {
		if (mCurrentFragments == null) {
			return false;
		}
		if (mFragmentIndex >= mCurrentFragments.size()) {
			return false;
		}
		return mCurrentFragments.get(mFragmentIndex).getDiscontinuity() != mDiscontinuity;
	}

	/**
	 * @return The duration of the currently playing stream in milliseconds.
	 */
	public synchronized int getDuration() {
		int d = 0;
		for (Frag f : mCurrentFragments) {
			d += f.getDuration();
		}
		return d;
	}

	/**
	 * @return The resulting stream position of the last seek operation. Note
	 *         that this is not necessarily equal to the value of the position
	 *         parameter in the last seek operation.
	 * 
	 *         Used for determining the current position in the complete stream.
	 */
	public synchronized int getLastSeekPosition() {
		return mLastSeekPosition;
	}

	/**
	 * @return Whether fragment loading for the current level has finished.
	 */
	public synchronized boolean isFinished() {
		if (mCurrentFragments == null) {
			Log.w(TAG, "No fragments available.");
			return true;
		}
		if (mFragmentIndex >= mCurrentFragments.size()) {
			// Are there more manifests than the ones we have?
			return isFinalized();
		}
		Log.v(TAG, "Need to load new playlist.");
		return false;
	}

	/**
	 * @return Whether all manifests for the stream were loaded.
	 */
	public synchronized boolean isFinalized() {
		return mCurrentFragments.isFinished();
	}

	/**
	 * @return The next fragment in the stream
	 */
	public synchronized Frag getNextFragment() {
		if (mFragmentIndex < 0 && !mCurrentFragments.isFinished()) {
			// Live streams start at fragment N - 2
			mFragmentIndex = mCurrentFragments.size() - 2;
		}
		if (mFragmentIndex < 0) {
			mFragmentIndex = 0;
		}
		if (mCurrentFragments == null) {
			return null;
		}
		if (mFragmentIndex >= mCurrentFragments.size()) {
			return null;
		}
		Frag f = mCurrentFragments.get(mFragmentIndex++);
		mDiscontinuity = f.getDiscontinuity();
		return f;
	}

	/**
	 * Seek to the specified position in the stream.
	 * 
	 * @param position
	 *            The position (in milliseconds) in the stream to seek to.
	 */
	public synchronized void seek(int position) {
		if (!mCurrentFragments.isFinished()) {
			// Live streams should start at n - 2
			mFragmentIndex = -1;
			mLastSeekPosition = 0;
			return;
		}
		
		int f = 0;
		int time = 0;
		while ((time += mCurrentFragments.get(f).getDuration()) < position) {
			++f;
		}
		mLastSeekPosition = time - mCurrentFragments.get(f).getDuration();
		// No need to force discontinuity
		mFragmentIndex = f;
	}

	/**
	 * @return The ratio between the index of the current fragment and the total
	 *         number of fragments.
	 */
	public float getPositionRatio() {
		return mFragmentIndex / (float) mCurrentFragments.size();
	}

	/**
	 * Contents of a level. Contains fragment information and whether fragment
	 * loading has finished.
	 * 
	 * @author tom
	 */
	private static class LevelContents extends ArrayList<Frag> {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5987822966275691953L;

		// Whether fragment loading has finished for this level.
		private boolean mFinished = false;

		/**
		 * @return Whether fragment loading has finished for this level.
		 */
		public boolean isFinished() {
			return mFinished;
		}

		/**
		 * @param finished
		 *            Whether fragment loading has finished for this level.
		 */
		public void setFinished(boolean finished) {
			mFinished = finished;
		}
	}

	/**
	 * Rewind by a single fragment.
	 */
	public void rewindFragment() {
		if (mFragmentIndex > 0) {
			--mFragmentIndex;
		}
	}
}
