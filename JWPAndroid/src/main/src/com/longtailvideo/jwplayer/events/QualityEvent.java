package com.longtailvideo.jwplayer.events;

import com.longtailvideo.jwplayer.JWPlayer.QualityLevel;

/**
 * An event related to playback quality.
 * 
 * @author tom
 */
public class QualityEvent extends MediaEvent {
	/**
	 * The following properties should be defined for the mediaBuffer event:
	 * 
	 * currentLevel: The new quality level.
	 */
	public static final String JWPLAYER_QUALITY_CHANGE = "jwplayerQualityChange";

	/**
	 * The following properties should be defined for the mediaBuffer event:
	 * 
	 * levels: The list of current quality levels.
	 */
	public static final String JWPLAYER_QUALITY_LEVELS = "jwPlayerQualityLevels";

	private QualityLevel mLevel;
	private QualityLevel[] mLevels;

	/**
	 * @param type
	 *            The event type.
	 */
	public QualityEvent(String type) {
		super(type);
	}

	/**
	 * @return The current quality level.
	 */
	public QualityLevel getCurrentLevel() {
		return mLevel;
	}

	/**
	 * @param level
	 *            The current quality level.
	 */
	public void setCurrentLevel(QualityLevel level) {
		mLevel = level;
	}

	/**
	 * @return The current list of quality levels.
	 */
	public QualityLevel[] getLevels() {
		return mLevels;
	}

	/**
	 * @param levels
	 *            The current list of quality levels.
	 */
	public void setLevels(QualityLevel[] levels) {
		mLevels = levels;
	}

	@Override
	protected void appendDataString(StringBuilder builder) {
		super.appendDataString(builder);
		if (mLevel != null) {
			builder.append(" level=").append(mLevel);
		}
		if (mLevels != null) {
			builder.append(" levels=");
			boolean first = true;
			for (QualityLevel l : mLevels) {
				if (!first) {
					builder.append(',');
				}
				first = false;
				builder.append('"').append(l).append('"');
			}
		}
	}
}
