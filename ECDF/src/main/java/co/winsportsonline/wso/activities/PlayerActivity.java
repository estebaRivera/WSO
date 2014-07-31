package co.winsportsonline.wso.activities;

import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.longtailvideo.jwplayer.JWPlayer;
import com.longtailvideo.jwplayer.JWPlayerView;

import co.winsportsonline.wso.R;

/**
 * Created by Esteban- on 21-04-14.
 */
public class PlayerActivity extends ActionBarActivity implements
        JWPlayer.OnFullscreenListener, JWPlayer.OnPlayListener, JWPlayer.OnBufferListener, JWPlayer.OnIdleListener,
        JWPlayer.OnQualityChangeListener, JWPlayer.OnQualityLevelsListener , JWPlayer.OnErrorListener{

    private JWPlayerView playerView;
    private VideoView videoView;
    private String URL;
    private RelativeLayout contNative;
    private RelativeLayout contJWPLayer;
    private final int MAX = 5;
    private int intentos = 0;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.player);

        progress = new ProgressDialog(this);
        progress.show();
        progress.setContentView(R.layout.progress_dialog);
        progress.setCancelable(false);
        progress.setCanceledOnTouchOutside(false);
        Bundle extras = getIntent().getExtras();
        URL = extras.getString("media");

        contJWPLayer = (RelativeLayout) findViewById(R.id.video_preview);
        contNative = (RelativeLayout) findViewById(R.id.player_native);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            videoView = (VideoView) findViewById(R.id.video_view);
            contNative.setVisibility(View.VISIBLE);
            contJWPLayer.setVisibility(View.GONE);
            displayMediaNative();
        }
        else{
            contJWPLayer.setVisibility(View.VISIBLE);
            contNative.setVisibility(View.GONE);
            playerView = (JWPlayerView)findViewById(R.id.jwplayerView);
            playerView.setOnFullscreenListener( this);
            displayMedia();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void displayMediaNative(){
        videoView.setVideoPath(URL);
        videoView.setMediaController(new MediaController(this));
        progress.dismiss();
        videoView.start();

    }

    private void displayMedia() {
        playerView.setFullscreen(true);
        playerView.release();
        playerView.load(URL);
        progress.dismiss();
        playerView.play();

    }

    @Override
    public void onBuffer() {

    }

    @Override
    public void onFullscreen(boolean state) {
        if(state == false){
            playerView.stop();
            onBackPressed();
        }
    }

    @Override
    public void onIdle() {

    }

    @Override
    public void onPlay() {
        intentos = 0;
    }

    @Override
    public void onQualityChange(JWPlayer.QualityLevel currentQuality) {

    }

    @Override
    public void onQualityLevels(JWPlayer.QualityLevel[] levels) {

    }

    @Override
    public void onError(String message) {
        if(intentos< MAX){
            playerView.play();
            intentos++;
            Log.d("STREAM", "PLAY AGAIN!");
        }else {
            Log.d("STREAM", "Max restore attempts for streaming reached!!! May God have mercy of our souls!!!!");
        }
    }
}
