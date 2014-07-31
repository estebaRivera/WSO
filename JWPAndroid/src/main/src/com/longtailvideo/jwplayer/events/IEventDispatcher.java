package com.longtailvideo.jwplayer.events;

/**
 * An event dispatcher. Sends out events to a set of handlers.
 * 
 * This interface can be implemented if extending the EventDispatcher class is
 * not possible.
 * 
 * @author tom
 */
public interface IEventDispatcher {
	/**
	 * Add a listener to the specified event.
	 * 
	 * @param type
	 *            The event type to listen to.
	 * @param listener
	 *            A listener to the specified event type. The generic parameter
	 *            should be the same as (or an ancestor of) the class containing
	 *            the type parameter as a static final String.
	 */
	public void addEventListener(String type,
			EventListener<? extends Event> listener);

	/**
	 * Add a listener to all events.
	 * 
	 * @param listener
	 *            A listener for the events. Its generic parameter should be an
	 *            ancestor class of all events that may be dispatched ("Event"
	 *            is a good choice).
	 */
	public void addGlobalEventListener(EventListener<? extends Event> listener);

	/**
	 * Remove a listener to a certain event.
	 * 
	 * @param type
	 *            The type of event for which listener is listening.
	 * @param listener
	 *            The listener to remove.
	 * @return True if and only if listener was listening to the specified
	 *         event.
	 */
	public boolean removeEventListener(String type,
			EventListener<? extends Event> listener);

	/**
	 * Remove a global listener.
	 * 
	 * @param listener
	 *            The listener to remove.
	 * @return True if and only if listener was listening globally.
	 */
	public boolean removeGlobalEventListener(
			EventListener<? extends Event> listener);

	/**
	 * Dispatch an event. Causes all listeners for the given event, as well as
	 * all global listeners to execute their event handler.
	 * 
	 * @param e
	 *            The event to dispatch.
	 */
	public void dispatchEvent(Event e);
}
