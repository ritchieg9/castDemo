package com.example.koalatv;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class VideoActivity extends AppCompatActivity {
    public static final String ARG_VIDEO_URL = "VideoActivity.URL";
    public static final String ARG_VIDEO_POSITION = "VideoActivity.POSITION";

    boolean isInPipMode = false;
    String mUrl;
    SimpleExoPlayer player;
    long videoPosition;
    boolean isPIPModeeEnabled = true;
    PlayerView playerView;
    boolean returnResultOnce = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        playerView = findViewById(R.id.playerView);
        if (getIntent().getExtras() == null || !getIntent().hasExtra(ARG_VIDEO_URL)) {
            finish();
        }
        mUrl = getIntent().getStringExtra(ARG_VIDEO_URL);
        if (savedInstanceState != null) {
            this.videoPosition = savedInstanceState.getLong(ARG_VIDEO_POSITION);
        }

        String channelHeader = getIntent().getStringExtra("channelHeader");
        TextView t = (TextView) findViewById(R.id.channel_title);
        t.setText(channelHeader);

//        fullscreen button pressed
        ImageView btFullScreen = playerView.findViewById(R.id.bt_fullscreen);
        btFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRequestedOrientation();
                if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    btFullScreen.setImageDrawable(getResources().getDrawable(R.drawable.ic_fullscreen));
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    btFullScreen.setImageDrawable(getResources().getDrawable(R.drawable.ic_fullscreen_exit));
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStart() {
        super.onStart();

        //DRM
        String userAgent = Util.getUserAgent(this, "Mozilla/5.0 (Linux; Android 8.0.0; SHIELD Android TV Build/OPR6.170623.010; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/67.0.3396.87 Mobile Safari/537.36");
        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;

        MediaSource mediaSource = buildMediaSource(Uri.parse(mUrl));
        String wvLicenseUri = "https://fubotv.live.ott.irdeto.com/licenseServer/widevine/v1/FuboTV/license/";

        try {
            MediaDrmCallback drmCallback = new HttpMediaDrmCallback(wvLicenseUri,
                    new DefaultHttpDataSourceFactory(userAgent));
            drmSessionManager = DefaultDrmSessionManager.newWidevineInstance(drmCallback, null);
        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
        }

        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        LoadControl defaultLoadControl = new DefaultLoadControl();

        //Remove subs
        DefaultTrackSelector.Parameters currentParameters = ((DefaultTrackSelector) trackSelector).getParameters();
        DefaultTrackSelector.Parameters newParameters = currentParameters.buildUpon().setRendererDisabled(2, true).build();
        trackSelector.setParameters(newParameters);
        //END OF DRM

        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, defaultLoadControl, drmSessionManager);
        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        player.prepare(mediaSource);
        playerView.requestFocus();

        returnResultOnce = true;
        player.setPlayWhenReady(true);

        //Use Media Session Connector from the EXT library to enable MediaSession Controls in PIP.
        MediaSessionCompat mediaSession = new MediaSessionCompat(this, getPackageName());
        MediaSessionConnector mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setPlayer(player, null);
        mediaSession.setActive(true);
    }

    @Override
    protected void onPause() {
        videoPosition = player.getCurrentPosition();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(videoPosition > 0L && !isInPipMode){
            player.seekTo(videoPosition);
        }
        //Makes sure that the media controls pop up on resuming and when going between PIP and non-PIP states.
        playerView.setUseController(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        playerView.setPlayer(null);
        player.release();
        //PIPmode activity.finish() does not remove the activity from the recents stack.
        //Only finishAndRemoveTask does this.
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
        if (outState !=null) {
            outState.putLong(ARG_VIDEO_POSITION, player.getCurrentPosition());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        videoPosition = savedInstanceState.getLong(ARG_VIDEO_POSITION);
    }

    @Override
    public void onBackPressed() {
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
//                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
//                && isPIPModeeEnabled) {
//            enterPIPMode();
//        } else {
//            super.onBackPressed();
//        }
        super.onBackPressed();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (newConfig!=null) {
            videoPosition = player.getCurrentPosition();
            isInPipMode = !isInPictureInPictureMode;
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
    }
    //Called when the user touches the Home or Recents button to leave the app.
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        enterPIPMode();
    }

    private void enterPIPMode(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            videoPosition = player.getCurrentPosition();
            playerView.setUseController(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();
                this.enterPictureInPictureMode(params.build());
            } else {
                this.enterPictureInPictureMode();
            }
            /* We need to check this because the system permission check is publically hidden for integers for non-manufacturer-built apps
               https://github.com/aosp-mirror/platform_frameworks_base/blob/studio-3.1.2/core/java/android/app/AppOpsManager.java#L1640

               ********* If we didn't have that problem *********
                val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                if(appOpsManager.checkOpNoThrow(AppOpManager.OP_PICTURE_IN_PICTURE, packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).uid, packageName) == AppOpsManager.MODE_ALLOWED)

                30MS window in even a restricted memory device (756mb+) is more than enough time to check, but also not have the system complain about holding an action hostage.
             */
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkPIPPermission();
                }
            }, 30);
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void checkPIPPermission(){
        isPIPModeeEnabled = isInPictureInPictureMode();
        if(!isInPictureInPictureMode()){
            onBackPressed();
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        @C.ContentType int type = Util.inferContentType(uri);

        String userAgent = Util.getUserAgent(this, "Mozilla/5.0 (Linux; Android 8.0.0; SHIELD Android TV Build/OPR6.170623.010; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/67.0.3396.87 Mobile Safari/537.36");
        DefaultDataSourceFactory fac = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, userAgent));

        switch (type) {
            case C.TYPE_DASH:
                return  new DashMediaSource.Factory(fac).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(fac).createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }
}
