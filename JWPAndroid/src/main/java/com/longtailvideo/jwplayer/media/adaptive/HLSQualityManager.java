package com.longtailvideo.jwplayer.media.adaptive;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.longtailvideo.jwplayer.JWPlayer.QualityLevel;
import com.longtailvideo.jwplayer.media.adaptive.manifest.Level;

/**
 * Quality manager dealing with HLS quality levels.
 * 
 * @author tom
 */
public class HLSQualityManager extends QualityManager {
	// Map translating Player quality levels into their corresponding HLS
	// manifest level.
	private final Map<QualityLevel, Level> mLevelMap;
	private final LevelSwitcher mLevelSwitcher;

	/**
	 * @param levelSwitcher
	 *            The object reacting to quality level switches.
	 */
	public HLSQualityManager(LevelSwitcher levelSwitcher) {
		mLevelMap = new HashMap<QualityLevel, Level>();
		mLevelSwitcher = levelSwitcher;
	}

	/**
	 * @param levels
	 *            The set of levels from the manifest.
	 */
	public void setManifestLevels(Iterable<Level> levels) {
		mLevelMap.clear();
		Set<QualityLevel> qLevels = new HashSet<QualityLevel>();
		for (Level l : levels) {
			QualityLevel qLevel = toQualityLevel(l);
			mLevelMap.put(qLevel, l);
			qLevels.add(qLevel);
		}
		setLevels(qLevels);
		mLevelSwitcher.setLevel(mLevelMap.get(mCurrentLevel));
	}

	/**
	 * @param level
	 *            The HLS manifest level.
	 * @return The JWPlayer quality level which represents it.
	 */
	private static QualityLevel toQualityLevel(Level level) {
		QualityLevel l;
		if (level.getName() == null) {
			l = new QualityLevel(level.getBandwidth());
		}
		else {
			l = new QualityLevel(level.getName(), level.getBandwidth());
		}
		l.setWidth(level.getWidth());
		l.setHeight(level.getHeight());
		l.setAudio(level.isAudio());
		return l;
	}

	public Level getCurrentManifestLevel() {
		return mLevelMap.get(getCurrentQuality());
	}

	@Override
	public void updateLevel(boolean userChoice) {
		super.updateLevel(userChoice);
		Level l = mLevelMap.get(mCurrentLevel);
		if (l == null) {
			return;
		}
		mLevelSwitcher.setLevel(l);
	}

	/**
	 * A class which implements HLS level switches.
	 * 
	 * @author tom
	 */
	public interface LevelSwitcher {
		/**
		 * @param level
		 *            The new quality level.
		 */
		public void setLevel(Level level);
	}
}
