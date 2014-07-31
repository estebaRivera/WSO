package com.longtailvideo.jwplayer.utils;

/**
 * A simple worker with polling behavior. While the worker is active, the a tick
 * function is executed at regular intervals.
 * 
 * @author tom
 */
public abstract class PollingWorker implements Runnable {
	// The thread on which the worker is running.
	private Thread mThread;
	// The polling interval in milliseconds
	private final int mInterval;

	/**
	 * @param interval
	 *            The interval between two ticks in milliseconds.
	 * 
	 */
	public PollingWorker(int interval) {
		mInterval = interval;
	}

	/**
	 * Start the worker if it has not yet started.
	 */
	public void start() {
		if (mThread == null) {
			mThread = new Thread(this);
			mThread.start();
		}
	}

	/**
	 * Stop the worker.
	 * 
	 * This call is synchronous, and waits for the actual thread to complete
	 * (should be instantaneous).
	 */
	public synchronized void stop() {
		if (mThread != null) {
			mThread.interrupt();
			mThread = null;
			try {
				wait();
			}
			catch (InterruptedException e) {
				// Just ignore an interruption here
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		// Simply call updatePosition until the worker is told to stop.
		try {
			while (mThread != null) {
				tick();
				try {
					Thread.sleep(mInterval);
				}
				catch (InterruptedException e) {
					// We should be stopping
					return;
				}
			}
		}
		finally {
			synchronized (this) {
				notifyAll();
			}
		}
	}

	/**
	 * The function that is executed at a regular interval.
	 */
	protected abstract void tick();
}
