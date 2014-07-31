package com.longtailvideo.jwplayer.events;

/**
 * A class which listens to a set of events and dispatches them to its own set
 * of listeners.
 * 
 * @author tom
 */
public class EventForwarder extends EventDispatcher implements
		EventListener<Event> {
	/**
	 * Start forwarding events of a certain type from a certain source.
	 * 
	 * @param type
	 *            The event type to forward.
	 * @param source
	 *            The source of the events to forward.
	 */
	public void startForward(String type, IEventDispatcher source) {
		source.addEventListener(type, this);
	}

	/**
	 * Start forwarding all events from a certain source.
	 * 
	 * @param source
	 *            The source of the events to forward.
	 */
	public void startForwardGlobal(IEventDispatcher source) {
		source.addGlobalEventListener(this);
	}

	/**
	 * Stop forwarding events of a certain type from a certain source. Note that
	 * this does not stop forwarding events if the forwarding was started using
	 * startForwardGlobal.
	 * 
	 * @param type
	 *            The event type that is forwarded.
	 * @param source
	 *            The source of the events.
	 */
	public void stopForward(String type, IEventDispatcher source) {
		source.removeEventListener(type, this);
	}

	/**
	 * Stop forwarding all events from a certain source. Note that this does not
	 * stop forwarding events if the forwarding was started using startForward.
	 * 
	 * @param source
	 *            The source of the events.
	 */
	public void stopForwardGlobal(IEventDispatcher source) {
		source.removeGlobalEventListener(this);
	}

	@Override
	public void handle(Event event) {
		dispatchEvent(event);
	}
}
