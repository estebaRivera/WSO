package com.longtailvideo.jwplayer.view;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * An object which fades a view in and out as desired.
 * 
 * @author tom
 */
public class ViewFader {
	/**
	 * Interface to be implemented by a view which allows alpha changes.
	 * 
	 * Used because old Android API versions do not support alpha blending of
	 * views.
	 * 
	 * @author tom
	 */
	public interface AlphaView {
		/**
		 * @param alpha
		 *            The alpha value of the view (0 <= alpha < 256)
		 */
		public void setAlpha(int alpha);

		/**
		 * Execute an action on the UI thread.
		 * 
		 * @param action
		 *            The action to execute.
		 * @param delayMillis
		 *            The delay before executing the action, in milliseconds.
		 * @return Whether posting was successful.
		 */
		public boolean postDelayed(Runnable action, long delayMillis);

		/**
		 * @param visibility
		 *            The visibility of the view.
		 */
		public void setVisibility(int visibility);
	}

	/**
	 * A listener which gets called when the view changes visibility.
	 * 
	 * @author tom
	 */
	public interface OnVisibleListener {
		/**
		 * @param visible
		 *            Whether the view is visible.
		 */
		public void onVisible(boolean visible);
	}

	// The view to fade in and out
	private final AlphaView mView;

	// The counter which determines whether to execute a fade-in
	// Increasing the counter cancels all previous fade-in commands.
	private int mFadeInCounter;

	// The counter which determines whether to execute a fade-out
	// Increasing the counter cancels all previous fade-out commands.
	private int mFadeOutCounter;

	// The alpha value of the view (0 <= mAlpha < 256)
	private int mAlpha = 255;

	// Animation parameters
	private final int mTimeStep;
	private final int mStepSize;

	// The interpolator to use for the animation
	private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

	// The listener which gets called when the view changes visibility.
	OnVisibleListener mOnVisibleListener = null;

	/**
	 * @param view
	 *            The view to fade.
	 * @param timeStep
	 *            The timeStep animation parameter.
	 * @param stepSize
	 *            The stepSize animation parameter.
	 */
	public ViewFader(AlphaView view, int timeStep, int stepSize) {
		mView = view;
		mTimeStep = timeStep;
		mStepSize = stepSize;
	}

	/**
	 * @param view
	 *            The view to fade.
	 */
	public ViewFader(AlphaView view) {
		this(view, 10, 50);
	}

	/**
	 * Make the view visible or invisible without fading.
	 * 
	 * @param visible
	 *            Whether the view should be visible.
	 */
	public synchronized void setVisible(boolean visible) {
		mView.setVisibility(visible ? android.view.View.VISIBLE
				: android.view.View.GONE);
		mView.setAlpha(visible ? 255 : 0);
	}

	/**
	 * Make the view visible or invisible by fading.
	 * 
	 * @param visible
	 *            Whether the view should be visible.
	 * @param delay
	 *            The delay in milliseconds before initiating the fade.
	 * @param resetQueue
	 *            Whether to remove the previous fade events of the same type.
	 */
	public synchronized void fade(final boolean visible, int delay,
			boolean resetQueue) {
		// Reject all previous fades
		final int count;
		if (visible) {
			count = ++mFadeInCounter;
			if (resetQueue) {
				++mFadeOutCounter;
			}
		}
		else {
			count = ++mFadeOutCounter;
			if (resetQueue) {
				++mFadeInCounter;
			}
		}
		mView.postDelayed(new Runnable() {
			@Override
			public void run() {
				if ((visible && mFadeInCounter != count)
						|| (!visible && mFadeOutCounter != count)) {
					// Fade was already rejected
					return;
				}
				if (visible) {
					boolean wasVisible = isVisible();
					mView.setVisibility(android.view.View.VISIBLE);
					mAlpha = Math.min(255, mAlpha + mStepSize);
					mView.setAlpha((int) (mInterpolator
							.getInterpolation(mAlpha / 255f) * 255));
					if (mOnVisibleListener != null && !wasVisible) {
						mOnVisibleListener.onVisible(true);
					}
					if (mAlpha < 255) {
						mView.postDelayed(this, mTimeStep);
					}
				}
				else {
					boolean wasVisible = isVisible();
					mAlpha = Math.max(0, mAlpha - mStepSize);
					mView.setAlpha((int) (mInterpolator
							.getInterpolation(mAlpha / 255f) * 255));
					if (mOnVisibleListener != null && wasVisible
							&& !isVisible()) {
						mOnVisibleListener.onVisible(false);
					}
					if (mAlpha > 0) {
						mView.postDelayed(this, mTimeStep);
					}
					else {
						mView.setVisibility(android.view.View.GONE);
					}
				}
			}
		}, delay);
	}

	/**
	 * Make the view visible or invisible by fading.
	 * 
	 * @param visible
	 *            Whether the view should be visible.
	 * @param resetQueue
	 *            Whether to remove the previous fade events of the same type.
	 */
	public void fade(boolean visible, boolean resetQueue) {
		fade(visible, 0, resetQueue);
	}

	/**
	 * Make the view visible or invisible by fading.
	 * 
	 * @param visible
	 *            Whether the view should be visible.
	 * @param delay
	 *            The delay in milliseconds before initiating the fade.
	 */
	public void fade(boolean visible, int delay) {
		fade(visible, delay, true);
	}

	/**
	 * Make the view visible or invisible by fading.
	 * 
	 * @param visible
	 *            Whether the view should be visible.
	 */
	public void fade(boolean visible) {
		fade(visible, 0);
	}

	/**
	 * @param interpolator
	 *            The parameter for the fade animation.
	 */
	public void setInterpolator(Interpolator interpolator) {
		mInterpolator = interpolator;
	}

	/**
	 * @return Whether the view is visible.
	 */
	public boolean isVisible() {
		return mAlpha > 0;
	}

	/**
	 * @param listener
	 *            The listener which gets called when the view changes
	 *            visibility.
	 */
	public void setOnVisibleListener(OnVisibleListener listener) {
		mOnVisibleListener = listener;
	}
}
