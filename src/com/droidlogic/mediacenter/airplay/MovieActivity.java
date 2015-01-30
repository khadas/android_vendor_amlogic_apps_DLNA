/*
 * Copyright (C) 2007 The Android Open Source Project
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
package com.droidlogic.mediacenter.airplay;

import nz.co.iswe.android.airplay.video.HttpVideoHandler;

import com.droidlogic.mediacenter.R;
//import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.os.PowerManager;

import com.droidlogic.mediacenter.airplay.player.MoviePlayer;
import com.droidlogic.mediacenter.airplay.player.MoviePlayerUtils;
import com.droidlogic.mediacenter.airplay.proxy.AirplayBroadcastFactory;
import com.droidlogic.mediacenter.airplay.proxy.AirplayController;
import com.droidlogic.mediacenter.airplay.util.ApiHelper;
import com.amlogic.util.Debug;

/**
 * This activity plays a video from a specified URI. The client of this activity
 * can pass a logo bitmap in the intent (KEY_LOGO_BITMAP) to set the action bar
 * logo so the playback process looks more seamlessly integrated with the
 * original activity.
 */
public class MovieActivity extends Activity {
        @SuppressWarnings ( "unused" )
        private static final String TAG                  = "MovieActivity";
        public static final String  KEY_LOGO_BITMAP      = "logo-bitmap";
        public static final String  KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
        public static boolean       isRunning            = false;
        private Context             mContext;
        private MoviePlayer         mPlayer;
        private boolean             mFinishOnCompletion;
        private Uri                 mUri;
        private boolean             mTreatUpAsBack;
        private PlayerStopReceiver  mPlayerStopReceiver;
        private String mMediaBuffer = "0.0";
        private PowerManager.WakeLock mWakeLock;

        private void setSystemUiVisibility ( View rootView ) {
            if ( ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE ) {
                rootView.setSystemUiVisibility ( View.SYSTEM_UI_FLAG_VISIBLE );
            }
        }

        @Override
        public void onCreate ( Bundle savedInstanceState ) {
            super.onCreate ( savedInstanceState );
            mContext = MovieActivity.this;
            requestWindowFeature ( Window.FEATURE_ACTION_BAR );
            requestWindowFeature ( Window.FEATURE_ACTION_BAR_OVERLAY );
            setContentView ( R.layout.movie_view );
            View rootView = findViewById ( R.id.movie_view_root );
            setSystemUiVisibility ( rootView );
            Intent intent = getIntent();
            initializeActionBar ( intent );
            mFinishOnCompletion = intent.getBooleanExtra (
                                      MediaStore.EXTRA_FINISH_ON_COMPLETION, true );
            mTreatUpAsBack = intent.getBooleanExtra ( KEY_TREAT_UP_AS_BACK, false );
            int startPos = intent.getIntExtra ( "start-position", 0 );
            boolean isPhoto = intent.getBooleanExtra ( "isphoto", false );
            Debug.i ( TAG, "==>onCreate" );
            SystemProperties.set ( "media.amplayer.buffertime", "6" );
            mPlayer = new MoviePlayer ( rootView, this, intent.getData(),
            savedInstanceState, !mFinishOnCompletion, startPos, isPhoto ) {
                @Override
                public void onCompletion() {
                    if ( mFinishOnCompletion ) {
                        finish();
                    }
                }
            };
            if ( intent.hasExtra ( MediaStore.EXTRA_SCREEN_ORIENTATION ) ) {
                int orientation = intent.getIntExtra (
                                      MediaStore.EXTRA_SCREEN_ORIENTATION,
                                      ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED );
                if ( orientation != getRequestedOrientation() ) {
                    setRequestedOrientation ( orientation );
                }
            }
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
            winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            win.setAttributes ( winParams );
            // We set the background in the theme to have the launching animation.
            // But for the performance (and battery), we remove the background here.
            win.setBackgroundDrawable ( null );
            mPlayerStopReceiver = new PlayerStopReceiver();
        }

        private void setActionBarLogoFromIntent ( Intent intent ) {
            Bitmap logo = intent.getParcelableExtra ( KEY_LOGO_BITMAP );
            if ( logo != null ) {
                getActionBar().setLogo ( new BitmapDrawable ( getResources(), logo ) );
            }
        }

        private void initializeActionBar ( Intent intent ) {
            mUri = intent.getData();
            final ActionBar actionBar = getActionBar();
            setActionBarLogoFromIntent ( intent );
            if ( actionBar == null ) {
                Log.d ( TAG, "ActionBar is null" );
                return;
            }
            actionBar.setDisplayOptions ( ActionBar.DISPLAY_HOME_AS_UP,
                                          ActionBar.DISPLAY_HOME_AS_UP );
            String title = intent.getStringExtra ( Intent.EXTRA_TITLE );
            if ( title != null ) {
                actionBar.setTitle ( title );
            } else {
                // Displays the filename as title, reading the filename from the
                // interface: {@link android.provider.OpenableColumns#DISPLAY_NAME}.
                AsyncQueryHandler queryHandler = new AsyncQueryHandler (
                getContentResolver() ) {
                    @Override
                    protected void onQueryComplete ( int token, Object cookie,
                    Cursor cursor ) {
                        try {
                            if ( ( cursor != null ) && cursor.moveToFirst() ) {
                                String displayName = cursor.getString ( 0 );
                                // Just show empty title if other apps don't set
                                // DISPLAY_NAME
                                actionBar.setTitle ( ( displayName == null ) ? ""
                                                     : displayName );
                            }
                        } finally {
                            MoviePlayerUtils.closeSilently ( cursor );
                        }
                    }
                };
                queryHandler.startQuery ( 0, null, mUri, new String[] {
                    OpenableColumns.DISPLAY_NAME
                }, null, null, null );
            }
        }

        @Override
        public boolean onCreateOptionsMenu ( Menu menu ) {
            super.onCreateOptionsMenu ( menu );
            return true;
        }

        @Override
        public boolean onOptionsItemSelected ( MenuItem item ) {
            return false;
        }

        @Override
        public void onStart() {
            ( ( AudioManager ) getSystemService ( AUDIO_SERVICE ) ).requestAudioFocus (
                null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
            super.onStart();
        }

        @Override
        protected void onStop() {
            Debug.i ( TAG, "==>onStop" );
            ( ( AudioManager ) getSystemService ( AUDIO_SERVICE ) )
            .abandonAudioFocus ( null );
            super.onStop();
        }

        @Override
        public void onPause() {
            isRunning = false;
            mPlayer.onPause();
            super.onPause();
            mPlayerStopReceiver.unregister();
            SystemProperties.set ( "media.amplayer.buffertime", mMediaBuffer );
            mWakeLock.release();
        }

        @Override
        public void onResume() {
            mMediaBuffer = SystemProperties.get ( "media.amplayer.buffertime" );
            SystemProperties.set ( "media.amplayer.buffertime", "6" );
            isRunning = true;
            Intent intent = getIntent();
            Debug.d ( TAG, "" + intent.getData().toString() );
            if ( mUri.compareTo ( intent.getData() ) == 0 ) {
                mPlayer.onResume();
            } else {
                Debug.d ( TAG, "equal to previous url" );
                mUri = intent.getData();
                int startPos = intent.getIntExtra ( "start-position", 0 );
                boolean isPhoto = intent.getBooleanExtra ( "isphoto", false );
                mPlayer.onPlayNext ( mUri, startPos, isPhoto );
            }
            super.onResume();
            mPlayerStopReceiver.register();
            AirplayController.mSync.unlock();
            Debug.i ( TAG, "==>onResume, unlock" );
            /* enable backlight */
            PowerManager pm = ( PowerManager ) getSystemService ( Context.POWER_SERVICE );
            mWakeLock = pm.newWakeLock ( PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG );
            mWakeLock.acquire();
        }

        @Override
        public void onNewIntent ( Intent intent ) {
            Debug.d ( TAG, "!!!onNewIntent" );
            super.onNewIntent ( intent );
            setIntent ( intent );
        }

        @Override
        public void onSaveInstanceState ( Bundle outState ) {
            super.onSaveInstanceState ( outState );
            mPlayer.onSaveInstanceState ( outState );
        }

        @Override
        public void onDestroy() {
            Debug.i ( TAG, "==>onDestroy, unlock" );
            AirplayController.mSync.unlock();
            mPlayer.onDestroy();
            super.onDestroy();
        }

        @Override
        public boolean onKeyDown ( int keyCode, KeyEvent event ) {
            return mPlayer.onKeyDown ( keyCode, event )
                   || super.onKeyDown ( keyCode, event );
        }

        @Override
        public boolean onKeyUp ( int keyCode, KeyEvent event ) {
            if ( keyCode == KeyEvent.KEYCODE_BACK ) {
                finish();
                mPlayer.mAirplayProxy.postMoviePlayerState ( HttpVideoHandler.VIDEO_STOP );
                return true;
            }
            return mPlayer.onKeyUp ( keyCode, event ) || super.onKeyUp ( keyCode, event );
        }

        private class PlayerStopReceiver extends BroadcastReceiver {
                public void register() {
                    IntentFilter filter = new IntentFilter (
                        AirplayBroadcastFactory.ACTION_PLAYER_CONTROL );
                    filter.addAction ( AirplayBroadcastFactory.ACTION_PLAYER_SEEK );
                    filter.addAction ( AirplayBroadcastFactory.ACTION_PLAYER_NEXT );
                    mContext.registerReceiver ( this, filter );
                }

                public void unregister() {
                    mContext.unregisterReceiver ( this );
                }

                @Override
                public void onReceive ( Context context, Intent intent ) {
                    Debug.i ( TAG, "onReceive, " + intent.getAction() );
                    if ( AirplayBroadcastFactory.ACTION_PLAYER_CONTROL.equals ( intent
                            .getAction() ) ) {
                        Bundle extras = intent.getExtras();
                        if ( extras != null ) {
                            String state = extras
                                           .getString ( AirplayBroadcastFactory.PLAY_STATE );
                            if ( AirplayBroadcastFactory.PLAY_STATE_STOP.equals ( state ) ) {
                                Debug.i ( TAG, "==>finish" );
                                MovieActivity.this.finish();
                            } else if ( AirplayBroadcastFactory.PLAY_STATE_PLAY
                                        .equals ( state ) ) {
                                if ( mPlayer != null )
                                { mPlayer.onPlayPause ( true ); }
                            } else if ( AirplayBroadcastFactory.PLAY_STATE_PAUSE
                                        .equals ( state ) ) {
                                if ( mPlayer != null )
                                { mPlayer.onPlayPause ( false ); }
                            }
                        }
                    } else if ( AirplayBroadcastFactory.ACTION_PLAYER_SEEK.equals ( intent
                                .getAction() ) ) {
                        Bundle extras = intent.getExtras();
                        if ( extras != null ) {
                            int pos = extras.getInt ( AirplayBroadcastFactory.SEEK_POS );
                            if ( mPlayer != null )
                            { mPlayer.onPlayerSeek ( pos ); }
                        }
                    } else if ( AirplayBroadcastFactory.ACTION_PLAYER_NEXT.equals ( intent
                                .getAction() ) ) {
                        Bundle extras = intent.getExtras();
                        if ( extras != null ) {
                            Uri videoUri = null;
                            String uriString = intent.getStringExtra ( "uri-string" );
                            if ( uriString != null ) {
                                videoUri = Uri.parse ( uriString );
                            }
                            int startPos = intent.getIntExtra ( "start-position", 0 );
                            boolean isPhoto = intent.getBooleanExtra ( "isphoto", false );
                            if ( mPlayer != null && videoUri != null )
                            { mPlayer.onPlayNext ( videoUri, startPos, isPhoto ); }
                        }
                    }
                }
        }
}
