/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.droidlogic.mediacenter.airplay.player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import nz.co.iswe.android.airplay.video.HttpVideoHandler;
//import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.droidlogic.mediacenter.R;
import com.droidlogic.mediacenter.airplay.MovieActivity;
import com.droidlogic.mediacenter.airplay.proxy.AirplayProxy;
import com.droidlogic.mediacenter.airplay.util.ApiHelper;
import com.droidlogic.mediacenter.airplay.util.Utils;
import com.amlogic.util.Debug;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;
import android.widget.Toast;

public class MoviePlayer implements MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener, ControllerOverlay.Listener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener {
        @SuppressWarnings ( "unused" )
        private static final String              TAG                 = "MoviePlayer";
        private static final String              KEY_VIDEO_POSITION  = "video-position";
        private static final String              KEY_RESUMEABLE_TIME = "resumeable-timeout";
        // These are constants in KeyEvent, appearing on API level 11.
        private static final int                 KEYCODE_MEDIA_PLAY  = 126;
        private static final int                 KEYCODE_MEDIA_PAUSE = 127;
        // Copied from MediaPlaybackService in the Music Player app.
        private static final String              SERVICECMD          = "com.android.music.musicservicecommand";
        private static final String              CMDNAME             = "command";
        private static final String              CMDPAUSE            = "pause";
        private static final long                BLACK_TIMEOUT       = 500;
        // If we resume the acitivty with in RESUMEABLE_TIMEOUT, we will keep
        // playing.
        // Otherwise, we pause the player.
        private static final long                RESUMEABLE_TIMEOUT  = 3 * 60 * 1000;                          // 3
        // mins
        private Context                          mContext;
        private final View                       mRootView;
        private final VideoView                  mVideoView;
        private final Bookmarker                 mBookmarker;
        private Uri                              mUri;
        private final Handler                    mHandler            = new Handler();
        private final Handler                    mVideoBufferHandler = new Handler();
        private final AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver;
        private final MovieControllerOverlay     mController;
        public AirplayProxy                     mAirplayProxy;
        private long                             mResumeableTime     = Long.MAX_VALUE;
        private int                              mVideoPosition      = 0;
        private int                              mVideoStartPosition = 0;
        private boolean                          mHasPaused          = false;
        private int                              mLastSystemUiVis    = 0;
        // If the time bar is being dragged.
        private boolean                          mDragging;
        // If the time bar is visible.
        private boolean                          mShowing;
        private boolean                          isRunning           = false;
        private int                              mBufferPercent      = 0;
        private boolean                          mIsPhoto            = false;
        private final Runnable                   mPlayingChecker     = new Runnable() {
            @Override
            public void run() {
                if ( mVideoView
                .isPlaying() ) {
                    if ( mVideoStartPosition > 0
                            && mVideoStartPosition < 100
                            && mVideoView
                    .getDuration() > 0 ) {
                        // mVideoView.suspend();
                        mVideoView
                        .seekTo ( mVideoStartPosition
                                  * mVideoView
                                  .getDuration()
                                  / 100 );
                        setProgress();
                        // mVideoView.resume();
                    }
                    mAirplayProxy
                    .postMoviePlayerState ( HttpVideoHandler.VIDEO_PLAY );
                    mController
                    .showPlaying();
                    // mController.hide();
                    //mController.showLoading();
                } else {
                    mHandler.postDelayed (
                        mPlayingChecker,
                        250 );
                }
            }
        };
        private final Runnable                   mProgressChecker    = new Runnable() {
            @Override
            public void run() {
                int pos = setProgress();
                mHandler.postDelayed (
                    mProgressChecker,
                    1000 - ( pos % 1000 ) );
            }
        };
        private final Runnable                   mPlayerStateUpdater = new Runnable() {
            @Override
            public void run() {
                if ( mVideoView
                .isPlaying() ) {
                    mAirplayProxy
                    .updateCurPosition ( mVideoView
                                         .getCurrentPosition() );
                    mAirplayProxy
                    .updateDuration ( mVideoView
                                      .getDuration() );
                }
                mHandler.postDelayed (
                    mPlayerStateUpdater,
                    1000 );
            }
        };

        public MoviePlayer ( View rootView, final MovieActivity movieActivity,
                             Uri videoUri, Bundle savedInstance, boolean canReplay,
                             int startPos, boolean isphoto ) {
            mContext = movieActivity.getApplicationContext();
            mRootView = rootView;
            mVideoView = ( VideoView ) rootView.findViewById ( R.id.surface_view );
            mBookmarker = new Bookmarker ( movieActivity );
            mUri = videoUri;
            mAirplayProxy = AirplayProxy.getInstance ( mContext );
            mVideoStartPosition = startPos;
            mIsPhoto = isphoto;
            mController = new MovieControllerOverlay ( mContext );
            ( ( ViewGroup ) rootView ).addView ( mController.getView() );
            mController.setListener ( this );
            mController.setCanReplay ( canReplay );
            mVideoView.setOnPreparedListener ( this );
            mVideoView.setOnErrorListener ( this );
            mVideoView.setOnCompletionListener ( this );
            // if (Utils.getSDKVersion() >= 17) this.setOnInfoListener(this);
            mVideoView.setVideoURI ( mUri );
            mVideoView.setOnTouchListener ( new View.OnTouchListener() {
                @Override
                public boolean onTouch ( View v, MotionEvent event ) {
                    mController.show();
                    return true;
                }
            } );
            // The SurfaceView is transparent before drawing the first frame.
            // This makes the UI flashing when open a video. (black -> old screen
            // -> video) However, we have no way to know the timing of the first
            // frame. So, we hide the VideoView for a while to make sure the
            // video has been drawn on it.
            mVideoView.postDelayed ( new Runnable() {
                @Override
                public void run() {
                    mVideoView.setVisibility ( View.VISIBLE );
                }
            }, BLACK_TIMEOUT );
            setOnSystemUiVisibilityChangeListener();
            // Hide system UI by default
            showSystemUi ( false );
            mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
            mAudioBecomingNoisyReceiver.register();
            Intent i = new Intent ( SERVICECMD );
            i.putExtra ( CMDNAME, CMDPAUSE );
            movieActivity.sendBroadcast ( i );
            if ( savedInstance != null ) { // this is a resumed activity
                mVideoPosition = savedInstance.getInt ( KEY_VIDEO_POSITION, 0 );
                mResumeableTime = savedInstance.getLong ( KEY_RESUMEABLE_TIME,
                                  Long.MAX_VALUE );
                mVideoView.start();
                mVideoView.suspend();
                mHasPaused = true;
            } else {
                final Integer bookmark = null; // mBookmarker.getBookmark(mUri);
                if ( bookmark != null ) {
                    showResumeDialog ( movieActivity, bookmark );
                } else {
                    startVideo ( mIsPhoto );
                }
            }
        }

        // @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        private void setOnSystemUiVisibilityChangeListener() {
            if ( !ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION )
            { return; }
            // When the user touches the screen or uses some hard key, the framework
            // will change system ui visibility from invisible to visible. We show
            // the media control and enable system UI (e.g. ActionBar) to be visible
            // at this point
            mVideoView
            .setOnSystemUiVisibilityChangeListener ( new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange ( int visibility ) {
                    int diff = mLastSystemUiVis ^ visibility;
                    mLastSystemUiVis = visibility;
                    if ( ( diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION ) != 0
                    && ( visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION ) == 0 ) {
                        mController.show();
                        mRootView.setBackgroundColor ( Color.BLACK );
                    }
                }
            } );
        }

        @SuppressWarnings ( "deprecation" )
        // @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        private void showSystemUi ( boolean visible ) {
            if ( !ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE )
            { return; }
            int flag = View.SYSTEM_UI_FLAG_VISIBLE;
            if ( !visible ) {
                // We used the deprecated "STATUS_BAR_HIDDEN" for unbundling
                flag |= View.STATUS_BAR_HIDDEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            mVideoView.setSystemUiVisibility ( flag );
        }

        public void onSaveInstanceState ( Bundle outState ) {
            outState.putInt ( KEY_VIDEO_POSITION, mVideoPosition );
            outState.putLong ( KEY_RESUMEABLE_TIME, mResumeableTime );
        }

        private void showResumeDialog ( Context context, final int bookmark ) {
            AlertDialog.Builder builder = new AlertDialog.Builder ( context );
            builder.setTitle ( R.string.resume_playing_title );
            builder.setMessage ( String.format (
                                     context.getString ( R.string.resume_playing_message ),
                                     MoviePlayerUtils.formatDuration ( context, bookmark / 1000 ) ) );
            builder.setOnCancelListener ( new OnCancelListener() {
                @Override
                public void onCancel ( DialogInterface dialog ) {
                    onCompletion();
                }
            } );
            builder.setPositiveButton ( R.string.resume_playing_resume,
            new OnClickListener() {
                @Override
                public void onClick ( DialogInterface dialog, int which ) {
                    mVideoView.seekTo ( bookmark );
                    startVideo ( mIsPhoto );
                }
            } );
            builder.setNegativeButton ( R.string.resume_playing_restart,
            new OnClickListener() {
                @Override
                public void onClick ( DialogInterface dialog, int which ) {
                    startVideo ( mIsPhoto );
                }
            } );
            builder.show();
        }

        public void onPause() {
            mHasPaused = true;
            mHandler.removeCallbacksAndMessages ( null );
            mAirplayProxy.updateCurPosition ( 0 );
            mAirplayProxy.updateDuration ( 0 );
            mVideoPosition = mVideoView.getCurrentPosition();
            mBookmarker.setBookmark ( mUri, mVideoPosition, mVideoView.getDuration() );
            mVideoView.suspend();
            //mAirplayProxy.postMoviePlayerState(HttpVideoHandler.VIDEO_PAUSE);
            mResumeableTime = System.currentTimeMillis() + RESUMEABLE_TIMEOUT;
        }

        float mTransitionAnimationScale = 1.0f;

        public void onResume() {
            if ( mHasPaused ) {
                mVideoView.seekTo ( mVideoPosition );
                mVideoView.resume();
                mAirplayProxy.postMoviePlayerState ( HttpVideoHandler.VIDEO_PLAY );
                // If we have slept for too long, pause the play
                if ( System.currentTimeMillis() > mResumeableTime ) {
                    pauseVideo();
                }
            }
            mHandler.post ( mProgressChecker );
            mHandler.post ( mPlayerStateUpdater );
        }

        public void onDestroy() {
            Debug.i ( TAG, "==>onDestroy" );
            mVideoView.stopPlayback();
            //mAirplayProxy.postMoviePlayerState(HttpVideoHandler.VIDEO_STOP);
            mAudioBecomingNoisyReceiver.unregister();
            isRunning = false;
        }

        // This updates the time bar display (if necessary). It is called every
        // second by mProgressChecker and also from places where the time bar needs
        // to be updated immediately.
        private int setProgress() {
            if ( mDragging || !mShowing ) {
                return 0;
            }
            int position = mVideoView.getCurrentPosition();
            int duration = mVideoView.getDuration();
            mController.setTimes ( position, duration, 0, 0 );
            return position;
        }

        private void startVideo ( boolean isphoto ) {
            // For streams that we expect to be slow to start up, show a
            // progress spinner until playback starts.
            String scheme = mUri.getScheme();
            if ( "http".equalsIgnoreCase ( scheme ) || "rtsp".equalsIgnoreCase ( scheme ) ) {
                mController.showLoading();
                mAirplayProxy.postMoviePlayerState ( HttpVideoHandler.VIDEO_LOADING );
                mHandler.removeCallbacks ( mPlayingChecker );
                mHandler.postDelayed ( mPlayingChecker, 250 );
            } else {
                mAirplayProxy.postMoviePlayerState ( HttpVideoHandler.VIDEO_PLAY );
                mController.showPlaying();
                mController.hideLoading();
                mController.hide();
            }
            if ( mIsPhoto ) {
                isRunning = true;
                mVideoBufferHandler.postDelayed ( mVideoBufferRunnable, 10000 );
            }
            mVideoView.start();
            setProgress();
        }

        private void playVideo() {
            if ( mIsPhoto ) {
                isRunning = true;
                mVideoBufferHandler.postDelayed ( mVideoBufferRunnable, 10000 );
            }
            mVideoView.start();
            mAirplayProxy.postMoviePlayerState ( HttpVideoHandler.VIDEO_PLAY );
            mController.showPlaying();
            setProgress();
        }

        private void pauseVideo() {
            isRunning = false;
            mVideoView.pause();
            mAirplayProxy.postMoviePlayerState ( HttpVideoHandler.VIDEO_PAUSE );
            mController.showPaused();
        }

        // Below are notifications from VideoView
        @Override
        public boolean onError ( MediaPlayer player, int arg1, int arg2 ) {
            mHandler.removeCallbacksAndMessages ( null );
            // VideoView will show an error dialog if we return false, so no need
            // to show more message.
            // mController.showErrorMessage("");
            Debug.i ( TAG, "==>onError" );
            Toast.makeText ( mContext, R.string.play_fail, Toast.LENGTH_LONG ).show();
            onCompletion();
            return true;
        }

        @Override
        public void onCompletion ( MediaPlayer mp ) {
            Debug.i ( TAG, "onCompletion 398" );
            mController.showEnded();
            onCompletion();
            mAirplayProxy.postMoviePlayerState ( HttpVideoHandler.VIDEO_STOP );
        }

        public void onCompletion() {
            Debug.i ( TAG, "onCompletion 404" );
        }

        public void onPlayPause ( boolean play ) {
            if ( play ) {
                if ( !mVideoView.isPlaying() ) {
                    playVideo();
                }
            } else {
                if ( mVideoView.isPlaying() ) {
                    pauseVideo();
                }
            }
        }

        public void onPlayerSeek ( int pos ) {
            if ( pos < mVideoView.getDuration()
                    && mVideoView.getCurrentPosition() > 0 ) {
                mVideoView.seekTo ( pos );
                setProgress();
            }
        }

        private final Runnable mPlayNext = new Runnable() {
            @Override
            public void run() {
                mVideoView.setVideoURI ( mUri );
                startVideo ( mIsPhoto );
            }
        };

        public void onPlayNext ( Uri videoUri, int startPos, boolean isphoto ) {
            mVideoView.suspend();
            mVideoView.stopPlayback();
            mUri = videoUri;
            mVideoStartPosition = startPos;
            mIsPhoto = isphoto;
            // mVideoView.setVideoURI(mUri);
            // startVideo(mIsPhoto);
            mHandler.postDelayed ( mPlayNext, 2000 );
        }

        // Below are notifications from ControllerOverlay
        @Override
        public void onPlayPause() {
            if ( mVideoView.isPlaying() ) {
                pauseVideo();
            } else {
                playVideo();
            }
        }

        @Override
        public void onSeekStart() {
            mDragging = true;
        }

        @Override
        public void onSeekMove ( int time ) {
            mVideoView.seekTo ( time );
        }

        @Override
        public void onSeekEnd ( int time, int start, int end ) {
            mDragging = false;
            mVideoView.seekTo ( time );
            setProgress();
        }

        @Override
        public void onShown() {
            mShowing = true;
            setProgress();
            showSystemUi ( true );
        }

        @Override
        public void onHidden() {
            mShowing = false;
            showSystemUi ( false );
        }

        @Override
        public void onReplay() {
            startVideo ( mIsPhoto );
        }

        // Below are key events passed from MovieActivity.
        public boolean onKeyDown ( int keyCode, KeyEvent event ) {
            // Some headsets will fire off 7-10 events on a single click
            if ( event.getRepeatCount() > 0 ) {
                return isMediaKey ( keyCode );
            }
            switch ( keyCode ) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if ( mVideoView.isPlaying() ) {
                        pauseVideo();
                    } else {
                        playVideo();
                    }
                    return true;
                case KEYCODE_MEDIA_PAUSE:
                    if ( mVideoView.isPlaying() ) {
                        pauseVideo();
                    }
                    return true;
                case KEYCODE_MEDIA_PLAY:
                    if ( !mVideoView.isPlaying() ) {
                        playVideo();
                    }
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    // TODO: Handle next / previous accordingly, for now we're
                    // just consuming the events.
                    return true;
            }
            return false;
        }

        public boolean onKeyUp ( int keyCode, KeyEvent event ) {
            return isMediaKey ( keyCode );
        }

        private static boolean isMediaKey ( int keyCode ) {
            return keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                   || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                   || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                   || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                   || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                   || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE;
        }

        @Override
        public boolean onInfo ( MediaPlayer mp, int what, int extra ) {
            Debug.i ( TAG, "what=" + what + ", extra=" + extra );
            if ( what == MediaPlayer.MEDIA_INFO_BUFFERING_START
                    && mVideoView.getBufferPercentage() < 99 && isRunning ) {
                mVideoView.pause();
                mBufferPercent = mVideoView.getCurrentPosition();
                mController.showLoading();
            }
            if ( what == MediaPlayer.MEDIA_INFO_BUFFERING_START ) {
                mController.showLoading();
            } else if ( what == MediaPlayer.MEDIA_INFO_BUFFERING_END ) {
                mController.hideLoading();
                // mController.hide();
                // mController.showPlaying();
            }
            return true;
        }

        // We want to pause when the headset is unplugged.
        private class AudioBecomingNoisyReceiver extends BroadcastReceiver {
                public void register() {
                    mContext.registerReceiver ( this, new IntentFilter (
                                                    AudioManager.ACTION_AUDIO_BECOMING_NOISY ) );
                }

                public void unregister() {
                    mContext.unregisterReceiver ( this );
                }

                @Override
                public void onReceive ( Context context, Intent intent ) {
                    if ( mVideoView.isPlaying() )
                    { pauseVideo(); }
                }
        }

        public Runnable mVideoBufferRunnable = new Runnable() {
            @Override
            public void run() {
                Debug.i ( TAG,
                          "++++++++++BufferPercentage = "
                          + mVideoView
                          .getBufferPercentage()
                          * mVideoView
                          .getDuration()
                          / 100
                          + ", mBufferPercent="
                          + mBufferPercent );
                if ( isRunning ) {
                    mVideoBufferHandler
                    .postDelayed (
                        mVideoBufferRunnable,
                        2000 );
                } else {
                    return;
                }
                mController
                .setTimes (
                    mVideoView
                    .getCurrentPosition(),
                    mVideoView
                    .getDuration(),
                    0, 0 );
                if ( mVideoView
                        .getBufferPercentage()
                        * mVideoView
                        .getDuration()
                        / 100 - mBufferPercent < 20 * 1000
                        && mVideoView
                .getBufferPercentage() < 99 ) {
                    mVideoView.pause();
                    mController.showLoading();
                } else {
                    mVideoView.start();
                    // mBufferPercent =
                    // mVideoView.getBufferPercentage();
                    mController.hide();
                }
            }
        };

        /*
         * (non-Javadoc)
         *
         * @see
         * android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media
         * .MediaPlayer)
         */
        @Override
        public void onPrepared ( MediaPlayer mp ) {
            mp.setOnInfoListener ( this );
            mVideoView.start();
            mController.hide();
        }
}

class Bookmarker {
        private static final String TAG                        = "Bookmarker";
        private static final String BOOKMARK_CACHE_FILE        = "bookmark";
        private static final int    BOOKMARK_CACHE_MAX_ENTRIES = 100;
        private static final int    BOOKMARK_CACHE_MAX_BYTES   = 10 * 1024;
        private static final int    BOOKMARK_CACHE_VERSION     = 1;
        private static final int    HALF_MINUTE                = 30 * 1000;
        private static final int    TWO_MINUTES                = 4 * HALF_MINUTE;
        private final Context       mContext;

        public Bookmarker ( Context context ) {
            mContext = context;
        }

        public void setBookmark ( Uri uri, int bookmark, int duration ) {
            try {
                BlobCache cache = CacheManager.getCache ( mContext,
                                  BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                                  BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION );
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream ( bos );
                dos.writeUTF ( uri.toString() );
                dos.writeInt ( bookmark );
                dos.writeInt ( duration );
                dos.flush();
                cache.insert ( uri.hashCode(), bos.toByteArray() );
            } catch ( Throwable t ) {
                Debug.w ( TAG, "setBookmark failed", t );
            }
        }

        public Integer getBookmark ( Uri uri ) {
            try {
                BlobCache cache = CacheManager.getCache ( mContext,
                                  BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                                  BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION );
                byte[] data = cache.lookup ( uri.hashCode() );
                if ( data == null )
                { return null; }
                DataInputStream dis = new DataInputStream ( new ByteArrayInputStream (
                            data ) );
                String uriString = DataInputStream.readUTF ( dis );
                int bookmark = dis.readInt();
                int duration = dis.readInt();
                if ( !uriString.equals ( uri.toString() ) ) {
                    return null;
                }
                if ( ( bookmark < HALF_MINUTE ) || ( duration < TWO_MINUTES )
                        || ( bookmark > ( duration - HALF_MINUTE ) ) ) {
                    return null;
                }
                return Integer.valueOf ( bookmark );
            } catch ( Throwable t ) {
                Debug.w ( TAG, "getBookmark failed", t );
            }
            return null;
        }
}
