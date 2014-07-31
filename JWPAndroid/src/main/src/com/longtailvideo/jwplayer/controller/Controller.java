package com.longtailvideo.jwplayer.controller;

import com.longtailvideo.jwplayer.JWPlayer;
import com.longtailvideo.jwplayer.model.Model;

/**
 * The controller part of the JW Player.
 * 
 * @author tom
 * 
 */
public class Controller {
	// The player of which this the controller
	private final JWPlayer mPlayer;
	// The model of mPlayer
	private final Model mModel;
	// Whether media have been loaded.
	private boolean mMediaLoaded = false;

	/**
	 * @param player
	 *            The player of which this is the controller.
	 * @param model
	 *            The model of the player.
	 */
	public Controller(JWPlayer player, Model model) {
		mPlayer = player;
		mModel = model;
	}

	/**
	 * Load media by URL.
	 * 
	 * @param url
	 *            The URL pointing to the media that should be loaded.
	 * @return True if and only if the operation is successful.
	 */
	public boolean load(String url) {
		stop();
		boolean result = mModel.load(url);
		if (result) {
			mMediaLoaded = true;
		}
		return result;
	}

	/**
	 * Toggle playing state of a video. If playing, pauses. If paused, resumes.
	 * If idle, starts.
	 * 
	 * @return whether the operation was successful.
	 */
	public boolean play() {
		if (!mMediaLoaded) {
			return false;
		}
		switch (mPlayer.getState()) {
		case IDLE:
			mModel.loadItem();
			break;
		case PAUSED:
			play(true);
			break;
		case BUFFERING:
		case PLAYING:
			play(false);
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * Start or suspend playback.
	 * 
	 * @param state
	 *            If true, start playback. If false, pause.
	 * @return whether the operation was successful.
	 */
	public boolean play(boolean state) {
		if (!mMediaLoaded) {
			return !state;
		}
		if (state) {
			switch (mPlayer.getState()) {
			case IDLE:
				mModel.loadItem();
				break;
			case PAUSED:
				mModel.getMedia().play();
				break;
			default:
				break;
			}
		}
		else {
			switch (mPlayer.getState()) {
			case BUFFERING:
			case PLAYING:
				mModel.getMedia().pause();
				break;
			default:
				break;
			}
		}
		return true;
	}

	/**
	 * Toggle playing state of a video. If playing, pauses. If paused, resumes.
	 * If idle, starts.
	 * 
	 * @return whether the operation was successful.
	 */
	public boolean pause() {
		return play();
	}

	/**
	 * Suspend or resume playback.
	 * 
	 * @param state
	 *            If true, pause playback. If false, resume.
	 * @return whether the operation was successful.
	 */
	public boolean pause(boolean state) {
		return play(!state);
	}

	/**
	 * Stop playing the currently playing media.
	 * 
	 * @return True if and only if anything was stopped.
	 */
	public boolean stop() {
		if (mModel.getMedia() != null) {
			switch (mModel.getMedia().getState()) {
			case PLAYING:
			case BUFFERING:
			case PAUSED:
				mModel.getMedia().stop();
				return true;
			default:
				break;
			}
			return false;
		}
		return false;
	}

	/**
	 * Seek the currently playing media to the specified position.
	 * 
	 * @param position
	 *            The position (in milliseconds) to seek to.
	 * @return Whether the operation was successful.
	 */
	public boolean seek(int position) {
		if (!mModel.getMedia().canSeek()) {
			return false;
		}
		switch (mModel.getMedia().getState()) {
		case BUFFERING:
		case PAUSED:
			mModel.getMedia().seek(position);
			play(true);
			return true;
		case PLAYING:
			mModel.getMedia().seek(position);
			return true;
		case IDLE:
			return false;
		default:
			return false;
		}
	}
}
