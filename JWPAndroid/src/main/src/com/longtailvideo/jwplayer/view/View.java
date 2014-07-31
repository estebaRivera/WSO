package com.longtailvideo.jwplayer.view;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.longtailvideo.jwplayer.JWPlayer;
import com.longtailvideo.jwplayer.JWPlayer.PlayerState;
import com.longtailvideo.jwplayer.R;
import com.longtailvideo.jwplayer.controller.Controller;
import com.longtailvideo.jwplayer.events.Event;
import com.longtailvideo.jwplayer.events.EventDispatcher;
import com.longtailvideo.jwplayer.events.EventListener;
import com.longtailvideo.jwplayer.events.IEventDispatcher;
import com.longtailvideo.jwplayer.events.MediaEvent;
import com.longtailvideo.jwplayer.events.PlayerEvent;
import com.longtailvideo.jwplayer.events.PlayerStateEvent;
import com.longtailvideo.jwplayer.events.ViewEvent;
import com.longtailvideo.jwplayer.model.Model;
import com.longtailvideo.jwplayer.view.ViewFader.AlphaView;

/**
 * The player view. Contains the video view.
 * 
 * Events:
 * 
 * viewFullscreen: When switching to fullscreen or back.
 * 
 * @author tom
 */
@SuppressLint("NewApi")
public class View extends RelativeLayout implements EventListener<PlayerEvent>,
		android.view.View.OnClickListener, IEventDispatcher {
	private static final String TAG = "View";

	private static final int PLAYER_MARGIN = 10;
	
	private final EventDispatcher mEventDispatcher = new EventDispatcher();

	// The model of the player which is presented by the view.
	private final Model mModel;
	// The controller of the player which is presented by the view.
	private final Controller mController;

	// The width and height of the video.
	// Used for enforcing aspect ratio.
	private int mVideoWidth, mVideoHeight;

	// Whether controls are disabled entirely
	private final boolean mControls;

	private JWVideoView mVideoView; 
	// The player icon UI component
	private PlayerIcon mIcon;
	// The control bar UI component
	private ControlBar mControlBar;
	// Whether the player is currently fullscreen.
	private boolean mIsFullscreen = false;
	// The dialog that is used for fullscreen.
	private Dialog mFullScreenDialog;
	// The layout for the fullscreen mode.
	RelativeLayout mFullScreenLayout;
	// Whether we are switching from or to fullscreen.
	// Used to know not to pause the video on display detach.
	private boolean mSwitchingFullscreen = false;
	
	private boolean mHasLogo;
	// The logo
	private ImageView mLogo;
	/**
	 * @param model
	 *            The player model.
	 * @param controller
	 *            The player controller
	 * @param context
	 *            The context for the View
	 * @param controls
	 *            Whether to show controls.
	 */
	public View(Model model, Controller controller, Context context,
			boolean controls) {
		super(context);
		mModel = model;
		mController = controller;
		mControls = controls;
		mHasLogo = LogoConfig.LOGO;
		setupView();
	}

	/**
	 * @param model
	 *            The player model.
	 * @param controller
	 *            The player controller
	 * @param context
	 *            The context for the View
	 * @param attrs
	 *            The attributes for the View
	 * @param controls
	 *            Whether to show controls.
	 */
	public View(Model model, Controller controller, Context context,
			AttributeSet attrs, boolean controls) {
		super(context, attrs);
		mModel = model;
		mController = controller;
		mControls = controls;
		setupView();
	}

	/**
	 * @param model
	 *            The player model.
	 * @param controller
	 *            The player controller
	 * @param context
	 *            The context for the View
	 * @param attrs
	 *            The attributes for the View
	 * @param defStyle
	 *            The default style
	 * @param controls
	 *            Whether to show controls.
	 */
	public View(Model model, Controller controller, Context context,
			AttributeSet attrs, int defStyle, boolean controls) {
		super(context, attrs, defStyle);
		mModel = model;
		mController = controller;
		mControls = controls;
		setupView();
	}

	/**
	 * Setup the complete view initializes the video view and optionally the
	 * controls.
	 */
	@SuppressLint("NewApi")
	private void setupView() {
		removeAllViews();

		setupVideoView();
		addView(mVideoView);

		if (mControls) {
			setupPlayerIcon();
			setupControlBar();
			setupLogo();
			setOnClickListener(this);
		}

		// Add all event listeners
		// Used to show the correct controls, and toggle video visibility.
		// Some of the handlers are unused if controls are disabled, but they
		// are still added for consistency.
		mModel.addEventListener(PlayerStateEvent.JWPLAYER_PLAYER_STATE, this);
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_COMPLETE, this);
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_TIME, this);
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_SEEK, this);
		mModel.addEventListener(PlayerEvent.JWPLAYER_ERROR, this);
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_BUFFER, this);
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_META, this);
		mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_SELECTED, this);

		// Listen for system UI visibility in recent Android versions.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
				@Override
				public void onSystemUiVisibilityChange(int visibility) {
					View.this.onSystemUiVisibilityChange(visibility);
				}
			});
		}

		setFocusable(true);
		requestFocus();
	}

	private void setupVideoView() {
		// Video view is centered in the view, and scaled up as far as possible.
		// Dimensions are done in the video view itself.
		mVideoView = new JWVideoView(getContext());
		@SuppressWarnings("deprecation")
		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.FILL_PARENT);
		params.addRule(CENTER_IN_PARENT);
		mVideoView.setLayoutParams(params);
	}

	private void setupPlayerIcon() {
		// Player icon is sized to wrap the icon graphic and/or error message
		// and centered in the view.
		mIcon = new PlayerIcon(getContext());
		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		params.addRule(CENTER_IN_PARENT);
		params.setMargins(PLAYER_MARGIN, PLAYER_MARGIN, PLAYER_MARGIN, PLAYER_MARGIN);
		mIcon.setLayoutParams(params);
		if (isFullScreen()) {
			mFullScreenLayout.addView(mIcon);
		}
		else {
			addView(mIcon);
		}
	}

	private void setupLogo() {
		
		if (mHasLogo) {
			mLogo = new ImageView(getContext());
			//mLogo.setScaleType(ScaleType.CENTER);
			mLogo.setImageResource(R.drawable.logo);
			LayoutParams params = new LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
			
			params.addRule(ALIGN_PARENT_RIGHT);
			params.rightMargin = params.topMargin = PLAYER_MARGIN;
	
	
			mLogo.setLayoutParams(params);
			if (isFullScreen()) {
				mFullScreenLayout.addView(mLogo);
			} else {
				addView(mLogo);
			}
		}
	}
	private void setupControlBar() {
		// Control bar is scaled horizontally to fit the view and vertically to
		// wrap all elements.
		// It is placed in the bottom of the view and is, by default, invisible.
		mControlBar = new ControlBar(getContext());
		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(PLAYER_MARGIN, PLAYER_MARGIN, PLAYER_MARGIN, PLAYER_MARGIN);
		params.addRule(CENTER_HORIZONTAL);
		params.addRule(ALIGN_PARENT_BOTTOM);
		mControlBar.setLayoutParams(params);
		mControlBar.setVisible(false);
		if (isFullScreen()) {
			mFullScreenLayout.addView(mControlBar);
		}
		else {
			addView(mControlBar);
		}
	}

	private void handleMediaTime(MediaEvent event) {
		if (mControls) {
			// Update the time in the control bar
			mControlBar.updateTime(event.getPosition(), event.getDuration());
		}
	}

	private void handleMediaBuffer(MediaEvent event) {
		if (mControls) {
			// Update the buffer in the control bar
			mControlBar.updateBuffer(event.getBufferPercent());
		}
	}

	private void handleMediaSeek(MediaEvent event) {
		if (mControls) {
			// Update the time in the control bar
			mControlBar.updateTime(event.getOffset());
		}
	}

	private void handleMediaSelected(
			@SuppressWarnings("unused") MediaEvent event) {
		if (mControls) {
			post(new Runnable() {
				@Override
				public void run() {
					// Reset the view
					mControlBar.setLive(false);
					mIcon.setText(null);
					mIcon.setState(PlayerIconState.PLAY);
				}
			});
		}
	}

	private void handlePlayerState(final PlayerStateEvent event) {
		// Update the state in the player icon and play button in the
		// control bar
		post(new Runnable() {
			@Override
			public void run() {
				if (mControls) {
					mIcon.setText(null);
				}
				switch (event.getNewState()) {
				case PLAYING:
					// Video is visible, icon and control bar fade out
					mVideoView.setVideoVisible(true);
					if (mControls) {
						mIcon.setVisible(false);
						mControlBar.setPlaying(true);
						mControlBar.setVisible(false);
					}
					break;
				case PAUSED:
					// Video visibility is unchanged, icon and control bar
					// fade in
					if (mControls) {
						mIcon.setState(View.PlayerIconState.PLAY);
						mIcon.setVisible(true);
						mControlBar.setPlaying(false);
						mControlBar.setVisible(true);
					}
					break;
				case IDLE:
					if (mIcon.getState() != PlayerIconState.REPLAY) {
						// Video is invisible, icon fades in, control bar
						// fades out
						mVideoView.setVideoVisible(false);
						if (mControls) {
							mIcon.setState(View.PlayerIconState.PLAY);
							mIcon.setVisible(true);
							mControlBar.setPlaying(false);
							mControlBar.setVisible(false);
							mControlBar.updateBuffer(0);
						}
					}
					break;
				case BUFFERING:
					// Video visibility is unchanged, icon fades in and
					// control bar fades out
					if (mControls) {
						mIcon.setState(View.PlayerIconState.BUFFERING);
						mIcon.setVisible(true);
						mControlBar.setVisible(false);
						mControlBar.setPlaying(true);
					}
					break;
				default:
					break;
				}
			}
		});
	}

	private void handleMediaComplete(
			@SuppressWarnings("unused") MediaEvent event) {
		// Update the state in the player icon and play button in the
		// control bar
		post(new Runnable() {
			@Override
			public void run() {
				// Video is not visible, icon fades in, control bar fades
				// out
				mVideoView.setVideoVisible(false);
				if (mControls) {
					mIcon.setState(View.PlayerIconState.REPLAY);
					mIcon.setText(null);
					mIcon.setVisible(true);
					mControlBar.setPlaying(false);
					mControlBar.setVisible(false);
					mControlBar.updateBuffer(0);
				}
			}
		});
	}

	private void handlePlayerError(final PlayerEvent event) {
		// Update the state in the player icon and play button in the
		// control bar
		post(new Runnable() {
			@Override
			public void run() {
				// Video is not visible, icon (including error message)
				// fades in, control bar fades out
				mVideoView.setVideoVisible(false);
				if (mControls) {
					mIcon.setState(View.PlayerIconState.ERROR);
					mIcon.setText(event.getMessage());
					mIcon.setVisible(true);
					mControlBar.setPlaying(false);
					mControlBar.setVisible(false);
					mControlBar.updateBuffer(0);
				}
			}
		});
	}

	private void handleMediaMeta(MediaEvent event) {
		JWPlayer.Metadata m = event.getMetadata();
		if (m instanceof JWPlayer.Metadata.Stream) {
			// Stream update (live, duration)
			if (mControls) {
				JWPlayer.Metadata.Stream s = (JWPlayer.Metadata.Stream) m;
				mControlBar.updateTime(0, s.getDuration());
				mControlBar.setLive(s.isLive());
			}
		}
	}

	@Override
	public void handle(PlayerEvent event) {
		// Handle an incoming event.
		if (MediaEvent.JWPLAYER_MEDIA_TIME.equals(event.getType())) {
			handleMediaTime((MediaEvent) event);
		}
		else if (MediaEvent.JWPLAYER_MEDIA_BUFFER.equals(event.getType())) {
			handleMediaBuffer((MediaEvent) event);
		}
		else if (MediaEvent.JWPLAYER_MEDIA_SEEK.equals(event.getType())) {
			handleMediaSeek((MediaEvent) event);
		}
		else if (MediaEvent.JWPLAYER_MEDIA_SELECTED.equals(event.getType())) {
			handleMediaSelected((MediaEvent) event);
		}
		else if (PlayerStateEvent.JWPLAYER_PLAYER_STATE.equals(event.getType())) {
			handlePlayerState((PlayerStateEvent) event);
		}
		else if (MediaEvent.JWPLAYER_MEDIA_COMPLETE.equals(event.getType())) {
			handleMediaComplete((MediaEvent) event);
		}
		else if (PlayerEvent.JWPLAYER_ERROR.equals(event.getType())) {
			handlePlayerError(event);
		}
		else if (MediaEvent.JWPLAYER_MEDIA_META.equals(event.getType())) {
			handleMediaMeta((MediaEvent) event);
		}
	}

	/**
	 * @return Whether the player is currently in fullscreen mode.
	 */
	public boolean isFullScreen() {
		return mIsFullscreen;
	}

	/**
	 * Create the fullscreen layout if necessary.
	 */
	private void setupFullScreenLayout() {
		if (mFullScreenLayout == null) {
			mFullScreenLayout = new RelativeLayout(getContext());
			mFullScreenLayout.setBackgroundColor(Color.BLACK);

			mFullScreenLayout.setOnClickListener(this);
		}
	}

	/**
	 * Create the fullscreen dialog if necessary.
	 */
	private void setupFullScreenDialog() {
		if (mFullScreenDialog == null) {
			mFullScreenDialog = new Dialog(getContext(),
					android.R.style.Theme_NoTitleBar_Fullscreen);
			mFullScreenDialog.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FULLSCREEN
							| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
			mFullScreenDialog.setContentView(mFullScreenLayout);
			mFullScreenDialog
					.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							fullScreen(false);
						}
					});
		}
	}

	/**
	 * @param state
	 *            Whether the player should be in fullscreen mode.
	 */
	public synchronized void fullScreen(boolean state) {
		if (state) {
			if (isFullScreen()) {
				return;
			}
			// Dispatch the fullscreen event
			dispatchEvent(new ViewEvent(ViewEvent.JWPLAYER_VIEW_FULLSCREEN,
					true));
			mIsFullscreen = true;
			Log.i(TAG, "Switching to full screen.");
			mSwitchingFullscreen = true;

			// Create the fullscreen layout if necessary
			setupFullScreenLayout();

			// Move the video view to the fullscreen layout
			removeView(mVideoView);
			mFullScreenLayout.addView(mVideoView);

			// Move the controls to the fullscreen layout
			if (mControls) {
				removeView(mIcon);
				mFullScreenLayout.addView(mIcon);
				removeView(mControlBar);
				LayoutParams p = (LayoutParams) mControlBar.getLayoutParams();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					// Move the control bar out of the way of the system UI

					// TODO: This is rather arbitrary; maybe we can find a
					// better position?
					p.bottomMargin = 60;
				}
				mFullScreenLayout.addView(mControlBar, p);
			}
			if (mHasLogo) {
				removeView(mLogo);
				mFullScreenLayout.addView(mLogo);
			}
				// Create the fullscreen dialog if necessary
			setupFullScreenDialog();
			mFullScreenDialog.show();
			
			if (mControls) {
				// mIcon.bringToFront();
				// mControlBar.bringToFront();
			}
		}
		else {
			if (!isFullScreen()) {
				return;
			}
			// Dispatch the fullscreen event
			dispatchEvent(new ViewEvent(ViewEvent.JWPLAYER_VIEW_FULLSCREEN,
					false));

			mIsFullscreen = false;
			Log.i(TAG, "Switching from fullscreen.");
			mSwitchingFullscreen = true;

			// Move the video to the original position
			mFullScreenLayout.removeView(mVideoView);
			addView(mVideoView);
			if (mHasLogo) {
				mFullScreenLayout.removeView(mLogo);
				addView(mLogo);
			}
				
			// Move the controls to the original position
			if (mControls) {
				mFullScreenLayout.removeView(mIcon);
				addView(mIcon);
				mFullScreenLayout.removeView(mControlBar);
				LayoutParams p = (LayoutParams) mControlBar.getLayoutParams();
				p.bottomMargin = PLAYER_MARGIN;
				addView(mControlBar, p);
				bringChildToFront(mIcon);
				bringChildToFront(mControlBar);
			}

			mFullScreenDialog.dismiss();
		}
		// Set the visibility of the system UI
		// Post this, because fullscreen switches seem to make hiding impossible
		// on Android 4.1
		post(new Runnable() {
			@Override
			public void run() {

				if (mControls) {
					setSystemUIVisibility(!mIsFullscreen
							|| mControlBar.isVisible());
				}
				else {
					setSystemUIVisibility(!mIsFullscreen);
				}
			}
		});

		// Video is invisible until next state update
		mVideoView.setVideoVisible(false);
		// Force the stream to go into buffering mode
		mModel.getMedia().triggerBuffer();
	}

	/**
	 * Toggle fullscreen mode.
	 */
	public void fullScreen() {
		fullScreen(!isFullScreen());
	}

	/**
	 * @param visible
	 *            Whether the system UI should be visible.
	 */
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void setSystemUIVisibility(boolean visible) {
		// Android < 3 has no system UI
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// The flags to set on the system UI
			int flags;
			if (!visible) {
				// Flags for hiding:
				// 3 <= version < 4: STATUS_BAR_HIDDEN
				// 4 <= version: SYSTEM_UI_FLAG_LOW_PROFILE
				// 4.1 <= version: SYSTEM_UI_FLAG_HIDE_NAVIGATION
				flags = android.view.View.STATUS_BAR_HIDDEN;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					flags = android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
							| android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						// This flag does not seem to work on 4.0
						// When it is set, two taps are required for some reason
						flags |= android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
					}
				}
			}
			else {
				// Flags for showing:
				// 3 <= version < 4: STATUS_BAR_VISIBLE
				// 4 <= version: SYSTEM_UI_FLAG_VISIBLE
				flags = android.view.View.STATUS_BAR_VISIBLE;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					flags = android.view.View.SYSTEM_UI_FLAG_VISIBLE;
					if (isFullScreen()) {
						flags |= android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
					}
				}
			}
			mVideoView.setSystemUiVisibility(flags);
		}
	}

	@Override
	public void onClick(android.view.View v) {
		// Only listen to click events when we have controls.
		// API should take care of it otherwise.
		// Note that clicks from the control bar do not bubble up to here.
		if (mControls) {
			switch (mModel.getState()) {
			case BUFFERING:
			case PLAYING:
				// Simply show the control bar
				if (!mControlBar.isVisible()) {
					mControlBar.setVisible(true);
					mControlBar.tickFadeOut();
				}
				else {
					mControlBar.setVisible(false);
				}
				break;
			case IDLE:
			case PAUSED:
				// Start playback
				mController.play();
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Handle system UI visibility changes. Used for detecting touch events when
	 * fullscreen with the UI hidden.
	 * 
	 * @param visibility
	 *            The visibility flags.
	 */
	private void onSystemUiVisibilityChange(int visibility) {
		if (mControls) {
			if (visibility == SYSTEM_UI_FLAG_VISIBLE && isFullScreen()) {
				switch (mModel.getState()) {
				case IDLE:
				case PAUSED:
					// Start playback
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
						mController.play();
					}
					//$FALL-THROUGH$
				case BUFFERING:
				case PLAYING:
					// Always show the control bar, because we are also showing
					// the system UI.
					mControlBar.setVisible(true);
					mControlBar.tickFadeOut();
					break;
				default:
					break;
				}
			}
		}
		else {
			// No controls: hide the system UI after a while
			postDelayed(new Runnable() {
				@Override
				public void run() {
					setSystemUIVisibility(false);
				}
			}, 4000);
		}
	}

	/**
	 * View containing the video image.
	 * 
	 * @author tom
	 */
	private class JWVideoView extends SurfaceView implements
			SurfaceHolder.Callback, EventListener<MediaEvent> {
		@SuppressWarnings("hiding")
		private static final String TAG = "JWVideoView";

		// Whether to resume the video when the display is back
		private boolean mResumeOnDisplay = false;

		/**
		 * @param context
		 *            The view context.
		 */
		public JWVideoView(Context context) {
			super(context);
			setupVideoView();
		}

		/**
		 * @param context
		 *            The view context.
		 * @param attrs
		 *            The attributes for the view.
		 */
		public JWVideoView(Context context, AttributeSet attrs) {
			super(context, attrs);
			setupVideoView();
		}

		@SuppressWarnings("deprecation")
		private void setupVideoView() {
			setFocusable(false);
			getHolder().addCallback(this);
			getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

			mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_LOADED, this);
			mModel.addEventListener(MediaEvent.JWPLAYER_MEDIA_META, this);

			// When nothing is loaded, do not show.
			setVideoVisible(false);
		}

		/**
		 * @return The width of the video, or 16 if width or height is unknown.
		 */
		private int getVideoWidth() {
			if (mVideoWidth < 1 || mVideoHeight < 1) {
				return 16;
			}
			return mVideoWidth;
		}

		/**
		 * @return The height of the video, or 9 if width or height is unknown.
		 */
		private int getVideoHeight() {
			if (mVideoWidth < 1 || mVideoHeight < 1) {
				return 9;
			}
			return mVideoHeight;
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int w = getVideoWidth(), h = getVideoHeight();
			double ratio = (double) w / (double) h;
			// Grow the width and height if necessary
			int width = getDefaultSize(w, widthMeasureSpec);
			int height = getDefaultSize(h, heightMeasureSpec);
			// Correct to maintain aspect ratio
			if (height * ratio < width) {
				width = (int) (height * ratio);
			}
			else {
				height = (int) (width / ratio);
			}
			setMeasuredDimension(width, height);
		}

		/**
		 * Set the display for the player.
		 * 
		 * @param holder
		 *            The display to connect to the media.
		 */
		private void setDisplay(SurfaceHolder holder) {
			if (mModel.getMedia() != null) {
				mModel.getMedia().setDisplay(holder);
			}
		}

		@SuppressLint("DrawAllocation")
		@Override
		public void draw(android.graphics.Canvas canvas) {
			if (isInEditMode()) {
				// Draw allocation is not a big problem in edit mode
				// In edit mode, draw a rectangle with a nice gradient to point
				// out the 16:9 area
				Paint p = new Paint();
				p.setShader(new LinearGradient(0, 0, 0, getHeight(),
						Color.DKGRAY, Color.BLACK, Shader.TileMode.CLAMP));
				canvas.drawRect(canvas.getClipBounds(), p);
			}
			else {
				super.draw(canvas);
			}
		}

		/**
		 * @param visible
		 *            Whether the video should be visible in the view.
		 */
		public void setVideoVisible(boolean visible) {
			Log.v(TAG, "Video visible: " + visible);
			// Video is only visible if background is transparent
			setBackgroundColor(visible ? Color.TRANSPARENT : Color.BLACK);
			invalidate();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {
			// Nothing to do here
			Log.v(TAG, "Surface changed");
			invalidate();
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.v(TAG, "Surface was created");
			setDisplay(holder);

			if (!mSwitchingFullscreen) {
				if (mResumeOnDisplay) {
					mController.play(true);
					mResumeOnDisplay = false;
				}
			}
			mSwitchingFullscreen = false;

			postInvalidate();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.v(TAG, "Surface was destroyed");
			setDisplay(null);
			if (!mSwitchingFullscreen) {
				if (mModel.getState() == PlayerState.PLAYING
						|| mModel.getState() == PlayerState.BUFFERING) {
					mResumeOnDisplay = true;
					mController.pause(true);
				}
			}
			postInvalidate();
		}

		/**
		 * Handle an update to the video dimensions. Also requests a layout
		 * update.
		 * 
		 * @param width
		 *            The width of the video.
		 * @param height
		 *            The height of the video.
		 */
		private void updateVideoDimensions(int width, int height) {
			Log.v(TAG, "Updating dimensions to " + width + "x" + height);
			if (mVideoHeight != height || mVideoWidth != width) {
				mVideoWidth = width;
				mVideoHeight = height;
				requestLayout();
			}
		}

		/**
		 * @param event
		 *            The event that should be handled.
		 */
		@Override
		public void handle(MediaEvent event) {
			if (MediaEvent.JWPLAYER_MEDIA_LOADED.equals(event.getType())) {
				// A handler for when media are loaded.
				// Should set the display and reset the dimensions.
				updateVideoDimensions(0, 0);
				Log.v(TAG, "Updating display for current media.");
				mModel.getMedia().setDisplay(getHolder());
			}
			else if (MediaEvent.JWPLAYER_MEDIA_META.equals(event.getType())) {
				// Handler for new metadata.
				// Should update the dimensions if necessary.
				JWPlayer.Metadata metadata = event.getMetadata();
				if (metadata instanceof JWPlayer.Metadata.Dimensions) {
					JWPlayer.Metadata.Dimensions d = (JWPlayer.Metadata.Dimensions) metadata;
					updateVideoDimensions(d.getWidth(), d.getHeight());
				}
			}
		}
	}

	/**
	 * The possible states for the player icon.
	 * 
	 * @author tom
	 */
	private static enum PlayerIconState {
		PLAY, BUFFERING, REPLAY, ERROR
	}

	/**
	 * The icon in the center of the player. Displays as play, buffering, replay
	 * or error.
	 * 
	 * @author tom
	 */
	private class PlayerIcon extends RelativeLayout implements AlphaView {
		// The current state of the icon
		private View.PlayerIconState mState = View.PlayerIconState.PLAY;
		// Whether the buffering animation is running
		private boolean mAnimationActive = false;

		// Object which fades the view in and out.
		private ViewFader mFader;

		// The imageview containing the actual icon
		private ImageView mImage;
		// The textview containing the (optional) text to show next to the icon
		private TextView mText;
		

		

		private int mAlpha = 255;

		/**
		 * @param context
		 *            The context of the view.
		 */
		public PlayerIcon(Context context) {
			super(context);
			setup();
		}

		public PlayerIconState getState() {
			return mState;
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 */
		public PlayerIcon(Context context, AttributeSet attrs) {
			super(context, attrs);
			setup();
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 * @param defStyle
		 *            The default style for the view.
		 */
		public PlayerIcon(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			setup();
		}

		private void setup() {
			// Start out showing the play button
			mState = View.PlayerIconState.PLAY;
			setBackgroundResource(R.drawable.display_back);

			setupIcon();
			setupText();
			setupLogo();
			updateState();

			mFader = new ViewFader(this);
		}

		private void setupIcon() {
			mImage = new ImageView(getContext());
			mImage.setScaleType(ScaleType.CENTER);
			mImage.setImageResource(R.drawable.display_play);
			LayoutParams params = new LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
			params.addRule(CENTER_VERTICAL);
			params.addRule(ALIGN_PARENT_LEFT);
			mImage.setId(1);
			addView(mImage, params);
		}

		private void setupText() {
			mText = new TextView(getContext());
			mText.setMaxLines(3);
			mText.setEllipsize(TruncateAt.MARQUEE);
			mText.setTextSize(17);
			LayoutParams params = new LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
			params.addRule(CENTER_VERTICAL);
			// Text is align to the right of the image
			params.addRule(RIGHT_OF, mImage.getId());
			mText.setVisibility(GONE);
			addView(mText, params);
		}

		
		/**
		 * Update the icon graphic to represent the icon state.
		 */
		private void updateState() {
			final View.PlayerIconState state = mState;
			post(new Runnable() {
				@Override
				public void run() {
					switch (state) {
					case PLAY:
						mImage.setImageResource(R.drawable.display_play);
						break;
					case BUFFERING:
						mImage.setImageResource(R.drawable.display_buffer_rotate);
						// Start the buffering animation
						if (!mAnimationActive) {
							mAnimationActive = true;
							updateBufferingAnimation();
						}
						break;
					case REPLAY:
						mImage.setImageResource(R.drawable.display_replay);
						break;
					case ERROR:
						mImage.setImageResource(R.drawable.display_error);
						break;
					default:
						break;
					}
				}
			});
		}

		private void updateBufferingAnimation() {
			// We do a manual animation, because built-in rotation animations
			// want to rotate a whole view, instead of just the foreground.
			postDelayed(new Runnable() {
				@Override
				public void run() {
					Drawable d = mImage.getDrawable();
					if (mState == View.PlayerIconState.BUFFERING
							&& getParent() != null && mFader.isVisible()) {
						d.setLevel(d.getLevel() + (10000 / 8));
						updateBufferingAnimation();
					}
					else {
						mAnimationActive = false;
					}
				}
			}, 100);
		}

		/**
		 * @param state
		 *            The new icon state.
		 */
		public void setState(View.PlayerIconState state) {
			mState = state;
			updateState();
		}

		/**
		 * @param text
		 *            The text to show in the icon. If empty or null, only the
		 *            icon is shown.
		 */
		public void setText(String text) {
			String t = text;
			if (t == null) {
				t = "";
			}
			mText.setText(t);
			mText.setVisibility(t.isEmpty() ? GONE : VISIBLE);
		}

		/**
		 * @param visible
		 *            Whether the icon should be visible.
		 */
		public void setVisible(final boolean visible) {
			mFader.fade(visible);
		}

		@Override
		public void draw(Canvas canvas) {
			// Homebrew alpha blending
			canvas.saveLayerAlpha(null, mAlpha, Canvas.ALL_SAVE_FLAG);
			super.draw(canvas);
			canvas.restore();
		}

		@Override
		public void setAlpha(int alpha) {
			if (alpha != mAlpha) {
				invalidate();
			}
			mAlpha = alpha;
		}
	}

	/**
	 * The control bar at the bottom of the player.
	 * 
	 * @author tom
	 */
	private class ControlBar extends LinearLayout implements
			View.OnClickListener, ViewFader.AlphaView,
			ViewFader.OnVisibleListener {
		// The maximum width of the control bar
		private static final int MAX_WIDTH = 1000;

		// Whether playback is currently active
		private boolean mPlaying = false;

		// Object which fades the view in and out
		private ViewFader mFader;

		// The play/pause button
		private ControlBarButton mPlayButton;
		// The fullscreen button
		private ControlBarButton mFullScreenButton;
		// The text denoting the current position and the duration
		private TimerLabel mPositionLabel, mDurationLabel;
		// The time slider
		private TimeSlider mTimeSlider;

		// Iff <= 0, fading is allowed
		// If !mVisible && mFadeAllowed > 1, the controls will remain visible
		private final int mFadeAllowed = 0;

		private int mAlpha = 255;

		/**
		 * @param context
		 *            The context of the view.
		 */
		public ControlBar(Context context) {
			super(context);
			setup();
		}

		public boolean isVisible() {
			return mFader.isVisible();
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 */
		public ControlBar(Context context, AttributeSet attrs) {
			super(context, attrs);
			setup();
		}

		private void setup() {
			setBackgroundResource(R.drawable.controlbar_back);
			setPadding(5, 3, 5, 3);
			setGravity(Gravity.CENTER_VERTICAL);

			setupPlayButton();
			setupPositionLabel();
			setupTimeSlider();
			setupDurationLabel();
			setupFullScreenButton();

			mFader = new ViewFader(this);
			mFader.setOnVisibleListener(this);
			mFader.setVisible(isInEditMode());
		}

		private void setupPlayButton() {
			mPlayButton = new ControlBarButton(getContext());
			addView(mPlayButton);
			mPlayButton.setOnClickListener(this);

			// Set the correct graphic
			updateState();
		}

		private void setupPositionLabel() {
			mPositionLabel = new TimerLabel(getContext());
			addView(mPositionLabel);
		}

		private void setupTimeSlider() {
			mTimeSlider = new TimeSlider(getContext());
			LayoutParams p = new LayoutParams(0,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1);
			p.gravity = Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL;
			addView(mTimeSlider, p);
		}

		private void setupDurationLabel() {
			mDurationLabel = new TimerLabel(getContext());
			addView(mDurationLabel);
		}

		private void setupFullScreenButton() {
			mFullScreenButton = new ControlBarButton(getContext());
			mFullScreenButton
					.setImageResource(R.drawable.controlbar_fullscreenbutton);
			mFullScreenButton.setOnClickListener(this);
			addView(mFullScreenButton);
		}

		/**
		 * @param playing
		 *            Whether the player is currently in a playback state.
		 */
		public void setPlaying(boolean playing) {
			mPlaying = playing;
			updateState();
		}

		/**
		 * Update the state of the control bar to represent the player state.
		 */
		private void updateState() {
			final boolean playing = mPlaying;
			// Set the correct graphic
			post(new Runnable() {
				@Override
				public void run() {
					if (playing) {
						mPlayButton
								.setImageResource(R.drawable.controlbar_pausebutton);
					}
					else {
						mPlayButton
								.setImageResource(R.drawable.controlbar_playbutton);
					}
				}
			});
		}

		/**
		 * Update the time information in the control bar.
		 * 
		 * @param position
		 *            The current playback position.
		 */
		public void updateTime(int position) {
			mPositionLabel.setTime(position);
			mTimeSlider.updateTime(position);
		}

		/**
		 * Update the time information in the control bar.
		 * 
		 * @param position
		 *            The current playback position.
		 * @param duration
		 *            The total duration of the current media.
		 */
		public void updateTime(int position, int duration) {
			mDurationLabel.setTime(duration);
			mPositionLabel.setTime(position);
			mTimeSlider.updateTime(position, duration);
		}

		/**
		 * @param bufferPercent
		 *            The buffer percentage.
		 */
		public void updateBuffer(int bufferPercent) {
			mTimeSlider.setSecondaryProgress(mTimeSlider.getMax()
					* bufferPercent / 100);
		}

		/**
		 * @param visible
		 *            Whether the view should be visible.
		 */
		public void setVisible(boolean visible) {
			mFader.fade(visible || mFadeAllowed > 0);
		}

		@Override
		public void onClick(android.view.View button) {
			if (button == mPlayButton) {
				// Play button was clicked
				mController.play();
			}
			else if (button == mFullScreenButton) {
				// Fullscreen button was clicked
				fullScreen();
			}
		}

		@Override
		public void draw(Canvas canvas) {
			// Homebrew alpha blending
			canvas.saveLayerAlpha(null, mAlpha, Canvas.ALL_SAVE_FLAG);
			super.draw(canvas);
			canvas.restore();
		}

		@Override
		public void setAlpha(int alpha) {
			if (alpha != mAlpha) {
				invalidate();
			}
			mAlpha = alpha;
		}

		/**
		 * Fade out after 4 seconds. Previous fade-out commands are ignored.
		 */
		public void tickFadeOut() {
			mFader.fade(false, 4000, false);
		}

		@Override
		public void onVisible(boolean visible) {
			// Fade the status bar in newer Android devices
			setSystemUIVisibility(visible || !isFullScreen());
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			// Restrict the width
			if (getMeasuredWidth() > MAX_WIDTH) {
				setMeasuredDimension(MAX_WIDTH, getMeasuredHeight());
			}
		}

		/**
		 * @param live
		 *            Whether the currently playing stream is a livestream.
		 */
		public void setLive(final boolean live) {
			// Live:
			// - Position label displays "Live" text
			// - Timeslider and duration are invisible
			// Not live:
			// - Position label and duration label display time
			// - Timeslider is visible
			post(new Runnable() {
				@Override
				public void run() {
					mPositionLabel.setLive(live);
					if (live) {
						mTimeSlider.setVisibility(INVISIBLE);
						mDurationLabel.setVisibility(GONE);
					}
					else {
						mTimeSlider.setVisibility(VISIBLE);
						mDurationLabel.setVisibility(VISIBLE);
					}
				}
			});
		}
	}

	/**
	 * A button in the control bar.
	 * 
	 * @author tom
	 */
	private static class ControlBarButton extends ImageButton {
		/**
		 * @param context
		 *            The context of the view.
		 */
		public ControlBarButton(Context context) {
			super(context);
			setup();
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 */
		public ControlBarButton(Context context, AttributeSet attrs) {
			super(context, attrs);
			setup();
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 * @param defStyle
		 *            The default style of the view.
		 */
		public ControlBarButton(Context context, AttributeSet attrs,
				int defStyle) {
			super(context, attrs, defStyle);
			setup();
		}

		private void setup() {
			setPadding(3, 3, 3, 3);
			setBackgroundResource(0);
		}
	}

	/**
	 * Text representing a time value; showed on the control bar.
	 * 
	 * @author tom
	 */
	private static class TimerLabel extends TextView {
		private boolean mLive;

		/**
		 * @param context
		 *            The context of the view.
		 */
		public TimerLabel(Context context) {
			super(context);
			setup();
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 */
		public TimerLabel(Context context, AttributeSet attrs) {
			super(context, attrs);
			setup();
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 * @param defStyle
		 *            The default style of the view.
		 */
		public TimerLabel(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			setup();
		}

		private void setup() {
			setTextColor(Color.LTGRAY);
			setTextSize(17);
			setTime(0);
		}

		/**
		 * @param time
		 *            The time to display in the view (in milliseconds).
		 */
		public void setTime(int time) {
			final int t = time / 1000;
			final int m = t / 60;
			final int s = t % 60;
			// If live, display text. Otherwise, display the time.
			post(new Runnable() {
				@Override
				public void run() {
					if (mLive) {
						setText("Live broadcast");
					}
					else {
						// Format: 00:00
						setText(String.format("%02d:%02d", Integer.valueOf(m),
								Integer.valueOf(s)));
					}
				}
			});
		}

		/**
		 * @param live
		 *            Whether the currently playing stream is live.
		 */
		public void setLive(boolean live) {
			mLive = live;
			setTime(0);
		}
	}

	/**
	 * The time slider, which displays the playback progress and buffer level.
	 * The user can swipe over the slider to seek.
	 * 
	 * @author tom
	 */
	private class TimeSlider extends SeekBar {
		/**
		 * @param context
		 *            The context of the view.
		 */
		public TimeSlider(Context context) {
			super(context);
			setup();
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 */
		public TimeSlider(Context context, AttributeSet attrs) {
			super(context, attrs);
			setup();
		}

		/**
		 * @param context
		 *            The context of the view.
		 * @param attrs
		 *            The attributes for the view.
		 * @param defStyle
		 *            The default style of the view.
		 */
		public TimeSlider(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			setup();
		}

		private void setup() {
			setPadding(15, 0, 15, 0);

			setProgressDrawable(getResources().getDrawable(
					R.drawable.timeslider));
			setThumb(getResources().getDrawable(R.drawable.timeslider_thumb));
			setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					mController.seek(getProgress());
					mControlBar.tickFadeOut();
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					mControlBar.tickFadeOut();
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if (fromUser) {
						mControlBar.tickFadeOut();
					}
				}
			});
		}

		/**
		 * @param position
		 *            The current playback position.
		 */
		public void updateTime(final int position) {
			if (!isPressed()) {
				post(new Runnable() {
					@Override
					public void run() {
						setProgress(position);
					}
				});
			}
		}

		/**
		 * @param position
		 *            The current playback position.
		 * @param duration
		 *            The duration of the currently playing media.
		 */
		public void updateTime(int position, int duration) {
			setMax(duration);
			updateTime(position);
		}

		// Custom draw operation, because making everything the right sizes with
		// the default approach turns out to be incredibly difficult.
		@Override
		public void draw(Canvas canvas) {
			canvas.save();
			// Draw the rail + progress
			Drawable rail = getProgressDrawable();
			int w = getWidth() - getPaddingLeft() - getPaddingRight();
			rail.setBounds(0, 0, w, 20);
			canvas.translate(getPaddingLeft(), getHeight() / 2 - 10);
			rail.draw(canvas);
			// Draw the thumb
			if (getMax() > 0) {
				Drawable thumb = getResources().getDrawable(
						R.drawable.timeslider_thumb);
				canvas.translate(-10 + (w - 20) * getProgress() / getMax(), -10);
				thumb.setBounds(0, 0, 40, 40);
				thumb.draw(canvas);
			}
			canvas.restore();
		}
	}

	//
	// EventDispatcher methods
	//

	@Override
	public void addEventListener(String type,
			EventListener<? extends Event> listener) {
		mEventDispatcher.addEventListener(type, listener);
	}

	@Override
	public void addGlobalEventListener(EventListener<? extends Event> listener) {
		mEventDispatcher.addGlobalEventListener(listener);
	}

	@Override
	public boolean removeEventListener(String type,
			EventListener<? extends Event> listener) {
		return mEventDispatcher.removeEventListener(type, listener);
	}

	@Override
	public boolean removeGlobalEventListener(
			EventListener<? extends Event> listener) {
		return mEventDispatcher.removeGlobalEventListener(listener);
	}

	@Override
	public void dispatchEvent(Event e) {
		mEventDispatcher.dispatchEvent(e);
	}
}
