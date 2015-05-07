package com.droidlogic.mediacenter.airplay;

import org.apache.http.util.EncodingUtils;

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
import android.net.InterfaceConfiguration;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.ServiceManager;
import android.util.Log;

import com.droidlogic.mediacenter.MediaCenterApplication;
import com.droidlogic.mediacenter.R;

public class AirPlayService extends Service {
        private String TAG = "AirPlayService";
        private AllCast mAllCast;
        private int mSN = 0;
        public static boolean STATEDFLAG = false;
        private byte[] txtairplay = new byte[2048];
        private byte[] txtraop = new byte[2048];
        private int txtairplay_len = 0;
        private int txtraop_len = 0;
        private String mType = "AIRPLAY";
        private String mSessionID = "";
        private AudioManager mAudioManager;
        private OnAirPlayEventListener eventlistener;
        private int mStartPosition;
        private boolean mIsPrepared;
        private float mStartPlayPosition = 0;
        private String mUri;
        private String mDeviceName;
        private VideoSession mVideoSession = VideoSession.getInstance();
        private int mID = 0;
        private int mDisplayMode;
        private String mDeivceMac;
        private boolean isRegistReceiver = false;
        private boolean isairplay, isdlna, isfps;
        private PrefUtils mPrefutils;
        private String mServiceName;
        private MediaCenterApplication mApp;
        //private boolean ServiceStatus = false;
        private boolean isphotoopen = false;

        @Override
        public IBinder onBind ( Intent intent ) {
            return null;
        }

        private BroadcastReceiver mAirplayServiceListener = new BroadcastReceiver() {
            @Override
            public void onReceive ( Context context, Intent intent ) {
                Log.d ( TAG,
                        "onReceive===============" + intent.getAction()
                        + intent.getStringExtra ( "service_name" ) );
                if ( null != mAllCast
                        && mAllCast.isAirPlayRuning()
                && intent.getAction() == MediaCenterService.SERVICE_NAME_CHANGE ) {
                    // stopAirplayService();
                    mAllCast.changeDeviceName ( "Airplay-"
                                                + intent.getStringExtra ( "service_name" ) );
                    // mAllCast.startDaemonService();
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
            boolean usescreencode = mAllCast.getScreenCodeAuthorizedMode();
            String devicepass = mAllCast.getAirPlayPassword();
            boolean useairplaypassword;
            if ( devicepass.equals ( "" ) ) {
                useairplaypassword = false;
            } else {
                useairplaypassword = true;
            }
            mDeivceMac = getMacAddr();
            txtairplay_len = 0;
            txtairplay_len = putstrtobyte ( txtairplay, "deviceid=", mDeivceMac,
                                            txtairplay_len );
            txtairplay_len = putstrtobyte ( txtairplay, "features=", "0x9DD",
                                            txtairplay_len );
            txtairplay_len = putstrtobyte ( txtairplay, "srcvers=", "150.33",
                                            txtairplay_len );
            if ( usescreencode ) {
                txtairplay_len = putstrtobyte ( txtairplay, "flags=", "0x4c",
                                                txtairplay_len );
            } else {
                if ( useairplaypassword )
                    txtairplay_len = putstrtobyte ( txtairplay, "flags=", "0xc4",
                                                    txtairplay_len );
                else
                    txtairplay_len = putstrtobyte ( txtairplay, "flags=", "0x44",
                                                    txtairplay_len );
            }
            txtairplay_len = putstrtobyte ( txtairplay, "vv=", "1", txtairplay_len );
            txtairplay_len = putstrtobyte ( txtairplay, "model=", "AppleTV3,1",
                                            txtairplay_len );
            if ( usescreencode ) {
                txtairplay_len = putstrtobyte ( txtairplay, "pw=", "false",
                                                txtairplay_len );
            } else {
                if ( useairplaypassword )
                    txtairplay_len = putstrtobyte ( txtairplay, "pw=", "1",
                                                    txtairplay_len );
                else
                    txtairplay_len = putstrtobyte ( txtairplay, "pw=", "false",
                                                    txtairplay_len );
            }
            mAllCast.setAirPlayTXTRecord ( null, 0 );
            txtraop_len = 0;
            txtraop_len = putstrtobyte ( txtraop, "ch=", "2", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "cn=", "0,1,3", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "da=", "true", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "et=", "0,3,5", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "md=", "0,1,2", txtraop_len );
            if ( usescreencode ) {
                txtraop_len = putstrtobyte ( txtraop, "pw=", "false", txtraop_len );
            } else {
                if ( useairplaypassword ) {
                    txtraop_len = putstrtobyte ( txtraop, "pw=", "true", txtraop_len );
                } else {
                    txtraop_len = putstrtobyte ( txtraop, "pw=", "false", txtraop_len );
                }
            }
            txtraop_len = putstrtobyte ( txtraop, "sr=", "44100", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "ss=", "16", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "sv=", "false", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "tp=", "UDP", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "txtvers=", "1", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "am=", "AppleTV3,1", txtraop_len );
            if ( usescreencode ) {
                txtraop_len = putstrtobyte ( txtraop, "sf=", "0x4c", txtraop_len );
            } else {
                if ( useairplaypassword ) {
                    txtraop_len = putstrtobyte ( txtraop, "sf=", "0xc4", txtraop_len );
                } else {
                    txtraop_len = putstrtobyte ( txtraop, "sf=", "0x44", txtraop_len );
                }
            }
            txtraop_len = putstrtobyte ( txtraop, "ft=", "0x9DD", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "vn=", "3", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "vs=", "150.33", txtraop_len );
            txtraop_len = putstrtobyte ( txtraop, "vv=", "1", txtraop_len );
            mAllCast.setRAOPTXTRecord ( null, 0 );
            // mAllCast.setDeviceName("AirPlay");
            mPrefutils = new PrefUtils ( this );
            mServiceName =  mPrefutils.getString ( PrefUtils.SERVICE_NAME, null );
            if ( mServiceName == null ) {
                mServiceName = this.getResources().getString (
                                   R.string.config_default_name );
            }
            mAllCast.setDeviceName (  "Airplay-"
                                      + mServiceName );
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
            Log.d ( "tt", "onDestroy////////////////////////////" + mServiceName + Log.getStackTraceString ( new Throwable() ) );
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
            Log.d ( "tt", "onStartCommand////////////////////////////" + intent.getAction() );
            if ( !isRegistReceiver ) {
                IntentFilter f = new IntentFilter();
                f.addAction ( MediaCenterService.SERVICE_NAME_CHANGE );
                registerReceiver ( mAirplayServiceListener, f );
                isRegistReceiver = true;
            }
            mServiceName =  mPrefutils.getString ( PrefUtils.SERVICE_NAME, null );
            if ( mServiceName == null ) {
                mServiceName = this.getResources().getString (
                                   R.string.config_default_name );
            }
            mAllCast.setDeviceName (  "Airplay-"
                                      + mServiceName );
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
            Log.d ( "tt", "mApp.isDaemonRun()" + mApp.isDaemonRun() + "mAllCast.isAirPlayRuning()" + mAllCast.isAirPlayRuning() );
            if ( !mApp.isDaemonRun() ) {
                mApp.startDaemon ( mAllCast );
                STATEDFLAG = true;
            }
            if ( !mAllCast.isAirPlayRuning() ) {
                isstart = mAllCast.startAirPlayService();
            }
            Log.d ( "tt", "mAllCast.startAirPlayService() return" + isstart + "running?" + mAllCast.isAirPlayRuning() );
            if ( !isstart || !mAllCast.isAirPlayRuning() ) {
                mHandler.sendEmptyMessageDelayed ( 0, 3000 );
            }
            return isstart;
        }

        // put string key pairs to byte[]
        private int putstrtobyte ( byte[] ba, String key, String value, int begin ) {
            ba[begin] = ( byte ) ( key.length() + value.length() );
            int start = begin + 1;
            byte[] bb = EncodingUtils.getAsciiBytes ( key );
            int bb_len = bb.length;
            System.arraycopy ( bb, 0, ba, start, bb_len );
            start = start + bb.length;
            bb = EncodingUtils.getAsciiBytes ( value );
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
                Log.i ( TAG, "setVideoUrl mSessionId:" + mSessionId + "mUrl:" + mUrl
                        + "mStart:" + mStart + "type:" + type + "devicename:"
                        + devicename );
                Log.i ( TAG, mSessionId );
                // Log.i(TAG,mUrl);
                mIsPrepared = false;
                mSessionID = mSessionId;
                mType = type; // AIRPLAY or DLNA
                mDeviceName = devicename; // iphone or ipad 's name
                if ( !mDeviceName.equals ( "" ) ) {
                    Log.i ( TAG, "Media From " + mDeviceName );
                }
                mStartPlayPosition = mStart;
                eventlistener = postevent;
                mUri = mUrl;
                eventlistener.OnPostEvent ( mSessionId, PlaybackState.LOADING, null );
            }
            @Override
            public void play ( String mSessionId ) {
                Log.i ( TAG, "play mSessionId:" + mSessionId + "mIsPrepared?"
                        + mIsPrepared );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return;
                if ( !mIsPrepared ) {
                    startVideoPlayer ( mUri, mStartPlayPosition, mType, mSessionID );
                    try {
                        Thread.sleep ( 1000 );
                    } catch ( InterruptedException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mIsPrepared = true;
                }
                playbackEvent ( "play", null, mSessionId );
                eventlistener.OnPostEvent ( mSessionId, PlaybackState.PLAYING, null );
            }
            @Override
            public void pause ( String mSessionId ) {
                Log.i ( TAG, "pause mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return;
                playbackEvent ( "pause", null, mSessionId );
                eventlistener.OnPostEvent ( mSessionId, PlaybackState.PAUSED, null );
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
                mIsPrepared = false;
            }
            @Override
            public boolean isPlaying ( String mSessionId ) {
                Log.i ( TAG, "isPlaying mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return false;
                // if (!mIsPrepared)
                // return false;
                return mVideoSession.isPlaying;
            }
            @Override
            public int getDuration ( String mSessionId ) {
                Log.i ( TAG, "getDuration mSessionId:" + mSessionId );
                // Log.i(TAG,mSessionId);
                // if (!mSessionID.equals(mSessionId))
                // return 0;
                // if (!mIsPrepared)
                // return 0;
                return mVideoSession.mCurrentDuration;
            }
            @Override
            public int getCurrentPosition ( String mSessionId ) {
                Log.i ( TAG, "getCurrentPosition mSessionId:" + mSessionId );
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
                mAudioManager.setStreamVolume ( AudioManager.STREAM_MUSIC, mVolume,
                                                AudioManager.FLAG_SHOW_UI );
            }
            @Override
            public void setMute ( String mSessionId, boolean mute ) {
                Log.i ( TAG, "setMute mSessionId:" + mSessionId + "mute?" + mute );
                // Log.i(TAG,mSessionId);
                mAudioManager.setStreamMute ( AudioManager.STREAM_MUSIC, mute );
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
            public void onPhotoDisplay ( String mSessionId, String path,
            String mDeviceName ) {
                // reserved
                Log.i ( TAG, "onPhotoDisplay,mSessionId=" + mSessionId + ",path="
                        + path + ",mDeviceName=" + mDeviceName );
                if ( !isphotoopen ) {
                    isphotoopen = true;
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
                isphotoopen = false;
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
            intent.setFlags ( Intent.FLAG_ACTIVITY_NEW_TASK );
            startActivity ( intent );
        }

        //
        private void playbackEvent ( String action, String seekTo, String sessionid ) {
            Log.d ( TAG, "==================startVideoPlayer action:" + action
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

        private String getMacAddr() {
            ConnectivityManager connectivity = ( ConnectivityManager ) getSystemService ( Context.CONNECTIVITY_SERVICE );
            String tempVal = "E2:23:61:D0:ED:9D";
            if ( null == connectivity ) {
                return tempVal;
            } else {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if ( null != info ) {
                    for ( int i = 0; i < info.length; i++ ) {
                        if ( info[i].getState() == NetworkInfo.State.CONNECTED ) {
                            NetworkInfo netWorkInfo = info[i];
                            if ( ConnectivityManager.TYPE_ETHERNET == netWorkInfo
                                    .getType() ) {
                                IBinder b = ServiceManager
                                            .getService ( Context.NETWORKMANAGEMENT_SERVICE );
                                INetworkManagementService networkManagement = INetworkManagementService.Stub
                                        .asInterface ( b );
                                if ( networkManagement != null ) {
                                    InterfaceConfiguration iconfig = null;
                                    try {
                                        String mac = networkManagement
                                                     .getInterfaceConfig ( "eth0" )
                                                     .getHardwareAddress();
                                        Log.d ( TAG, "getMacAddr=====null == mac"
                                                + mac );
                                        return mac;
                                    } catch ( Exception e ) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    return tempVal;
                                }
                            } else if ( ConnectivityManager.TYPE_WIFI == netWorkInfo
                                        .getType() ) {
                                WifiManager wifi = ( WifiManager ) getSystemService ( Context.WIFI_SERVICE );
                                WifiInfo wifiInfo = wifi.getConnectionInfo();
                                return wifiInfo.getMacAddress();
                            }
                        }
                    }
                }
                return tempVal;
            }
        }
}