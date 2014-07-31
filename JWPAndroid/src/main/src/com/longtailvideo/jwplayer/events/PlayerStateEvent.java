package com.longtailvideo.jwplayer.events;

import com.longtailvideo.jwplayer.JWPlayer;

/**
 * An event related to the player state.
 * 
 * @author tom
 */
public class PlayerStateEvent extends PlayerEvent {
	private final JWPlayer.PlayerState mNewState, mOldState;

	/**
	 * The following properties should be defined for the playerState event:
	 * 
	 * oldState: The state before the state change.
	 * 
	 * newState: The state after the state change.
	 */
	public static final String JWPLAYER_PLAYER_STATE = "jwplayerPlayerState";

	/**
	 * @param type
	 *            The type of player state event. Should be one of the static
	 *            final Strings in the class definition of this event class or
	 *            one of its descendant classes.
	 * @param newState
	 *            The state before the state change.
	 * @param oldState
	 *            The state after the state change.
	 */
	public PlayerStateEvent(String type, JWPlayer.PlayerState newState,
			JWPlayer.PlayerState oldState) {
		super(type);
		mNewState = newState;
		mOldState = oldState;
	}

	/**
	 * @return The previous state of the player.
	 */
	public JWPlayer.PlayerState getOldState() {
		return mOldState;
	}

	/**
	 * @return the new state of the player.
	 */
	public JWPlayer.PlayerState getNewState() {
		return mNewState;
	}

	@Override
	protected void appendDataString(StringBuilder builder) {
		// Example [jwplayerPlayerState IDLE -> PLAYING]
		super.appendDataString(builder);
		builder.append(" ").append(mOldState.toString()).append(" -> ")
				.append(mNewState.toString());
	}
}
