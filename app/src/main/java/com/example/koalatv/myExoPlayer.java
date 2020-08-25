package com.example.koalatv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;

import org.jetbrains.annotations.Nullable;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link myExoPlayer.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link myExoPlayer#newInstance} factory method to
 * create an instance of this fragment.
 */
public class myExoPlayer extends Fragment implements View.OnClickListener, ExoPlayer.EventListener, PlaybackControlView.VisibilityListener, CastPlayer.SessionAvailabilityListener
{
    
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final String TAG = "PlayerActivity";
    private static final long ORIENTATION_HANDLER_DELAY = 1000L;
    public int savedOrientation;
    private OnFragmentInteractionListener mListener;
    private SimpleExoPlayer player;
    //    private TrackSelectionHelper trackSelectionHelper;
    private DefaultTrackSelector trackSelector;
    private DataSource.Factory mediaDataSourceFactory;
    private Handler mainHandler;
    private EventLogger eventLogger;
    private PlayerView localPlayerView;
    private PlayerControlView castControlView;
    private CastContext castContext;
    
    private long playbackPosition;
    private int currentWindow;
    private boolean playWhenReady = true;
    private ComponentListener componentListener;
    private boolean liveContent = false;
    private static String contentURI;
    private ImageView fullscreenButton;
    private int mLastOrientationRequested;
    private OrientationEventListener mOrientationEventListener;
    private Handler requestedOrientationHandler;
    private CastPlayer castPlayer;
    private Player currentPlayer;
    private MediaRouteButton mediaRouteButton;
    private boolean castMediaQueueCreationPending;

    public myExoPlayer(String mUrl)
    {
        setContentURI(mUrl);
        // Required empty public constructor
    }
    
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment myExoPlayer.
     */
    // TODO: Rename and change types and number of parameters
    public static myExoPlayer newInstance(PlayerControlView castControlView, String mUrl)
    {
        myExoPlayer fragment = new myExoPlayer(mUrl);

        fragment.castControlView = castControlView;
        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }
    
    public boolean isLiveContent()
    {
        return liveContent;
    }
    
    public void setLiveContent(boolean liveContent)
    {
        this.liveContent = liveContent;
    }
    
    public String getContentURI()
    {
        return contentURI;
    }
    
    public void setContentURI(String contentURI)
    {
        this.contentURI = contentURI;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setupOrientationListener();
    
        
        
    }
    
    /**
     * There is no set controller layout ID method,
     * therefore each UI must be contained in a unique
     * layout file
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        
        castContext = CastContext.getSharedInstance(getActivity());
        
        // Inflate the layout for this fragment
        
        View v;
        
        if (liveContent == true) {
            v = inflater.inflate(R.layout.fragment_my_exo_player_live, container, false);
        }
        else {
            v = inflater.inflate(R.layout.fragment_my_exo_player_default, container, false);
        }
        localPlayerView = (PlayerView) v.findViewById(R.id.videoView);
        localPlayerView.requestFocus();

        if (castControlView == null)
            castControlView = (PlayerControlView) v.findViewById(R.id.cast_control_view);

        castControlView.requestFocus();
        //setMediaRouteButton(v);
        
        fullscreenButton = (ImageView) v.findViewById(R.id.image_full_screen);
        
        fullscreenButton.setOnClickListener(new View.OnClickListener()
        {
            Activity a = getActivity();
            
            @Override
            public void onClick(View v)
            {
//                orientationEventListener.
                
                switch (getResources().getConfiguration().orientation) {
                    case Configuration.ORIENTATION_PORTRAIT:
                        a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        mLastOrientationRequested = Configuration.ORIENTATION_LANDSCAPE;
//                        onConfigurationChanged(getResources().getConfiguration());
                        break;
                    case Configuration.ORIENTATION_LANDSCAPE:
                        a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        mLastOrientationRequested = Configuration.ORIENTATION_PORTRAIT;
//                        onConfigurationChanged(getResources().getConfiguration());
                        break;
                }
            }
        });
        
        setupOrientationListener();
        
        return v;
    }
    
    private void setMediaRouteButton(View v){
        mediaRouteButton = (MediaRouteButton)v.findViewById(R.id.media_route_menu_item);
    
        CastButtonFactory.setUpMediaRouteButton(getContext(), mediaRouteButton);
        
    }
    
    private void initializePlayer()
    {
        boolean needNewPlayer = player == null;
        if (needNewPlayer) {


            player = ExoPlayerFactory.newSimpleInstance(getActivity());

            player.addListener(this);
            player.setPlayWhenReady(true);
            player.seekTo(currentWindow, playbackPosition);
            localPlayerView.setPlayer(player);
            
            castPlayer = new CastPlayer(castContext);
            castPlayer.addListener(this);
            castPlayer.setSessionAvailabilityListener(this);
            castControlView.setPlayer(castPlayer);

        }

        if (contentURI == null) {
            contentURI = getString(R.string.hls);

            // Create a data source factory.
            DataSource.Factory dataSourceFactory =
                    new DefaultHttpDataSourceFactory(Util.getUserAgent(getActivity(), "app-name"));
            // Create a HLS media source pointing to a playlist uri.
            HlsMediaSource hlsMediaSource =
                    new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(contentURI));
            // Create a player instance.
            SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(getActivity());
            // Prepare the player with the HLS media source.
            player.prepare(hlsMediaSource);
        }

        setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : player);
        
        //TODO: FOR TESTING ONLY, REMOVE WHEN NOT NEEDED


        if(!castPlayer.isCastSessionAvailable()) {
            MediaSource mediaSource = buildMediaSource(Uri.parse(contentURI));
            player.prepare(mediaSource, true, false);
        }
    }
    
    private MediaSource buildMediaSource(Uri uri)
    {
        
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory("ua");
        
        DefaultHlsDataSourceFactory hlsDataSourceFactory = new DefaultHlsDataSourceFactory(httpDataSourceFactory);
        
        return new HlsMediaSource.Factory(hlsDataSourceFactory).createMediaSource(uri);
    }
    
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
        else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }
    
    @Override
    public void onStart()
    {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
//    hideSystemUI();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }
    
    @SuppressLint("InlinedApi")
    private void hideSystemUI()
    {
        localPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
    
    @SuppressLint("InlinedApi")
    private void showSystemUI()
    {
        localPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
            if (castPlayer != null)
                castPlayer.release();
        }
    }
    
    @Override
    public void onStop()
    {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
            if (castPlayer != null)
                castPlayer.release();
        }
    }
    
    private void releasePlayer()
    {
        if (player != null && currentPlayer == player) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            castPlayer.setSessionAvailabilityListener(null);
            castPlayer.release();
            player.removeListener(componentListener);
            player.setVideoListener(null);
            player.removeVideoDebugListener(componentListener);
            player.removeAudioDebugListener(componentListener);
            player.release();
            player = null;
        }
        if (castPlayer != null)
            castPlayer.release();
    }
    
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            castPlayer.setSessionAvailabilityListener(null);
            castPlayer.release();
            player.removeListener(componentListener);
            player.setVideoListener(null);
            player.removeVideoDebugListener(componentListener);
            player.removeAudioDebugListener(componentListener);
            player.release();
            player = null;
        }
        if (castPlayer != null)
            castPlayer.release();
    }
    
    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        Log.d("tag", "config changed");
        super.onConfigurationChanged(newConfig);
        
        int orientation = newConfig.orientation;
        
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("tag", "Portrait");
            showSystemUI();
            fullscreenButton.setImageResource(R.drawable.player_vector_ic_fullscreen_open);
        }
        
        else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d("tag", "Landscape");
            hideSystemUI();
            fullscreenButton.setImageResource(R.drawable.player_vector_ic_fullscreen_close);
        }
        else Log.w("tag", "other: " + orientation);
    }
    
    private boolean isPortrait(int orientation)
    {
        if (orientation <= 45 || orientation >= 315) {
            return true;
        }
        return false;
    }
    
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason)
    {
    
    }
    
    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections)
    {
    
    }
    
    @Override
    public void onLoadingChanged(boolean isLoading)
    {
    
    }
    
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
    {
    
    }
    
    @Override
    public void onRepeatModeChanged(int repeatMode)
    {
    
    }
    
    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled)
    {
    
    }
    
    @Override
    public void onPlayerError(ExoPlaybackException error)
    {
    
    }
    
    @Override
    public void onPositionDiscontinuity(int reason)
    {
    
    }
    
    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters)
    {
    
    }
    
    @Override
    public void onSeekProcessed()
    {
    
    }
    
    @Override
    public void onClick(View v)
    {
    
    }
    
    @Override
    public void onVisibilityChange(int visibility)
    {
    
    }
    
    private void setupOrientationListener()
    {
        mLastOrientationRequested = Configuration.ORIENTATION_UNDEFINED;
        Activity a = getActivity();
        mOrientationEventListener = new OrientationEventListener(getActivity().getApplicationContext(), SensorManager.SENSOR_DELAY_UI)
        {
            // This constant defines the number of degrees of rotation away from perfect portrait or landscape
            // that we will use to decide if we should unlock the orientation
            static final int ORIENTATION_BREAKPOINT = 5;
            int lastRecordedValue;
            
            @Override
            public void onOrientationChanged(int o)
            {
                // Ignore anything that happens when the device is laying on its back or face.
                if (o == -1) {
                    return;
                }
                
                // Ignore drastic changes. Sometimes we get an erroneous value from the sensor. Note: If you're
                // rotating on an emulator, this will return
                if (Math.abs(o - lastRecordedValue) > 30) {
                    lastRecordedValue = o;
                    return;
                }
                lastRecordedValue = o;
                
                int orientation = getResources().getConfiguration().orientation;
                
                boolean inRequestedOrientation = mLastOrientationRequested == orientation;
                boolean shouldUnlockForLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE && (Math.abs(o - 270) <= ORIENTATION_BREAKPOINT || Math.abs(o - 90) <= ORIENTATION_BREAKPOINT);
                boolean shouldUnlockForPortrait = orientation == Configuration.ORIENTATION_PORTRAIT && (o <= ORIENTATION_BREAKPOINT || 360 - o <= ORIENTATION_BREAKPOINT);
                
                if (inRequestedOrientation && (shouldUnlockForLandscape || shouldUnlockForPortrait)) {
                    mLastOrientationRequested = Configuration.ORIENTATION_UNDEFINED;
                    // Delay unlocking the orientation because if we do it right away,
                    // the app will switch temporarily to the opposite orientation first, then unlock
                    requestedOrientationHandler = requestedOrientationHandler == null ? new Handler() : requestedOrientationHandler;
                    requestedOrientationHandler.postDelayed(() -> a.setRequestedOrientation(SCREEN_ORIENTATION_SENSOR), ORIENTATION_HANDLER_DELAY);
                }
            }
        };
        
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
        else {
            mOrientationEventListener.disable();
        }
    }
    
    @Override
    public void onCastSessionAvailable()
    {
        setCurrentPlayer(castPlayer);
    }
    
    // CastPlayer.SessionAvailabilityListener implementation.
    
    @Override
    public void onCastSessionUnavailable()
    {
        setCurrentPlayer(player);
    }
    
    // Internal methods.
    
    
    
    private void setCurrentPlayer(Player currentPlayer)
    {
        if (this.currentPlayer == currentPlayer) {
            return;
        }
        
        // View management.
        if (currentPlayer == player) {
            localPlayerView.setVisibility(View.VISIBLE);
            castControlView.hide();
            setMediaRouteButton(getView());
        }
        else /* currentPlayer == castPlayer */ {
            localPlayerView.setVisibility(View.GONE);
            castControlView.show();
            setMediaRouteButton(getView());
        }
        
        // Player state management.
        long playbackPositionMs = C.TIME_UNSET;
        int windowIndex = C.INDEX_UNSET;
        boolean playWhenReady = true;
        if (this.currentPlayer != null) {
            int playbackState = this.currentPlayer.getPlaybackState();
            if (playbackState != Player.STATE_ENDED) {
                playbackPositionMs = this.currentPlayer.getCurrentPosition();
                playWhenReady = this.currentPlayer.getPlayWhenReady();
                windowIndex = this.currentPlayer.getCurrentWindowIndex();
                
            }
            this.currentPlayer.stop(true);
        }
        else {
            // This is the initial setup. No need to save any state.
        }
    
        this.currentPlayer = currentPlayer;
        
        // Media queue management.
        castMediaQueueCreationPending = currentPlayer == castPlayer;
//        if (currentPlayer == exoPlayer) {
//            exoPlayer.prepare(concatenatingMediaSource);
//        }
    
        // Playback transition.
        
            setCurrentItem(playbackPositionMs, playWhenReady);
        
    }
    
    /**
     * Starts playback of the item at the given position.
     *
     * @param positionMs The position at which playback should start.
     * @param playWhenReady Whether the player should proceed when ready to do so.
     */
    private void setCurrentItem(long positionMs, boolean playWhenReady) {
//        maybeSetCurrentItemAndNotify(itemIndex);
        if (castMediaQueueCreationPending && contentURI != null) {
            MediaQueueItem item = buildMediaQueueItem(contentURI);
            castMediaQueueCreationPending = false;
            castPlayer.loadItem(item, positionMs);
        } else {
            currentPlayer.seekTo(positionMs);
            currentPlayer.setPlayWhenReady(playWhenReady);
        }
    }
    
    private static MediaQueueItem buildMediaQueueItem(String uri) {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, "Name");
        MediaInfo mediaInfo = new MediaInfo.Builder(uri)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE).setContentType(MimeTypes.APPLICATION_M3U8)
                .setMetadata(movieMetadata).build();
        return new MediaQueueItem.Builder(mediaInfo).build();
    }
        
    
    
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
    
    private class ComponentListener extends Player.DefaultEventListener implements VideoRendererEventListener, AudioRendererEventListener
    {
        
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
        {
            String stateString;
            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;
                case ExoPlayer.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    break;
                case ExoPlayer.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";
                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            Log.d(TAG, "changed state to " + stateString + " playWhenReady: " + playWhenReady);
        }
        
        @Override
        public void onVideoEnabled(DecoderCounters counters)
        {
        
        }
        
        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs)
        {
        
        }
        
        @Override
        public void onVideoInputFormatChanged(Format format)
        {
        
        }
        
        @Override
        public void onDroppedFrames(int count, long elapsedMs)
        {
        
        }
        
        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio)
        {
        
        }
        
        @Override
        public void onRenderedFirstFrame(Surface surface)
        {
        
        }
        
        @Override
        public void onVideoDisabled(DecoderCounters counters)
        {
        
        }
        
        @Override
        public void onAudioEnabled(DecoderCounters counters)
        {
        
        }
        
        @Override
        public void onAudioSessionId(int audioSessionId)
        {
        
        }
        
        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs)
        {
        
        }
        
        @Override
        public void onAudioInputFormatChanged(Format format)
        {
        
        }
        
        @Override
        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs)
        {
        
        }
        
        @Override
        public void onAudioDisabled(DecoderCounters counters)
        {
        
        }
    }
}