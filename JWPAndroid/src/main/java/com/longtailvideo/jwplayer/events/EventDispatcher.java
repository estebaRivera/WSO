package com.longtailvideo.jwplayer.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class which dispatches events to a set of listeners.
 * 
 * @author tom
 */
public class EventDispatcher implements IEventDispatcher {
	// The set of listeners per event type
	private final Map<String, Set<EventListener<? extends Event>>> mEventListeners;
	// The set of listeners to each event type.
	private final Set<EventListener<? extends Event>> mGlobalEventListeners;

	/**
	 * 
	 */
	public EventDispatcher() {
		mEventListeners = new HashMap<String, Set<EventListener<? extends Event>>>();
		mGlobalEventListeners = new HashSet<EventListener<? extends Event>>();
	}

	/**
	 * @param type
	 *            The event type to query.
	 * @return The set of objects which listen to the given type.
	 */
	private Set<EventListener<? extends Event>> getListeners(String type) {
		synchronized (mEventListeners) {
			Set<EventListener<? extends Event>> listeners = mEventListeners
					.get(type);
			if (listeners == null) {
				listeners = new HashSet<EventListener<? extends Event>>();
				mEventListeners.put(type, listeners);
			}
			return listeners;
		}
	}

	@Override
	public void addEventListener(String type,
			EventListener<? extends Event> listener) {
		// No type checking whatsoever, because of type erasure and the like.
		// We will simply error out when the event is dispatched.
		synchronized (mEventListeners) {
			Set<EventListener<? extends Event>> listeners = getListeners(type);
			listeners.add(listener);
		}
	}

	@Override
	public void addGlobalEventListener(EventListener<? extends Event> listener) {
		synchronized (mGlobalEventListeners) {
			mGlobalEventListeners.add(listener);
		}
	}

	@Override
	public boolean removeEventListener(String type,
			EventListener<? extends Event> listener) {
		synchronized (mEventListeners) {
			Set<EventListener<? extends Event>> listeners = getListeners(type);
			return listeners.remove(listener);
		}
	}

	@Override
	public boolean removeGlobalEventListener(
			EventListener<? extends Event> listener) {
		synchronized (mGlobalEventListeners) {
			return mGlobalEventListeners.remove(listener);
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void dispatchEvent(Event e) {
		// Copy the current set of handlers, in order to prevent concurrent
		// modifications.
		Set<EventListener<? extends Event>> listeners = new HashSet<EventListener<? extends Event>>();
		synchronized (mEventListeners) {
			for (EventListener<? extends Event> l : getListeners(e.getType())) {
				listeners.add(l);
			}
		}
		synchronized (mGlobalEventListeners) {
			for (EventListener<? extends Event> l : mGlobalEventListeners) {
				listeners.add(l);
			}
		}
		for (EventListener l : listeners) {
			// Use the raw type here, in order to dump e into the listener
			EventListener listener = l;
			// The following should throw an exception if the type parameter to
			// l is wrong
			listener.handle(e);
		}
	}
}
