package com.droidlogic.mediacenter.airplay.service;

import org.phlo.AirReceiver.AirReceiver;
import org.phlo.AirReceiver.AudioListener;
import org.phlo.AirReceiver.ImageListener;
import org.phlo.AirReceiver.VideoListener;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;

import com.droidlogic.mediacenter.airplay.proxy.AirplayBroadcastFactory;
import com.droidlogic.mediacenter.airplay.proxy.AirplayController;
import com.droidlogic.mediacenter.airplay.proxy.AirplayProxy;
import com.droidlogic.mediacenter.airplay.proxy.IAirplayListener;
import com.droidlogic.mediacenter.airplay.proxy.AirplayController.EventInfo;
import com.droidlogic.mediacenter.airplay.proxy.AirplayController.EventType;
import com.droidlogic.mediacenter.airplay.setting.SettingsPreferences;
import com.droidlogic.mediacenter.airplay.util.Utils;
import android.widget.Toast;
import android.text.format.Time;
import android.util.Log;
import android.content.Context;
import android.os.PowerManager;
import com.droidlogic.mediacenter.R;
import com.amlogic.util.Debug;

public class AirReceiverService extends Service implements IAirplayListener,
        ImageListener, VideoListener, AudioListener {
        private static final boolean    DEBUG                    = true;
        private static final String     TAG                      = "AirReceiverService";
        private boolean                 isStart;
        public static final String      ACTION_START_AIRRSERVICE = "com.example.airplay.START_AIRSERVICE";
        public static final String      ACTION_START_AIRRECEIVER = "com.example.airplay.START_AIRRECEIVER";
        public static final String      ACTION_STOP_AIRRECEIVER  = "com.example.airplay.STOP_AIRRECEIVER";
        public static final String      ACTION_RESET_AIRRECEIVER = "com.example.airplay.RESET_AIRRECEIVER";
        private AirplayProxy            mAirplayProxy;
        private AirplayController       mController;
        private AirplayBroadcastFactory mBrocastFactory;
        private PowerManager            mPWSvc;

        @Override
        public IBinder onBind ( Intent arg0 ) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            if ( DEBUG )
            { Debug.i ( TAG, "onCreate-------------------------------------" ); }
            isStart = false;
            init();
        }

        @Override
        public void onDestroy() {
            isStart = false;
            super.onDestroy();
            unInit();
            Debug.i ( TAG, "<==service onDestroy" );
        }

        @Override
        public int onStartCommand ( Intent intent, int flags, int startId ) {
            if ( DEBUG )
            { Debug.i ( TAG, "onStartCommand-----" + intent ); }
            if ( intent != null && intent.getAction() != null ) {
                String action = intent.getAction();
                if ( ACTION_START_AIRRECEIVER.equals ( action ) ) {
                    try {
                        Thread.sleep ( 2000 );
                    } catch ( Exception e ) {}
                    startReceiver();
                } else if ( ACTION_STOP_AIRRECEIVER.equals ( action ) ) {
                    stopReceiver();
                } else if ( ACTION_START_AIRRSERVICE.equals ( action ) ) {
                    ;
                } else if ( ACTION_RESET_AIRRECEIVER.equals ( action ) ) {
                    resetReceiver();
                }
            }
            return super.onStartCommand ( intent, flags, startId );
        }

        private void stopReceiver() {
            if ( AirReceiver.isRunning ) {
                Debug.d ( TAG, "stop" );
                new Thread ( mStopReceiver ).start();
            }
        }

        private void startReceiver() {
            if ( !AirReceiver.isRunning && Utils.isNetworkConnected ( this ) ) {
                Debug.d ( TAG, "startReceiver-----" );
                isStart = true;
                new Thread ( mStartReceiver ).start();
            }
        }

        private void resetReceiver() {
            if ( Utils.isNetworkConnected ( this ) && AirReceiver.isRunning ) {
                new Thread ( mResetReceiver ).start();
            }
        }

        private final Runnable mStartReceiver = new Runnable() {
            @Override
            public synchronized void run() {
                try {
                    if ( !AirReceiver.isRunning )
                        AirReceiver
                        .start ( Utils
                                 .getSavedDeviceName ( AirReceiverService.this ) );
                } catch ( Exception e ) {
                    // TODO Auto-generated
                    // catch block
                    e.printStackTrace();
                }
            }
        };
        private final Runnable mStopReceiver  = new Runnable() {
            @Override
            public synchronized void run() {
                try {
                    Debug.i ( TAG,
                              "*******AirReceiver.isRunning="
                              + AirReceiver.isRunning );
                    if ( AirReceiver.isRunning )
                    { AirReceiver.stop(); }
                } catch ( Exception e ) {
                    // TODO Auto-generated
                    // catch block
                    e.printStackTrace();
                }
            }
        };
        private final Runnable mResetReceiver = new Runnable() {
            @Override
            public synchronized void run() {
                try {
                    if ( AirReceiver.isRunning ) {
                        AirReceiver.stop();
                        AirReceiver
                        .start ( Utils
                                 .getSavedDeviceName ( AirReceiverService.this ) );
                    }
                } catch ( Exception e ) {
                    // TODO Auto-generated
                    // catch block
                    e.printStackTrace();
                }
            }
        };

        private void init() {
            mAirplayProxy = AirplayProxy.getInstance ( this );
            mController = AirplayController.getInstance ( this );
            mBrocastFactory = new AirplayBroadcastFactory ( this );
            AirReceiver.registerImageListener ( this );
            AirReceiver.registerVideoListener ( this );
            AirReceiver.registerAudioListener ( this );
            AirReceiver.addSlideShowTheme ( "Simple Fade", "Fade" );
            AirReceiver.addSlideShowTheme ( "Simple Slide", "Slide" );
            AirReceiver.addSlideShowTheme ( "Quad", "Quad" );
            /**
             * power manager
             */
            Context mContext = getApplicationContext();
            mPWSvc = ( PowerManager ) mContext
                     .getSystemService ( Context.POWER_SERVICE );
            mBrocastFactory.registerListener ( this );
        }

        private void unInit() {
            AirReceiver.registerImageListener ( null );
            AirReceiver.registerVideoListener ( null );
            AirReceiver.registerAudioListener ( null );
            AirReceiver.clearSlideShowTheme();
            mBrocastFactory.unRegisterListener();
        }

        @Override
        public void onNetworkStateChange ( boolean connect ) {
            if ( DEBUG ) {
                Debug.i ( TAG, "onNetworkStateChange------------------connect--"
                          + connect );
            }
            if ( connect ) {
                SharedPreferences prefs = Utils.getSharedPreferences ( this );
                boolean startService  = prefs.getBoolean ( SettingsPreferences.KEY_START_SERVICE, false );
                if ( startService ) { startReceiver(); }
            } else {
                Toast.makeText ( getApplicationContext(), getApplicationContext()
                                 .getString ( R.string.network_disconnected ),
                                 Toast.LENGTH_LONG );
                stop();
                stopReceiver();
            }
        }

        @Override
        public void onAirplayDeviceStateChange() {
            // TODO Auto-generated method stub
        }

        @Override
        public void onAirplayPlayStateChange() {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStorageStateChange() {
            // TODO Auto-generated method stub
        }

        // AudioListener
        @Override
        public void AudioPlay() {
            if ( DEBUG )
            { Debug.i ( TAG, "AudioPlay-------------------------------------" ); }
            mController.sendEvent ( this,
                                    new EventInfo ( EventType.LAUNCH_AUDIO_PLAYER ) );
        }

        @Override
        public void AudioProgressRefresh ( Float arg0, Float arg1 ) {
            if ( DEBUG )
            { Debug.i ( TAG, "AudioProgressRefresh-------" + arg0 + "---" + arg1 ); }
            mController.sendEvent ( this, new EventInfo (
                                        EventType.UPDATE_AUDIO_PROGRESS, arg0, arg1 ) );
        }

        @Override
        public void setInfo ( String arg0, String arg1 ) {
            if ( DEBUG )
            { Debug.i ( TAG, "setInfo-------" + arg0 + "---" + arg1 ); }
            mController.sendEvent ( this, new EventInfo ( EventType.UPDATE_AUDIO_INFO,
                                    arg0, arg1 ) );
        }

        @Override
        public void AudioStop() {
            if ( DEBUG )
            { Debug.i ( TAG, "AudioStop-------------------------------------" ); }
            mController.sendEvent ( this, new EventInfo ( EventType.EXIT_AUDIO_PLAYER ) );
        }

        @Override
        public void AudioVolumeChanged ( String arg0 ) {
            if ( DEBUG )
                Debug.i ( TAG,
                          "AudioVolumeChanged-------------------------------------" );
        }

        // VideoListener
        @Override
        public String getCurPosition() {
            String positon = mAirplayProxy.getCurPosition();
            if ( DEBUG )
            { Debug.i ( TAG, "getCurPosition-------" + positon ); }
            return positon;
        }

        @Override
        public String getDuration() {
            String duration = mAirplayProxy.getDuration();
            if ( DEBUG )
            { Debug.i ( TAG, "getDuration------------" + duration ); }
            return duration;
        }

        @Override
        public double getRate() {
            double state = mAirplayProxy.getRate();
            if ( DEBUG )
            { Debug.i ( TAG, "getPlayState------------" + state ); }
            return state;
        }

        @Override
        public boolean getReadyToPlay() {
            return mAirplayProxy.getReadyToPlay();
        }

        @Override
        public void play ( String arg0, String arg1, boolean isPhoto ) {
            if ( DEBUG )
            { Debug.i ( TAG, "play--------" + arg0 + "---" + arg1 ); }
            if ( arg1.indexOf ( "," ) != -1 )
            { arg1 = arg1.replace ( ',', '.' ); }
            float posf = Float.parseFloat ( arg1 ); // percent val
            //mPWSvc.wakeUp(SystemClock.uptimeMillis());
            //stop audio&image first
            ImageStop();
            AudioStop();
            mController.sendEvent ( this, new EventInfo (
                                        EventType.LAUNCH_VIDEO_PLAYER, arg0, ( int ) ( posf * 100 ),
                                        isPhoto ) );
            mAirplayProxy.setReadyToPlay ( false );
        }

        @Override
        public void seekTo ( String arg0 ) {
            if ( DEBUG )
            { Debug.i ( TAG, "seekTo-------------" + arg0 ); }
            float posf = Float.parseFloat ( arg0 );
            mController.sendEvent ( this, new EventInfo ( EventType.VIDEO_PLAYER_SEEK,
                                    ( int ) ( posf * 1000 ) ) );
        }

        @Override
        public void setRate ( String arg0 ) {
            if ( DEBUG )
            { Debug.i ( TAG, "setRate-------------" + arg0 ); }
            float rate = Float.parseFloat ( arg0 );
            if ( rate == 0 )
                mController.sendEvent ( this, new EventInfo (
                                            EventType.VIDEO_PLAYER_PAUSE ) );
            else if ( rate == 1 )
                mController.sendEvent ( this, new EventInfo (
                                            EventType.VIDEO_PLAYER_PLAY ) );
        }

        @Override
        public void setVolume ( String arg0 ) {
            if ( DEBUG )
            { Debug.i ( TAG, "setVolume-------------" + arg0 ); }
        }

        @Override
        public void stop() {
            if ( DEBUG )
            { Debug.i ( TAG, "stop-------------" ); }
            mController.sendEvent ( this, new EventInfo ( EventType.EXIT_VIDEO_PLAYER ) );
        }

        // ImageListener
        @Override
        public void ImageShow ( int arg0 ) {
            if ( DEBUG )
            { Debug.i ( TAG, "ImageShow-------------" + arg0 ); }
            //mPWSvc.wakeUp(SystemClock.uptimeMillis());
            mController.sendEvent ( this, new EventInfo (
                                        EventType.LAUNCH_IMAGE_PLAYER, arg0 ) );
        }

        // ImageListener
        @Override
        public void ImageStop() {
            if ( DEBUG )
            { Debug.i ( TAG, "ImageStop-------------" ); }
            mController.sendEvent ( this, new EventInfo ( EventType.EXIT_IMAGE_PLAYER ) );
        }

        @Override
        public void ImageLoading() {
            if ( DEBUG )
            { Debug.i ( TAG, "ImageLoading-------------" ); }
        }

        @Override
        public void SlideshowStart() {
            if ( DEBUG )
            { Debug.i ( TAG, "Image SlideShow----------" ); }
        }

        private int getAndroidSDKVersion() {
            int version = 0;
            try {
                version = Integer.valueOf ( android.os.Build.VERSION.SDK );
            } catch ( NumberFormatException e ) {
            }
            return version;
        }
}
