package com.longtailvideo.jwplayer.media.adaptive;

import java.util.ArrayList;

import android.os.Build;

import com.longtailvideo.jwplayer.JWPlayer.QualityLevel;

/**
 * A filter which rejects quality levels based on rules.
 * 
 * @author tom
 */
public abstract class QualityFilter {
	/**
	 * @param levels
	 *            The array of quality levels.
	 * @return The input array, with the rejected levels filtered out.
	 */
	public QualityLevel[] filter(QualityLevel[] levels) {
		ArrayList<QualityLevel> allowed = new ArrayList<QualityLevel>();
		for (QualityLevel l : levels) {
			if (allow(l)) {
				allowed.add(l);
			}
		}
		QualityLevel[] lvls = new QualityLevel[allowed.size()];
		return allowed.toArray(lvls);
	}

	/**
	 * @param level
	 *            The level to evaluate.
	 * @return Whether the given level is allowed.
	 */
	public abstract boolean allow(QualityLevel level);

	/**
	 * Filter which removes audio levels if video levels exist.
	 * 
	 * @author tom
	 */
	public static class AudioFilter extends QualityFilter {
		// True if and only if only audio levels exist
		private boolean mAudioOnly;

		@Override
		public QualityLevel[] filter(QualityLevel[] levels) {
			mAudioOnly = true;
			for (QualityLevel l : levels) {
				mAudioOnly &= l.isAudio();
			}
			return super.filter(levels);
		}

		@Override
		public boolean allow(QualityLevel level) {
			return mAudioOnly || !level.isAudio();
		}
	}

	/**
	 * Filter levels by bitrate, using the Android API level as a base.
	 * 
	 * @author tom
	 */
	public static class APIBitrateFilter extends QualityFilter {
		@Override
		public boolean allow(QualityLevel level) {
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1
					&& level.getBitrate() > 1000000) {
				return false;
			}
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
					&& level.getBitrate() > 1750000) {
				return false;
			}
			if (level.getBitrate() > 2500000) {
				return false;
			}
			return true;
		}
	}
}
