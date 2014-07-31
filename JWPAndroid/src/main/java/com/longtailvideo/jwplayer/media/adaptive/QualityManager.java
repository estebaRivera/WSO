package com.longtailvideo.jwplayer.media.adaptive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.util.Log;

import com.longtailvideo.jwplayer.JWPlayer.QualityLevel;
import com.longtailvideo.jwplayer.events.EventDispatcher;
import com.longtailvideo.jwplayer.events.QualityEvent;

/**
 * Manager of video qualities. Unifies quality management for all media
 * providers.
 * 
 * Events:
 * 
 * qualityChange: When the quality level changed.
 * 
 * qualityLevels: When the list of quality levels is set.
 * 
 * @author tom
 */
public class QualityManager extends EventDispatcher {
	private static final String TAG = "QualityManager";

	// The set of filters that are used to throw away qualities we do not want
	// to provide
	private static QualityFilter[] sQualityFilters = new QualityFilter[] {
			new QualityFilter.APIBitrateFilter(),
			new QualityFilter.AudioFilter() };

	// The set of levels
	private final List<QualityLevel> mLevels;
	/**
	 * The current level. This is the actual playing level.
	 */
	protected QualityLevel mCurrentLevel;
	// Whether the auto level was chosen.
	private boolean mAuto = true;
	/**
	 * The level which is shown to the user. This includes the auto level.
	 */
	private QualityLevel mProjectedLevel;

	// The last measured bitrate
	private int mMeasuredBitrate = -1;

	/**
	 * 
	 */
	public QualityManager() {
		mLevels = new ArrayList<QualityLevel>();
		mLevels.clear();
		QualityLevel autoLevel = new QualityLevel();
		mLevels.add(autoLevel);
		mCurrentLevel = mProjectedLevel = autoLevel;
	}

	/**
	 * Change the set of quality levels. This operation immediately chooses the
	 * correct quality to load.
	 * 
	 * @param levels
	 *            The set of levels from which to choose the quality.
	 */
	protected void setLevels(Collection<QualityLevel> levels) {
		mLevels.clear();
		QualityLevel autoLevel = new QualityLevel();
		mLevels.add(autoLevel);
		mMeasuredBitrate = -1;

		// Filter the levels
		QualityLevel[] filteredLevels = new QualityLevel[levels.size()];
		filteredLevels = levels.toArray(filteredLevels);
		for (QualityFilter f : sQualityFilters) {
			filteredLevels = f.filter(filteredLevels);
		}

		// Set the levels.
		// Clone so the levels aren't accidentally changed after adding.
		for (QualityLevel l : filteredLevels) {
			mLevels.add((QualityLevel) l.clone());
		}
		// Update current level
		if (!mLevels.contains(mCurrentLevel)) {
			mCurrentLevel = autoLevel;
			mAuto = true;
		}
		Collections.sort(mLevels);

		// Send out the quality event
		QualityEvent evt = new QualityEvent(
				QualityEvent.JWPLAYER_QUALITY_LEVELS);
		evt.setLevels(getLevels());
		dispatchEvent(evt);

		updateLevel(false);
	}

	/**
	 * @return The set of all quality levels.
	 */
	public QualityLevel[] getLevels() {
		switch (mLevels.size()) {
		case 0:
		case 1:
			// Only auto; return empty array
			return new QualityLevel[0];
		case 2: {
			// Only one quality level, do not return auto
			QualityLevel[] levels = new QualityLevel[1];
			levels[0] = mLevels.get(1);
			return levels;
		}
		default: {
			QualityLevel[] levels = new QualityLevel[mLevels.size()];
			int c = 0;
			for (QualityLevel l : mLevels) {
				levels[c++] = (QualityLevel) l.clone();
			}
			return levels;
		}
		}
	}

	/**
	 * @param level
	 *            The quality level to use.
	 */
	protected void setLevel(QualityLevel level) {
		mAuto = level.isAuto();
		if (mLevels.contains(level)) {
			mCurrentLevel = level;
		}
		updateLevel(true);
	}

	/**
	 * @return The current quality level.
	 */
	public QualityLevel getCurrentQuality() {
		// Note: this is the externally shown one.
		// For determining which quality is used in the "Auto" case, use
		// mCurrentLevel directly.
		if (mAuto) {
			return mLevels.get(0);
		}
		return mCurrentLevel;
	}

	/**
	 * @return The index of the current quality.
	 */
	public int getCurrentQualityIndex() {
		if (mAuto) {
			return 0;
		}
		if (mLevels.size() > 2) {
			// Includes auto level
			return mLevels.indexOf(mCurrentLevel);
		}
		// Does not include auto level
		return mLevels.indexOf(mCurrentLevel) - 1;
	}

	/**
	 * @param index
	 *            The index of the desired quality level.
	 */
	public void setCurrentQualityIndex(int index) {
		int idx = index;
		if (mLevels.size() <= 2 && idx >= 0) {
			// In this case, we are suppressing the "auto" level
			++idx;
		}
		if (idx >= mLevels.size() || idx < 0) {
			return;
		}
		setLevel(mLevels.get(idx));
	}

	/**
	 * React to level changes.
	 * 
	 * @param userChoice
	 *            Whether the level change was a consequence of user input, as
	 *            opposed to an automatic switch.
	 */
	protected void updateLevel(boolean userChoice) {
		if ((mAuto && userChoice) || mCurrentLevel.isAuto()) {
			// User chose auto level, or we haven't initialized yet
			if (mMeasuredBitrate < 0) {
				mCurrentLevel = mLevels.get(mLevels.size() - 1);
			}
			else {
				mCurrentLevel = chooseLevel();
			}
		}
		Log.i(TAG, "Selected quality level " + mCurrentLevel);

		if (!mProjectedLevel.equals(getCurrentQuality())) {
			QualityEvent evt = new QualityEvent(
					QualityEvent.JWPLAYER_QUALITY_CHANGE);

			mProjectedLevel = getCurrentQuality();
			evt.setCurrentLevel(mProjectedLevel);
			dispatchEvent(evt);
		}
	}

	/**
	 * Have a single, default quality.
	 */
	public void setDefault() {
		ArrayList<QualityLevel> levels = new ArrayList<QualityLevel>();
		levels.add(new QualityLevel("Default", 150000));
		setLevels(levels);
	}

	/**
	 * @return Whether a bitrate management is required.
	 */
	public boolean requiresBitrateMeasurement() {
		return mLevels.size() > 2 && mMeasuredBitrate < 0;
	}

	/**
	 * @param bps
	 *            The number of bytes per second from the measurement.
	 * @return Whether the quality level switched as a consequence of this.
	 */
	public boolean setMeasuredBitrate(int bps) {
		mMeasuredBitrate = bps;
		return switchIfNeeded();
	}

	/**
	 * @return The bitrate of the current level.
	 */
	public int getCurrentBitrate() {
		return mCurrentLevel.getBitrate();
	}

	/**
	 * Switch to a different level if auto and the we can find a better match
	 * for the bitrate.
	 * 
	 * @return Whether a switch was done.
	 */
	private boolean switchIfNeeded() {
		if (!mAuto || mLevels.size() <= 2) {
			return false;
		}
		if (mMeasuredBitrate >= mCurrentLevel.getBitrate()) {
			return false;
		}
		if (mCurrentLevel == mLevels.get(1)) {
			// Lowest level already
			return false;
		}
		QualityLevel chosenLevel = chooseLevel();
		if (chosenLevel == mCurrentLevel) {
			return false;
		}
		mCurrentLevel = chosenLevel;
		updateLevel(false);
		return true;
	}

	/**
	 * @return Choose the best quality level for the bitrate.
	 */
	private QualityLevel chooseLevel() {
		QualityLevel chosenLevel = mLevels.get(1);
		for (int i = 2; i < mLevels.size(); ++i) {
			QualityLevel level = mLevels.get(i);
			if (mMeasuredBitrate < level.getBitrate()) {
				break;
			}
			chosenLevel = level;
		}
		return chosenLevel;
	}
}
