package com.droidlogic.mediacenter.airplay;


import com.hpplay.happyplay.AllCast;
import com.hpplay.happyplay.OnAirPlayEventListener;
import com.hpplay.happyplay.PlaybackState;
import com.hpplay.happyplay.iControl;
import com.hpplay.happyplay.mainConst;
import com.droidlogic.mediacenter.dlna.MediaCenterService;
import com.droidlogic.mediacenter.dlna.PrefUtils;

import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;

import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.droidlogic.mediacenter.MediaCenterApplication;
import com.droidlogic.mediacenter.R;
import java.io.UnsupportedEncodingException;

public class AirPlayService extends Service {
        private String TAG = "AirPlayService";
        private AllCast mAllCast;
        private int mSN = 0;
        public static boolean STATEDFLAG = false;

        private String mType = "AIRPLAY";
        private String mSessionID = "";
        private AudioManager mAudioManager;
        private OnAirPlayEventListener eventlistener;
        private int mStartPosition;
        private float mStartPlayPosition = 0;
        private String mUri;
        private String mDeviceName;
        private VideoSession mVideoSession = VideoSession.getInstance();
        private int mID = 0;
        private int mDisplayMode;
        private boolean isRegistReceiver = false;
        private boolean isairplay, isdlna, isfps;
        private PrefUtils mPrefutils;
        private String mServiceName;
        private MediaCenterApplication mApp;

        public final String ACTION_MIRROR_START = "com.hpplaysdk.happyplay.MIRROR_START";
        public final String ACTION_MIRROR_STOP = "com.hpplaysdk.happyplay.MIRROR_STOP";
        private static final String mOMXDisplaymode = (String)PrefUtils.getProperties("media.omx.display_mode", "0");

        @Override
        public IBinder onBind ( Intent intent ) {
            return null;
        }

        private BroadcastReceiver mAirplayServiceListener = new BroadcastReceiver() {
            @Override
            public void onReceive ( Context context, Intent intent ) {
                if ( intent == null ) {
                    return;
                }
                String action = intent.getAction();
                if ( action == null ) {
                    return;
                }
                Log.d ( TAG, "onReceive===============" + action );
                if ( action.equals ( MediaCenterService.SERVICE_NAME_CHANGE ) ) {
                    // stopAirplayService();
                    Log.d ( TAG, "change name: " + intent.getStringExtra ( "service_name" ) );
                    if ( null != mAllCast && mAllCast.isAirPlayRuning() ) {
                        mAllCast.changeDeviceName ( "Airplay-" + intent.getStringExtra ( "service_name" ), true );
                    }
                    // mAllCast.startDaemonService();
                } else if ( action.equals ( "com.hpplaysdk.happyplay.MIRROR_START" ) ) {
                    Log.i( TAG, "Mirror Started" );
                    PrefUtils.setProperties("media.omx.display_mode", "0");
                } else if ( action.equals ( "com.hpplaysdk.happyplay.MIRROR_STOP" ) ) {
                    Log.i( TAG, "Mirror Stopped" );
                    PrefUtils.setProperties("media.omx.display_mode", mOMXDisplaymode);
                }
            }
        };

        @Override
        public void onCreate() {
            super.onCreate();
            init();
        }

        private void init() {
            // mAllCast = new AllCast(this, ic);
            mApp = ( MediaCenterApplication ) getApplication();
            mAllCast = mApp.getCastInstance ( ic );
            Log.i ( TAG, "SDK Version: " + mAllCast.getVersion() );
            mAllCast.setAirPlayPassword ( "" );
            mAllCast.setScreenCodeAuthorizedMode ( false );
            mAllCast.setPhotoDisplayMode ( false );
            mAllCast.setPublishType ( true );
            mAllCast.setPublishServiceSwith ( true );

            mAllCast.setAirPlayTXTRecord ( null, 0 );
            mAllCast.setRAOPTXTRecord ( null, 0 );
            // mAllCast.setDeviceName("AirPlay");
            mPrefutils = new PrefUtils ( this );
            mServiceName =  mPrefutils.getString ( PrefUtils.SERVICE_NAME, null );
            if ( mServiceName == null ) {
                mServiceName = this.getResources().getString (
                                   R.string.config_default_name );
            }
            Log.i ( TAG, "airplay name: Airplay-" + mServiceName );
            mAllCast.setDeviceName (  "Airplay-" + mServiceName, true );
            isairplay = true;
            mAllCast.setAirPlaySwitch ( isairplay );
            isdlna = false;
            mAllCast.setDMRSwitch ( isdlna );
            isfps = true;
            mAllCast.setDisplayFrameRateSwitch ( isfps );
            mAllCast.setMirrorResolution ( "1280*720" );
            mDisplayMode = 1;
            mAllCast.setMirrorDisplayMode ( mDisplayMode );
            // mAllCast.startDaemonService();
        }

        private void stopAirplayService() {
            while ( mHandler.hasMessages ( 0 ) ) {
                mHandler.removeMessages ( 0 );
            }
            mAllCast.stopAirPlayService();
            // mAllCast.stopDaemonService();
        }

        @Override
        public void onDestroy() {
            Log.d ( TAG, "onDestroy////////////////////////////" + mServiceName + Log.getStackTraceString ( new Throwable() ) );
            // mCheckDaemon.removeMessages ( 0 );
            super.onDestroy();
            if ( isRegistReceiver ) {
                unregisterReceiver ( mAirplayServiceListener );
                isRegistReceiver = false;
            }
            stopAirplayService();
            // mCheckDaemon = null;
        }

        @Override
        public int onStartCommand ( Intent intent, int flags, int startId ) {
            if ( intent == null ) {
                Log.d ( TAG, "onStartCommand////////////////////////////intent is null.");
            } else {
                Log.d ( TAG, "onStartCommand////////////////////////////" + intent.getAction() );
            }
            if ( !isRegistReceiver ) {
                IntentFilter f = new IntentFilter();
                f.addAction ( MediaCenterService.SERVICE_NAME_CHANGE );
                f.addAction ( ACTION_MIRROR_START );
                f.addAction ( ACTION_MIRROR_STOP );
                registerReceiver ( mAirplayServiceListener, f );
                isRegistReceiver = true;
            }
            mServiceName =  mPrefutils.getString ( PrefUtils.SERVICE_NAME, null );
            if ( mServiceName == null ) {
                mServiceName = this.getResources().getString (
                                   R.string.config_default_name );
            }
            mAllCast.setDeviceName (  "Airplay-" + mServiceName, true );
            // mCheckDaemon.removeMessages ( 0 );
            // mCheckDaemon.sendEmptyMessageDelayed ( 0, 1000 );
            mHandler.sendEmptyMessage ( 0 );
            // mAllCast.startDaemonService();
            super.onStartCommand ( intent, flags, startId );
            return START_STICKY;
        }
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage ( Message msg ) {
                switch ( msg.what ) {
                    case 0:
                        startAirplayService();
                        break;
                }
                // TODO Auto-generated method stub
                super.handleMessage ( msg );
            }
        };

        private boolean startAirplayService() {
            boolean isstart = true;
            Log.d ( TAG, "mApp.isDaemonRun()" + mApp.isDaemonRun() + "mAllCast.isAirPlayRuning()" + mAllCast.isAirPlayRuning() );
            if ( !mApp.isDaemonRun() ) {
                mApp.startDaemon ( mAllCast );
                STATEDFLAG = true;
            }
            if ( !mAllCast.isAirPlayRuning() ) {
                isstart = mAllCast.startAirPlayService();
            }
            Log.d ( TAG, "mAllCast.startAirPlayService() return" + isstart + "running?" + mAllCast.isAirPlayRuning() );
            if ( !isstart || !mAllCast.isAirPlayRuning() ) {
                mHandler.sendEmptyMessageDelayed ( 0, 3000 );
            }
            return isstart;
        }

        private static byte[] getAsciiBytes(String data) {
            if (data == null) {
                throw new IllegalArgumentException("Parameter may not be null");
            }

            try {
                return data.getBytes("US-ASCII");
            } catch (UnsupportedEncodingException e) {
                throw new Error("HttpClient requires ASCII support");
            }
        }

        // put string key pairs to byte[]
        private int putstrtobyte ( byte[] ba, String key, String value, int begin ) {
            ba[begin] = ( byte ) ( key.length() + value.length() );
            int start = begin + 1;
            byte[] bb = getAsciiBytes ( key );
            int bb_len = bb.length;
            System.arraycopy ( bb, 0, ba, start, bb_len );
            start = start + bb.length;
            bb = getAsciiBytes ( value );
            bb_len = bb.length;
            System.arraycopy ( bb, 0, ba, start, bb_len );
            start = start + bb.length;
            return start;
        }

        private iControl ic = new iControl() {
            // ///////////////////////
            // following is allcast implements
            // //////////////////////
            @Override
            public void setVideoUrl ( String mSessionId, String mUrl, float mStart,
            String type, String devicename, OnAirPlayEventListener postevent ) {
                Log.i ( TAG, "setVideoUrl   videoactive=" + mVideoSession.isActive + "mSessionId:" + mSessionId + "mUrl:" + mUrl
                        + "mStart:" + mStart + "type:" + type + "devicename:" + devicename );
                // Log.i(TAG,mUrl);
                if ( mVideoSession.isActive ) {
                    playbackEvent ( "stop", null, mVideoSession.mCurrentSessionID );
                    try {
                        Thread.sleep ( 200 );
                    } catch ( InterruptedException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                mVideoSession.isActive = false;
                mSessionID = mSessionId;
                // AIRPLAY or DLNA
                mType = type;
                mDeviceName = devicename;
                if ( !mDeviceName.equals ( "" ) ) {
                    Log.i ( TAG, "Media From " + mDeviceName );
                }
                mStartPlayPosition = mStart;
                eventlistener = postevent;
                mUri = mUrl;
                if ( !mVideoSession.isActive ) {
                    startVideoPlayer ( mUri, mStartPlayPosition, mType, mSessionID );
                    try {
                        Thread.sleep ( 300 );
                    } catch ( InterruptedException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mVideoSession.isActive = true;
                }
                if ( null != eventlistener ) {
                    eventlistener.OnPostEvent ( mSessionId, PlaybackState.LOADING, null );
                }
            }
            @Override
            public void play ( String mSessionId ) {
                Log.i ( TAG, "play mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return;
                if ( !mVideoSession.isActive ) {
                    startVideoPlayer ( mUri, mStartPlayPosition, mType, mSessionID );
                    try {
                        Thread.sleep ( 1000 );
                    } catch ( InterruptedException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mVideoSession.isActive = true;
                }
                playbackEvent ( "play", null, mSessionId );
                if ( null != eventlistener ) {
                    eventlistener.OnPostEvent ( mSessionId, PlaybackState.PLAYING, null );
                }
            }
            @Override
            public void pause ( String mSessionId ) {
                Log.i ( TAG, "pause mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return;
                playbackEvent ( "pause", null, mSessionId );
                if ( null != eventlistener ) {
                    eventlistener.OnPostEvent ( mSessionId, PlaybackState.PAUSED, null );
                }
            }
            @Override
            public void stop ( String mSessionId ) {
                Log.i ( TAG, "stop mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return;
                playbackEvent ( "stop", null, mSessionId );
                if ( null != eventlistener ) {
                    eventlistener.OnPostEvent ( mSessionId, PlaybackState.STOPPED,
                                                PlaybackState.STOPPED );
                }
            }
            @Override
            public boolean isPlaying ( String mSessionId ) {
                Log.i ( TAG, "isPlaying "+mVideoSession.isPlaying+",      mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return false;
                // if (!mIsPrepared)
                // return false;
                return mVideoSession.isPlaying;
            }
            @Override
            public int getDuration ( String mSessionId ) {
                //Log.i ( TAG, "getDuration mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return 0;
                // if (!mIsPrepared)
                // return 0;
                return mVideoSession.mCurrentDuration;
            }
            @Override
            public int getCurrentPosition ( String mSessionId ) {
                //Log.i ( TAG, "getCurrentPosition mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return 0;
                // if (!mIsPrepared)
                // return 0;
                return mVideoSession.mCurrentPosition;
            }
            @Override
            public void seekTo ( String mSessionId, int mPosition ) {
                Log.i ( TAG, "seekTo mSessionId:" + mSessionId + "mSessionId:"
                        + mPosition + "ms" );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return;
                // if (!mIsPrepared)
                // return;
                playbackEvent ( "seek", "" + mPosition, mSessionId );
            }
            @Override
            public void setVolume ( String mSessionId, int mVolume ) {
                Log.i ( TAG, "setVolume mSessionId:" + mSessionId + " mVolume:"
                        + mVolume );
                // Log.i(TAG,mSessionId);
                mAudioManager.setStreamVolume ( AudioManager.STREAM_SYSTEM, mVolume,
                                                AudioManager.FLAG_SHOW_UI );
            }
            @Override
            public void setMute ( String mSessionId, boolean mute ) {
                Log.i ( TAG, "setMute mSessionId:" + mSessionId + "mute?" + mute );
                // Log.i(TAG,mSessionId);
                mAudioManager.setStreamMute ( AudioManager.STREAM_SYSTEM, mute );
            }
            @Override
            public void onScreenCodeShow ( String mScreenCode, int mTimeout ) {
                // reserved
                Log.i ( TAG, "mScreenCode=" + mScreenCode + ",show dialog" );
                Log.i ( TAG, "mTimeout=" + mTimeout + "ms" );
            }
            @Override
            public void onScreenCodeDispose ( String mScreenCode ) {
                // reserved
                Log.i ( TAG, "mScreenCode=" + mScreenCode + ",dialog disposed" );
            }
            @Override
            public void onPhotoDisplay ( String mSessionId, String path, String mDeviceName ) {
                // reserved
                Log.i ( TAG, "onPhotoDisplay,mSessionId=" + mSessionId + ",path="
                                + path + ",mDeviceName=" + mDeviceName );
                if ( !MediaCenterApplication.getPhoto() ) {
                    startPhotoPlayer ( path, mDeviceName, mSessionId );
                } else {
                    Intent intent = new Intent ( "displayimage" );
                    Bundle bundle = new Bundle();
                    bundle.putString ( "IMAGE", path ); // url
                    bundle.putString ( "SOURCE", mDeviceName ); // AIRPLAY OR DLNA
                    bundle.putString ( "SESSIONID", mSessionId );
                    intent.putExtras ( bundle );
                    sendBroadcast ( intent );
                }
            }
            @Override
            public void onPhotoDispose() {
                // reserved
                Log.i ( TAG, "onPhotoDispose" );
                Intent intent = new Intent ( "stopimage" );
                sendBroadcast ( intent );
            }
        };

        private void startPhotoPlayer ( String url, String clientname,
                                        String sessionid ) {
            Intent intent = new Intent ( "displayimage" );
            Bundle bundle = new Bundle();
            bundle.putString ( "IMAGE", url ); // url
            bundle.putString ( "SOURCE", clientname ); // AIRPLAY OR DLNA
            bundle.putString ( "SESSIONID", sessionid );
            intent.putExtras ( bundle );
            intent.setClass ( this, Photo.class );
            intent.setFlags ( Intent.FLAG_ACTIVITY_NEW_TASK );
            startActivity ( intent );
        }

        //
        private void startVideoPlayer ( String url, float start, String type,
                                        String sessionid ) {
            Log.d ( TAG, "startVideoPlayer url:" + url + "start" + start + "type:"
                    + type + "sessionid:" + sessionid );
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString ( "URL", url ); // url
            bundle.putFloat ( "SP", start ); // start position
            bundle.putString ( "TYPE", type ); // AIRPLAY OR DLNA
            bundle.putString ( "SESSIONID", sessionid );
            intent.putExtras ( bundle );
            intent.setClass ( this, VideoPlayer.class );
            intent.setFlags ( Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK );
            startActivity ( intent );
        }

        //
        private void playbackEvent ( String action, String seekTo, String sessionid ) {
            Log.d ( TAG, "==================playbackEvent action:" + action
                    + "seekTo" + seekTo + "sessionid:" + sessionid );
            Intent intent = new Intent ( action );
            Bundle bundle = new Bundle();
            bundle.putString ( "SID", sessionid );
            if ( seekTo != null ) {
                int seekto = Integer.valueOf ( seekTo );
                bundle.putInt ( "seekTo", seekto );
            }
            intent.putExtras ( bundle );
            sendBroadcast ( intent );
        }
        //
        private void startVideoPlayer1(String url,float start,String type,String sessionid)
        {
            //MiniLog.d(TAG, "startVideoPlayer");
            Intent intent = new Intent("ready");
            Bundle bundle = new Bundle();
            bundle.putString("URL", url);
            bundle.putFloat("SP", start);
            bundle.putString("TYPE", type);
            bundle.putString("SESSIONID",sessionid);
            intent.putExtras(bundle);
            intent.setClass(this, VideoPlayer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendBroadcast(intent);
        }
}
