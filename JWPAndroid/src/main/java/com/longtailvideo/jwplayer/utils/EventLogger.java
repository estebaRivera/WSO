package com.longtailvideo.jwplayer.utils;

import android.util.Log;

import com.longtailvideo.jwplayer.events.Event;
import com.longtailvideo.jwplayer.events.EventForwarder;
import com.longtailvideo.jwplayer.events.EventListener;

/**
 * Log incoming events to the Android log.
 * 
 * Use forwardEvent calls to listen to events.
 * 
 * @author tom
 */
public class EventLogger extends EventForwarder {
	private static final String TAG = "JWPlayerEvent";

	/**
	 * @param severity
	 *            What severity should be used for logging. Should be one of the
	 *            constants of the Log class.
	 * @param tag
	 *            The tag to use in the log messages for the events.
	 */
	public EventLogger(final int severity, final String tag) {
		addGlobalEventListener(new EventListener<Event>() {
			@Override
			public void handle(Event event) {
				switch (severity) {
				case Log.DEBUG:
					Log.d(tag, event.toString());
					break;
				case Log.ERROR:
					Log.e(tag, event.toString());
					break;
				case Log.WARN:
					Log.w(tag, event.toString());
					break;
				case Log.VERBOSE:
					Log.v(tag, event.toString());
					break;
				case Log.INFO:
				default:
					Log.i(tag, event.toString());
					break;
				}
			}
		});
	}

	/**
	 * @param severity
	 *            What severity should be used for logging. Should be one of the
	 *            constants of the Log class.
	 */
	public EventLogger(final int severity) {
		this(severity, TAG);
	}

	/**
	 * 
	 */
	public EventLogger() {
		this(Log.INFO);
	}
}
