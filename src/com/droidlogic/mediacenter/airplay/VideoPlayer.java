package com.droidlogic.mediacenter.airplay;

import java.util.Timer;
import java.util.TimerTask;

import org.amlogic.upnp.MediaRendererDevice;

import com.droidlogic.mediacenter.R;
import com.droidlogic.mediacenter.dlna.LoadingDialog;
import com.droidlogic.mediacenter.MediaCenterApplication;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnInfoListener;

public class VideoPlayer extends Activity implements OnBufferingUpdateListener,
    OnCompletionListener, OnInfoListener, OnSeekCompleteListener,
        OnErrorListener, OnPreparedListener, OnVideoSizeChangedListener {
        public static final int DIALOG_VOLUME_ID = 2;
        public static final int DIALOG_EXIT_ID = 3;
        public static final int DIALOG_NEXT_ID = 4;
        private boolean running = false;
        private LoadingDialog    progressDialog      = null;
        public final static String TAG = "tt";
        private ProgressBar vol_bar;
        private VideoView mVideoView;
        private int mVideoHeight = 0;
        private int mVideoWidth = 0;
        private AudioManager mAudioManager;
        private Uri mUri = null;
        private String mUrl = null;
        // private boolean mIsPrepared = false;
        // private boolean mCanSeek = true;
        private float mStartPosition = 0;
        private MediaController mMediaController;
        // private String mClientName = "";
        // apple session id
        private String mAppleSessionID = "";
        // source is airplay or dlna
        private String mType = "AIRPLAY";
        private boolean isend = false;
        private boolean lastpauseplay = false;
        private PlaybackReceiver sReceiver = new PlaybackReceiver();
        private VideoSession mVideoSession = VideoSession.getInstance();
        private Dialog dialog_volume;
        private LoadingDialog exitDlg;
        private int volume_level = 50;
        private boolean          mVolTouch           = false;
        private boolean          mVolChanged         = false;
        private boolean          mProgressTouch      = false;
        private Timer mTimer;
        private TimerTask mTimerTask;
        private static final int UPDATE_POS          = 0;
        private static final int SHOW_STOP           = 1;
        private static final int SHOW_LOADING        = 2;
        private static final int HIDE_LOADING        = 3;
        @Override
        protected void onActivityResult ( int requestCode, int resultCode, Intent data ) {
            Log.i ( TAG, "onActivityResult=" + resultCode );
            super.onActivityResult ( requestCode, resultCode, data );
        }

        @Override
        protected void onCreate ( Bundle savedInstanceState ) {
            super.onCreate ( savedInstanceState );
            mTimer = new Timer();
            requestWindowFeature ( Window.FEATURE_NO_TITLE );
            getWindow().setFlags ( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                   WindowManager.LayoutParams.FLAG_FULLSCREEN );
            getWindow().getDecorView().setSystemUiVisibility (
                View.SYSTEM_UI_FLAG_LOW_PROFILE );
            // PowerManager pm = (PowerManager)
            // getSystemService(Context.POWER_SERVICE);
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            if ( bundle != null ) {
                mType = bundle.getString ( "TYPE", "AIRPLAY" );
                mAppleSessionID = bundle.getString ( "SESSIONID", "" );
                mUrl = bundle.getString ( "URL", "" );
                mUri = Uri.parse ( mUrl );
                mStartPosition = bundle.getFloat ( "SP", 0 );
                //
                mVideoSession.mCurrentSessionID = mAppleSessionID;
                mVideoSession.mUri = mUri;
            }
            setContentView ( R.layout.nativeplayer );
            mAudioManager = ( AudioManager ) getSystemService ( AUDIO_SERVICE );
            mVideoView = ( VideoView ) findViewById ( R.id.HappyPlayvideoview );
            mVideoWidth = 0;
            mVideoHeight = 0;
            setupView();
            mPlaybackInfoHandler.sendEmptyMessage ( SHOW_LOADING );
            mPlaybackInfoHandler.sendEmptyMessageDelayed ( HIDE_LOADING, 5000 );
            setRequestedOrientation ( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE );
            load();
            mPlaybackInfoHandler.sendEmptyMessage ( 0 );
            Log.i ( TAG, "Init Video Player OK" );
        }

        private void registerReceivers() {
            IntentFilter mFilter = new IntentFilter();
            mFilter.addAction ( "ready" );
            mFilter.addAction ( "seek" );
            mFilter.addAction ( "stop" );
            mFilter.addAction ( "pause" );
            mFilter.addAction ( "play" );
            mFilter.addAction ( "volume" );
            mFilter.addAction ( "mute" );
            mFilter.addAction ( "wait" );
            mFilter.addAction ( "ready" );
            registerReceiver ( sReceiver, mFilter );
        }

        private void unRegisterReceivers() {
            unregisterReceiver ( sReceiver );
        }

        public class PlaybackReceiver extends BroadcastReceiver {
                @Override
                public void onReceive ( Context context, Intent intent ) {
                    processExtraData ( intent );
                }
        }

        public Handler mPlaybackInfoHandler = new Handler() {
            @Override
            public void handleMessage ( Message msg ) {
                switch ( msg.what ) {
                    case UPDATE_POS:
                        if ( isend )
                            break;
                        try {
                            mVideoSession.isPlaying = mVideoView.isPlaying();
                            mVideoSession.mCurrentDuration = mVideoView.getDuration(); // unit: ms
                            mVideoSession.mCurrentPosition = mVideoView.getCurrentPosition(); // unit: ms
                        } catch ( IllegalStateException ise ) {
                        }
                        sendEmptyMessageDelayed ( 0, 1000 );
                        break;
                    case SHOW_STOP:
                        //wait2Exit();
                        return;
                    case SHOW_LOADING:
                        showLoading();
                        return;
                    case HIDE_LOADING:
                        hideLoading();
                        return;
                }
            }
        };

        @Override
        protected void onDestroy() {
            hideLoading();
            super.onDestroy();
            mVideoSession.isActive = false;
            Log.i ( TAG, "onDestroy" );
            // mPlaybackService.mHasVideoSession = false;
        }

        @Override
        protected void onPause() {
            if ( running == true ) {
                running = false;
            }
            super.onPause();
            Log.i ( TAG, "onPause" );
            if ( null != mPlaybackInfoHandler ) {
                unRegisterReceivers();
                while ( mPlaybackInfoHandler.hasMessages ( SHOW_LOADING ) ) {
                    mPlaybackInfoHandler.removeMessages ( SHOW_LOADING );
                }
                while ( mPlaybackInfoHandler.hasMessages ( UPDATE_POS ) ) {
                    mPlaybackInfoHandler.removeMessages ( UPDATE_POS );
                }
                mPlaybackInfoHandler.removeMessages ( SHOW_STOP );
                mPlaybackInfoHandler = null;
            }
            hideLoading();
            MediaCenterApplication.setPlayer ( false );
            stop();
            //SystemProperties.set ( "media.amplayer.displast_frame", "false" );
        }

        @Override
        protected void onResume() {
            super.onResume();
            if ( mPlaybackInfoHandler == null ) {
                Log.i ( TAG, "handler is null" );
                finish();
                return;
            }
            running = true;
            mPlaybackInfoHandler.sendEmptyMessage ( SHOW_LOADING );
            registerReceivers();
            MediaCenterApplication.setPlayer ( true );
            Log.i ( TAG, "onResume" );
            //SystemProperties.set ( "media.amplayer.displast_frame", "true" );
        }

        @Override
        public boolean onTrackballEvent ( MotionEvent event ) {
            return true;
        }

        @Override
        public void onConfigurationChanged ( Configuration newConfig ) {
            super.onConfigurationChanged ( newConfig );
        }

        /**
         * show/hide the overlay
         */

        @Override
        public boolean onTouchEvent ( MotionEvent event ) {
            if ( event.getAction() == MotionEvent.ACTION_UP ) {
                if ( mMediaController != null ) {
                    if ( !mMediaController.isShowing() ) {
                        mMediaController.show();
                    }
                }
            }
            return super.onTouchEvent ( event );
        }

        private void processExtraData ( Intent intent ) {
            String mAction = intent.getAction();
            if ( mAction == null ) {
                return;
            }
            Log.i ( TAG, mAction );
            if ( mAction.equals ( "stop" ) ) {
                Log.d ( TAG, "stop" );
                mVideoSession.isPlaying = false;
                stop();
                finish();
            } else if ( mAction.equals ( "pause" ) ) {
                if ( mVideoView == null ) {
                    return;
                }
                try {
                    lastpauseplay = false;
                    mVideoView.pause();
                } catch ( IllegalStateException ise ) {
                    ise.printStackTrace();
                }
            } else if ( mAction.equals ( "play" ) ) {
                /*if (mVideoView == null)
                    return;
                try {
                    mVideoView.start();
                    mPlaybackInfoHandler.sendEmptyMessage(HIDE_LOADING);
                } catch (IllegalStateException ise) {
                    ise.printStackTrace();
                }*/
                lastpauseplay = true;
                play();
            } else if ( mAction.equals ( "seek" ) ) {
                if ( mVideoView == null ) {
                    return;
                }
                Bundle bundle = intent.getExtras();
                if ( bundle == null ) {
                    return;
                }
                int seekto = bundle.getInt ( "seekTo", 0 );
                Log.d ( "tt", "processExtraData=======" + seekto );
                try {
                    if ( seekto > 300 ) {
                        mVideoView.seekTo ( seekto );
                    }
                } catch ( IllegalStateException ise ) {
                    ise.printStackTrace();
                }
            } else if ( mAction.equals ( "ready" ) ) {
                Bundle bundle = intent.getExtras();
                if ( bundle != null )
                {
                    mType = bundle.getString ( "TYPE", "AIRPLAY" );
                    mAppleSessionID = bundle.getString ( "SESSIONID", "" );
                    mUrl = bundle.getString ( "URL","" );
                    mUri = Uri.parse ( mUrl );
                    mStartPosition = bundle.getFloat ( "SP",0 );
                    //
                    mVideoSession.mCurrentSessionID = mAppleSessionID;
                    mVideoSession.mUri = mUri;
                    stop();
                    load();
                }
            }
        }

        @Override
        protected void onPrepareDialog ( int id, Dialog dialog ) {
            WindowManager wm = getWindowManager();
            Display display = wm.getDefaultDisplay();
            LayoutParams lp = dialog.getWindow().getAttributes();
            switch ( id ) {
                case DIALOG_VOLUME_ID: {
                        if ( display.getHeight() > display.getWidth() ) {
                            lp.width = ( int ) ( display.getWidth() * 1.0 );
                        } else {
                            lp.width = ( int ) ( display.getWidth() * 0.5 );
                        }
                        dialog.getWindow().setAttributes ( lp );
                        vol_bar = ( ProgressBar ) dialog_volume.getWindow().findViewById (
                                      android.R.id.progress );
                        int mmax = mAudioManager
                                   .getStreamMaxVolume ( AudioManager.STREAM_MUSIC );
                        int current = mAudioManager
                                      .getStreamVolume ( AudioManager.STREAM_MUSIC );
                        volume_level = current * 100 / mmax;/* volume_level relative value */
                        if ( vol_bar instanceof SeekBar ) {
                            SeekBar seeker = ( SeekBar ) vol_bar;
                            seeker.setOnSeekBarChangeListener ( new OnSeekBarChangeListener() {
                                private long mLastTime = 0;
                                public void onStartTrackingTouch ( SeekBar bar ) {
                                    Debug.d ( TAG, "vol_bar:onStartTrackingTouch" );
                                    mLastTime = 0;
                                    mVolTouch = true;
                                }
                                public void onProgressChanged ( SeekBar bar, int progress,
                                boolean fromuser ) {
                                    Debug.d ( TAG, "vol_bar:onProgressChanged=" + progress );
                                    if ( !fromuser ) {
                                        return;
                                    }
                                    long now = SystemClock.elapsedRealtime();
                                    if ( ( now - mLastTime ) > 250 ) {
                                        mLastTime = now;
                                        // trackball event, allow progress updates
                                        if ( mVolTouch ) {
                                            Debug.d ( TAG, "***progress=" + progress );
                                            vol_bar.setProgress ( progress );
                                            volume_level = progress;
                                            int max = mAudioManager
                                                      .getStreamMaxVolume ( AudioManager.STREAM_MUSIC );
                                            mAudioManager.setStreamVolume (
                                                AudioManager.STREAM_MUSIC, volume_level
                                                * max / 100, 0 );
                                            Intent intent = new Intent();
                                            intent.setAction ( MediaRendererDevice.PLAY_STATE_SETVOLUME );
                                            intent.putExtra ( "VOLUME", volume_level );
                                            sendBroadcast ( intent );
                                        }
                                    }
                                }
                                public void onStopTrackingTouch ( SeekBar bar ) {
                                    Debug.d ( TAG, "vol_bar:onStopTrackingTouch: "
                                              + volume_level );
                                    mVolTouch = false;
                                }
                            } );
                        }
                        vol_bar.setMax ( 100 );
                        vol_bar.setProgress ( volume_level );
                        break;
                    }
            }
        }

        /**
         * from cache
         */
        private void play() {
            if ( mVideoView == null ) {
                return;
            }
            Log.i ( TAG, "played" );
            mPlaybackInfoHandler.removeMessages ( SHOW_LOADING );
            mPlaybackInfoHandler.sendEmptyMessage ( HIDE_LOADING );

            try {
                mVideoView.start();
            } catch ( IllegalStateException ise ) {
                ise.printStackTrace();
            }
            return;
        }

        @Override
        public boolean dispatchKeyEvent ( KeyEvent event ) {
            if ( mMediaController != null ) {
                if ( mMediaController != null ) {
                    if ( !mMediaController.isShowing() ) {
                        mMediaController.show();
                    }
                }
            }
            return super.dispatchKeyEvent ( event );
        }

        private void pause() {
            if ( mVideoView == null ) {
                return;
            }
            Log.i ( TAG, "paused" );
            try {
                mVideoView.pause();
            } catch ( IllegalStateException ise ) {
                ise.printStackTrace();
                // return;
            }
        }

        /**
        *
        */
        private void stop() {
            Log.i(TAG, "stopped");
            release();
        }

        /**
         * seek
         *
         * @ps set play time
         */
        private void seekTo ( int ps ) {
            Log.i ( TAG, "start seekto " + ps + " ms" );
            if ( mVideoView == null ) {
                return;
            }
            mVideoView.seekTo ( ps );
        }

        /**
         * intent handle
         */
        @Override
        protected void onNewIntent ( Intent intent ) {
            super.onNewIntent ( intent );
        }

        /**
         *
         */
        private void load() {
            mVideoWidth = 0;
            mVideoHeight = 0;
            mVideoView.setVideoURI ( mUri );
        }

        private void setupView() {
            mMediaController = new MediaController ( VideoPlayer.this );
            mVideoView.setMediaController ( mMediaController );
            mVideoView.setOnCompletionListener ( this );
            mVideoView.setOnInfoListener ( this );
            mVideoView.setOnBufferingUpdateListener ( this );
            mVideoView.setOnPreparedListener ( this );
            mVideoView.setOnSeekCompleteListener ( this );
            mVideoView.setOnErrorListener ( this );
            mVideoView.setOnVideoSizeChangedListener ( this );
            mMediaController.setExitListener ( new View.OnClickListener() {
                public void onClick ( View v ) {
                    VideoPlayer.this.finish();
                }
            } );
            mMediaController.setVolumeListener ( new View.OnClickListener() {
                public void onClick ( View v ) {
                    if ( mTimerTask != null ) {
                        mTimerTask.cancel();
                        mTimerTask = null;
                    }
                    showDialog ( DIALOG_VOLUME_ID );
                    mTimerTask = new VolumeHideTask();
                    mTimer.schedule ( mTimerTask, 5000 );
                }
            } );
            mVideoView.setFocusable ( true );
            mVideoView.setFocusableInTouchMode ( true );
            mVideoView.requestFocus();
        }

        @Override
        public void onBufferingUpdate ( MediaPlayer arg0, int percent ) {
            //Log.i(TAG, "onBufferingUpdate percent:" + percent);
        }

        @Override
        public void onCompletion ( MediaPlayer arg0 ) {
            Log.i ( TAG, "onCompletion called" );
            isend = true;
            while ( mPlaybackInfoHandler.hasMessages( UPDATE_POS ) ) {
                mPlaybackInfoHandler.removeMessages( UPDATE_POS );
            }
            // //////////////////////////////////////////////////
            // should add followed message
            // /////////////////////////////////////////////////
            Intent intent;
            if ( mType.equals ( "AIRPLAY" ) ) {
                intent = new Intent ( "com.hpplaysdk.happyplay.QUERY_AIRPLAY_STATUS" );
            } else {
                intent = new Intent ( "com.hpplaysdk.happyplay.QUERY_DLNA_STATUS" );
            }
            Bundle bundle = new Bundle();
            bundle.putString ( "STATUS", "stopped" );
            bundle.putString ( "REASON", "ended" );
            bundle.putString ( "SESSIONID", mAppleSessionID );
            intent.putExtras ( bundle );
            sendBroadcast ( intent );
            // //////////////////////////////////////////////////
            mVideoSession.mCurrentPosition = mVideoSession.mCurrentDuration;
            mVideoSession.isPlaying = false;
            stop();
            VideoPlayer.this.finish();
        }

        @Override
        public void onStop() {
            hideLoading();
            super.onStop();
        }

        @Override
        public void onVideoSizeChanged ( MediaPlayer mp, int width, int height ) {
            Log.i ( TAG, "onVideoSizeChanged called" );
            if ( width == 0 || height == 0 ) {
                Log.e ( TAG, "invalid video width(" + width + ") or height(" + height
                        + ")" );
                return;
            }
            Log.i ( TAG, "video width is " + width + ",height is " + height + "." );
            mVideoWidth = width;
            mVideoHeight = height;
        }
        private void showLoading() {
            if ( progressDialog == null && running ) {
                progressDialog = new LoadingDialog ( this, LoadingDialog.TYPE_LOADING, this.getResources().getString (
                        R.string.loading ) );
            }
            if ( running &&  progressDialog != null ) {
                progressDialog.show();
            }
        }

        private void hideLoading() {
            if ( progressDialog != null ) {
                progressDialog.stopAnim();
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
        @Override
        public boolean onKeyDown ( int keyCode, KeyEvent event ) {
            if ( keyCode == KeyEvent.KEYCODE_BACK ) {
                hideLoading();
                finish();
                return true;
            }/* else if ( keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ) {
                mAudioManager.setStreamMute ( AudioManager.STREAM_MUSIC, true );
                Debug.d ( TAG, "input  keycode KeyEvent.KEYCODE_VOLUME_MUTE true" );
        } else if ( keyCode == KeyEvent.KEYCODE_VOLUME_UP ) {
             mAudioManager.setStreamMute ( AudioManager.STREAM_MUSIC, false );
            Debug.d ( TAG, "input  keycode KeyEvent.KEYCODE_VOLUME_Up" );
        }*/
            return super.onKeyDown ( keyCode, event );
        }

        private class VolumeDialog extends Dialog {
                VolumeDialog ( Context context ) {
                    super ( context, R.style.theme_dialog );
                    setContentView ( R.layout.volume_dialog );
                    LayoutParams params = getWindow().getAttributes();
                    params.x = 120;
                    params.y = -120;
                    getWindow().setAttributes ( params );
                }

                @Override
                public boolean onKeyDown ( int keyCode, KeyEvent event ) {
                    if ( keyCode == 24 || keyCode == 25
                            || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ) {
                        if ( keyCode == 24 || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ) {
                            volume_level = vol_bar.getProgress() + 5;
                            if ( volume_level < vol_bar.getMax() ) {
                                vol_bar.setProgress ( volume_level );
                            } else {
                                volume_level = vol_bar.getMax();
                                vol_bar.setProgress ( volume_level );
                            }
                        } else if ( keyCode == 25
                                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT ) {
                            volume_level = vol_bar.getProgress() - 5;
                            if ( volume_level > 0 ) {
                                vol_bar.setProgress ( volume_level );
                            } else {
                                volume_level = 0;
                                vol_bar.setProgress ( volume_level );
                            }
                        }
                        int max = mAudioManager
                                  .getStreamMaxVolume ( AudioManager.STREAM_MUSIC );
                        mAudioManager.setStreamVolume ( AudioManager.STREAM_MUSIC,
                                                        volume_level * max / 100, 0 );
                        Intent intent = new Intent();
                        intent.setAction ( MediaRendererDevice.PLAY_STATE_SETVOLUME );
                        intent.putExtra ( "VOLUME", volume_level );
                        sendBroadcast ( intent );
                        if ( mTimerTask != null ) {
                            mTimerTask.cancel();
                            mTimerTask = null;
                        }
                        if ( dialog_volume == null ) {
                            showDialog ( DIALOG_VOLUME_ID );
                        } else {
                            dialog_volume.show();
                        }
                        mTimerTask = new VolumeHideTask();
                        mTimer.schedule ( mTimerTask, 5000 );
                        if ( vol_bar != null )
                        { vol_bar.setProgress ( volume_level ); }
                        return true;
                    } else if ( keyCode == KeyEvent.KEYCODE_BACK ) {
                        VolumeDialog.this.cancel();
                        return true;
                    }
                    return super.onKeyDown ( keyCode, event );
                }
        }
        class VolumeHideTask extends TimerTask {
                public void run() {
                    if ( null != dialog_volume && dialog_volume.isShowing() ) {
                        dismissDialog ( DIALOG_VOLUME_ID );
                    }
                }
        }
        private void stopExit() {
            mPlaybackInfoHandler.removeMessages ( SHOW_STOP );
            if ( exitDlg != null ) {
                exitDlg.dismiss();
                exitDlg = null;
            }
        }


        public void wait2Exit() {
            Debug.d ( TAG, "wait2Exit......" + running );
            hideLoading();
            if ( !running ) {
                return;
            }
            if ( exitDlg == null ) {
                exitDlg = new LoadingDialog ( this, LoadingDialog.TYPE_EXIT_TIMER, "" );
                exitDlg.setCancelable ( true );
                exitDlg.setOnDismissListener ( new OnDismissListener() {
                    @Override
                    public void onDismiss ( DialogInterface arg0 ) {
                        if ( exitDlg != null && ( VideoPlayer.this.getClass().getName().equals ( exitDlg.getTopActivity ( VideoPlayer.this ) ) ||
                        exitDlg.getCountNum() == 0 ) ) {
                            VideoPlayer.this.finish();
                        }
                    }
                } );
                exitDlg.show();
            } else {
                exitDlg.setCountNum ( 10 );
                exitDlg.show();
            }
        }

        @Override
        public void onPrepared ( MediaPlayer mp ) {
            mPlaybackInfoHandler.sendEmptyMessage ( HIDE_LOADING );
            Log.i ( TAG, "onPrepared called" );
            int duration = mp.getDuration();
            int startposition = ( int ) ( mStartPosition * duration );
            if ( startposition > 3000 ) {
                seekTo ( startposition );
            }
            try {
                mp.start();
            } catch ( IllegalStateException ise ) {
            }
        }

        private void release() {
            if ( mVideoView != null ) {
                // if (mVideoView.isPlaying())
                mVideoView.stopPlayback();
                // mVideoView = null;
            }
        }

        /*
         * @Override protected void onPause() { super.onPause();
         * //mSurface.setKeepScreenOn(false); //if (mWakeLock.isHeld()) //
         * mWakeLock.release(); //stop(); //finish(); Log.i(TAG, "onPause"); }
         *
         * @Override protected void onResume() { super.onResume();
         * //mSurface.setKeepScreenOn(true); //if (!mWakeLock.isHeld()) //
         * mWakeLock.acquire(); Log.i(TAG, "onResume"); }
         */
        public boolean canPause() {
            // TODO Auto-generated method stub
            return mVideoView.canPause();
        }

        public boolean canSeekBackward() {
            // TODO Auto-generated method stub
            return mVideoView.canSeekBackward();
        }

        public boolean canSeekForward() {
            // TODO Auto-generated method stub
            return mVideoView.canSeekForward();
        }

        @Override
        public void onSeekComplete ( MediaPlayer mp ) {
            mPlaybackInfoHandler.sendEmptyMessage ( HIDE_LOADING );
            Log.i ( TAG, "seekComplete called" );

            try {
                if ( lastpauseplay ) {
                    mp.start();
                } else {
                    mp.pause();
                }
            } catch ( IllegalStateException ise ) {
            }
        }

        // if video resulotion is big,then buffering and pause to wait for 2s for
        // buffering
        @Override
        public boolean onInfo ( MediaPlayer mp, int arg1, int arg2 ) {
            Log.i ( TAG, "OnInfo a1= " + arg1 + " arg2= " + arg2 );
            switch ( arg1 ) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    mPlaybackInfoHandler.sendEmptyMessage ( SHOW_LOADING );
                    Log.i ( TAG, "MEDIA_INFO_BUFFERING_START info" );
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    mPlaybackInfoHandler.removeMessages ( SHOW_LOADING );
                    mPlaybackInfoHandler.sendEmptyMessage ( HIDE_LOADING );
                    Log.i ( TAG, "MEDIA_INFO_BUFFERING_END info" );
                    break;
                case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                    Log.i ( TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING info" );
                    break;
                case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                    Log.i ( TAG, "MEDIA_INFO_BAD_INTERLEAVING info" );
                    break;
                case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                    Log.i ( TAG, "MEDIA_INFO_METADATA_UPDATE info" );
                    break;
                case MediaPlayer.MEDIA_INFO_UNKNOWN:
                    Log.i ( TAG, "MEDIA_INFO_UNKNOWN info" );
                    break;
                case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                    Log.i ( TAG, "MEDIA_INFO_NOT_SEEKABLE info" );
                    break;
                case 705:
                    break;
            }
            return true;
        }

        @Override
        public boolean onError ( MediaPlayer mp, int arg1, int arg2 ) {
            Log.i ( TAG, "OnError arg1= " + arg1 + " arg2= " + arg2 );
            isend = true;
            mVideoSession.isPlaying = false;
            // /////////////////////////////////////////////////
            while ( mPlaybackInfoHandler.hasMessages( UPDATE_POS ) ) {
                mPlaybackInfoHandler.removeMessages( UPDATE_POS );
            }
            // ////////////////////////////////////////////////
            Intent intent;
            if ( mType.equals ( "AIRPLAY" ) ) {
                intent = new Intent ( "com.hpplaysdk.happyplay.QUERY_AIRPLAY_STATUS" );
            } else {
                intent = new Intent ( "com.hpplaysdk.happyplay.QUERY_DLNA_STATUS" );
            }
            Bundle bundle = new Bundle();
            bundle.putString ( "STATUS", "stopped" );
            bundle.putString ( "REASON", "error" );
            bundle.putString ( "SESSIONID", mAppleSessionID );
            intent.putExtras ( bundle );
            sendBroadcast ( intent );
            // ///////////////////////////////////////////////////
            switch ( arg1 ) {
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    Log.e ( TAG, "Media Error, Error Unknown " + arg2 );
                    // return true;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    Log.i ( TAG, "Media Error, Server Died " + arg2 );
                    // return true;
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    // return true;
            }
            stop();
            finish();
            return true;
        }

        @Override
        protected Dialog onCreateDialog ( int id ) {
            LayoutInflater inflater = ( LayoutInflater ) VideoPlayer.this
                                      .getSystemService ( LAYOUT_INFLATER_SERVICE );
            switch ( id ) {
                case DIALOG_VOLUME_ID:
                    View layout_volume = inflater.inflate ( R.layout.volume_dialog,
                                                            ( ViewGroup ) findViewById ( R.id.layout_root_volume ) );
                    dialog_volume = new VolumeDialog ( this );
                    return dialog_volume;
                case DIALOG_EXIT_ID:
                    Dialog errDlg = new AlertDialog.Builder ( VideoPlayer.this )
                    .setTitle ( R.string.video_err_title )
                    .setMessage ( R.string.video_err_summary )
                    .setPositiveButton ( R.string.str_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick ( DialogInterface dialog,
                        int which ) {
                            VideoPlayer.this.finish();
                        }
                    } ).create();
                    errDlg.setOnDismissListener ( new DialogInterface.OnDismissListener() {
                        public void onDismiss ( DialogInterface dialog ) {
                            VideoPlayer.this.finish();
                        }
                    } );
                    return errDlg;
            }
            return null;
        }

        /**
         * exit out player activity
         */
        @Override
        public void onBackPressed() { // Back pressed,means exit player,so send
            // stopped message to source devices
            Log.i ( TAG, "Back Pressed,Stop Player" );
            super.onBackPressed();
            // /////////////////////////////////////////////////
            //
            // ////////////////////////////////////////////////
            Intent intent;
            if ( mType.equals ( "AIRPLAY" ) ) {
                intent = new Intent ( "com.hpplaysdk.happyplay.QUERY_AIRPLAY_STATUS" );
            } else {
                intent = new Intent ( "com.hpplaysdk.happyplay.QUERY_DLNA_STATUS" );
            }
            Bundle bundle = new Bundle();
            bundle.putString ( "STATUS", "stopped" );
            bundle.putString ( "REASON", "stopped" );
            bundle.putString ( "SESSIONID", mAppleSessionID );
            intent.putExtras ( bundle );
            sendBroadcast ( intent );
            // ///////////////////////////////////////////////////
            stop();
            finish();
        }
}
