package com.droidlogic.mediacenter.airplay;

import nz.co.iswe.android.airplay.audio.AudioCmdClient;

import com.amlogic.util.Debug;
import com.droidlogic.mediacenter.airplay.proxy.AirplayBroadcastFactory;
import com.droidlogic.mediacenter.airplay.proxy.AirplayProxy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.droidlogic.mediacenter.R;

public class MusicActivity extends Activity
{
        public static final String           TAG             = "MusicActivity";
        private static final OnClickListener OnClickListener = null;
        public Context                       mContext;
        public MusicInfoReceiver             mInfoReceiver;
        private AirplayProxy                 mProxy;
        private PowerManager.WakeLock        mWakeLock;
        private static boolean               running         = false;
        private boolean                      isPlaying;
        public TextView                      mTitle;
        public TextView                      mArtist;
        public ImageButton                   mPrev;
        public ImageButton                   mPlayPause;
        public ImageButton                   mStop;
        public ImageButton                   mNext;
        @Override
        public void onCreate ( Bundle savedInstanceState )
        {
            super.onCreate ( savedInstanceState );
            setContentView ( R.layout.music_airplay );
            mContext = MusicActivity.this;
            mProxy = AirplayProxy.getInstance ( mContext );
            mInfoReceiver = new MusicInfoReceiver();
            /* LayoutParams params = getWindow().getAttributes();
             params.gravity=Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
             getWindow().setAttributes(params);*/
            getWindow().setGravity ( Gravity.BOTTOM | Gravity.FILL_HORIZONTAL );
        }
        
        @Override
        protected void onStart()
        {
            super.onStart();
        }
        
        @Override
        protected void onPause()
        {
            super.onPause();
            mProxy.postMusicPlayState ( AudioCmdClient.AUDIO_STOP_URI );
            mInfoReceiver.unregister();
            mWakeLock.release();
        }
        
        @Override
        protected void onStop()
        {
            super.onStop();
            running = false;
        }
        
        @Override
        protected void onDestroy()
        {
            super.onDestroy();
        }
        
        @Override
        protected void onResume()
        {
            mInfoReceiver.register();
            super.onResume();
            /* enable backlight */
            PowerManager pm = ( PowerManager ) getSystemService ( Context.POWER_SERVICE );
            mWakeLock = pm.newWakeLock ( PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                         | PowerManager.ON_AFTER_RELEASE, TAG );
            mWakeLock.acquire();
            mInfoReceiver.register();
            mTitle = ( TextView ) findViewById ( R.id.music_title );
            mArtist = ( TextView ) findViewById ( R.id.music_actor );
            // mPoster.setAlpha(128);
            isPlaying = true;
            mPrev = ( ImageButton ) findViewById ( R.id.btn_pre );
            mPrev.setOnClickListener ( new View.OnClickListener()
            {
                public void onClick ( View view )
                {
                    Debug.d ( TAG, "prev clicked" );
                    new Thread()
                    {
                        public void run()
                        {
                            mProxy.postMusicPlayState ( AudioCmdClient.AUDIO_ITEM_PREV );
                        }
                    } .start();
                }
            } );
            mNext = ( ImageButton ) findViewById ( R.id.btn_next );
            mNext.setOnClickListener ( new View.OnClickListener()
            {
                public void onClick ( View view )
                {
                    new Thread()
                    {
                        public void run()
                        {
                            mProxy.postMusicPlayState ( AudioCmdClient.AUDIO_ITEM_NEXT );
                        }
                    } .start();
                }
            } );
            mPlayPause = ( ImageButton ) findViewById ( R.id.btn_play_pause );
            mPlayPause.setOnClickListener ( new View.OnClickListener()
            {
                public void onClick ( View view )
                {
                    Debug.d ( TAG, "play_pause clicked" );
                    new Thread()
                    {
                        public void run()
                        {
                            if ( isPlaying )
                            {
                                mProxy.postMusicPlayState ( AudioCmdClient.AUDIO_PAUSE_URI );
                                isPlaying = false;
                            }
                            else
                            {
                                mProxy.postMusicPlayState ( AudioCmdClient.AUDIO_PLAY_URI );
                                isPlaying = true;
                            }
                        }
                    } .start();
                    
                    if ( isPlaying )
                    { mPlayPause.setImageResource ( R.drawable.style_play ); }
                    else
                    { mPlayPause.setImageResource ( R.drawable.style_pause ); }
                }
            } );
            mStop = ( ImageButton ) findViewById ( R.id.btn_stop );
            mStop.setOnClickListener ( new View.OnClickListener()
            {
                public void onClick ( View view )
                {
                    Debug.d ( TAG, "stop clicked" );
                    new Thread()
                    {
                        public void run()
                        {
                            mProxy.postMusicPlayState ( AudioCmdClient.AUDIO_STOP_URI );
                            //mHandler.sendEmptyMessageDelayed(0, 5000);
                        }
                    } .start();
                }
            } );
            ImageButton mExit = ( ImageButton ) findViewById ( R.id.btn_back );
            mExit.setOnClickListener ( new View.OnClickListener()
            {
                @Override
                public void onClick ( View v )
                {
                    mProxy.postMusicPlayState ( AudioCmdClient.AUDIO_STOP_URI );
                    MusicActivity.this.finish();
                }
            } );
        }
        /*
            @Override
            public void onBackPressed() {
                // mProxy.postMusicPlayState(AudioCmdClient.AUDIO_STOP_URI);
                // finish();
                moveTaskToBack(false);
            }
            */
        private void refreshInfo ( String title, String artist )
        {
            mTitle.setText ( title );
            mArtist.setText ( artist );
        }
        
        private class MusicInfoReceiver extends BroadcastReceiver
        {
                public void register()
                {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction ( AirplayBroadcastFactory.ACTION_UPDATE_INFO );
                    filter.addAction ( AirplayBroadcastFactory.ACTION_MUSIC_EXIT );
                    filter.addAction ( AirplayBroadcastFactory.ACTION_MUSIC_NEXT );
                    mContext.registerReceiver ( this, filter );
                }
                
                public void unregister()
                {
                    mContext.unregisterReceiver ( this );
                }
                
                @Override
                public void onReceive ( Context context, Intent intent )
                {
                    // TODO Auto-generated method stub
                    String action = intent.getAction();
                    
                    if ( AirplayBroadcastFactory.ACTION_UPDATE_INFO.equals ( action ) )
                    {
                        String title = intent.getStringExtra ( "title" );
                        String act = intent.getStringExtra ( "artist" );
                        refreshInfo ( title, act );
                    }
                    else if ( AirplayBroadcastFactory.ACTION_MUSIC_EXIT.equals ( action ) )
                    {
                        Debug.d ( TAG, "==music exit" );
                        mHandler.sendEmptyMessageDelayed ( 0, 2000 );
                    }
                    else if ( AirplayBroadcastFactory.ACTION_MUSIC_NEXT.equals ( action ) )
                    {
                        Debug.d ( TAG, "===onPlayNext" );
                        onPlayNext();
                    }
                }
        }
        
        private Handler mHandler = new Handler()
        {
            @Override
            public void handleMessage ( Message msg )
            {
                if ( msg.what == 0 )
                {
                    Debug.d ( TAG, "handleMessage finish" );
                    finish();
                }
            }
        };
        
        public void onPlayNext()
        {
            Debug.d ( TAG, "onPlayNext" );
            mHandler.removeMessages ( 0 );
        }
        
        public static boolean isRunning()
        {
            return running;
        }
        
}
