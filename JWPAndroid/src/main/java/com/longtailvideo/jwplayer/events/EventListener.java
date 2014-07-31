package com.longtailvideo.jwplayer.events;

/**
 * An interface that should be implemented by all event handlers.
 * 
 * @author tom
 * 
 * @param <EventType>
 *            The type (class, not type String) of the event which is listened
 *            to.
 */
public interface EventListener<EventType extends Event> extends
		java.util.EventListener {
	// We inherit from Java's EventListener, which is a tag type.

	/**
	 * Handle the event which is listened to.
	 * 
	 * @param event
	 *            The event to handle.
	 */
	public void handle(EventType event);
}
