package com.droidlogic.mediacenter;

import java.util.List;
import java.util.Map;

import org.cybergarage.util.Debug;

import com.droidlogic.mediacenter.airplay.proxy.AirplayProxy;
import com.droidlogic.mediacenter.airplay.service.AirReceiverService;
import com.droidlogic.mediacenter.airplay.setting.SettingsPreferences;
import com.droidlogic.mediacenter.dlna.DMRError;
import com.droidlogic.mediacenter.dlna.DmpFragment;
import com.droidlogic.mediacenter.dlna.DmpService;
import com.droidlogic.mediacenter.dlna.DmpStartFragment;
import com.droidlogic.mediacenter.dlna.MediaCenterService;
import com.droidlogic.mediacenter.dlna.PrefUtils;
import com.droidlogic.mediacenter.dlna.DmpFragment.FreshListener;
import com.droidlogic.mediacenter.dlna.DmpService.DmpBinder;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class MediaCenterActivity extends Activity  implements FreshListener {
        private static final String TAG = "DLNA";
        private PrefUtils mPrefUtils;
        private Animation animation;
        //private DmpBinder mBinder;
        private DmpService mService;
        private boolean mStartDmp;
        private ServiceConnection mConn = null;
        private TextView mDeviceName = null;
        private Fragment mCallbacks;
        private AirplayProxy mAirplayProxy;
        @Override
        protected void onCreate ( Bundle savedInstanceState ) {
            super.onCreate ( savedInstanceState );
            mAirplayProxy = AirplayProxy.getInstance ( this );
            setContentView ( R.layout.activity_main );
            mPrefUtils = new PrefUtils ( this );
            animation = ( AnimationSet ) AnimationUtils.loadAnimation ( this, R.anim.refresh_btn );
            mDeviceName = ( TextView ) findViewById ( R.id.device_name );
            checkNet();
            LogStart();
        }

        private void startDmpService() {
            mConn = new ServiceConnection() {
                @Override
                public void onServiceConnected ( ComponentName name, IBinder service ) {
                    DmpBinder mBinder = ( DmpBinder ) service;
                    mService = mBinder.getServiceInstance();
                }
                @Override
                public void onServiceDisconnected ( ComponentName name ) {
                    mService.forceStop();
                }
            };
            getApplicationContext().bindService ( new Intent ( this, DmpService.class ), mConn, Context.BIND_AUTO_CREATE );
            mStartDmp = true;
        }

        private void startAirplay() {
            if ( mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_START_SERVICE, false ) || mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_BOOT_CFG, false ) ) {
                Log.d ( TAG, "onStartAirProxy" );
                mAirplayProxy.startAirReceiver();
            }
        }

        private void stopAirplay() {
            if ( mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_START_SERVICE, false ) && !mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_BOOT_CFG, false ) ) {
                Log.d ( TAG, "onStartAirProxy" );
                mAirplayProxy.stopAirReceiver();
                stopService ( new Intent ( this, AirReceiverService.class ) );
            }
        }

        /**
         * @Description TODO
         * @return
         */
        public PrefUtils getPref() {
            return mPrefUtils;
        }


        @Override
        protected void onDestroy() {
            stopMediaCenterService();
            stopDmpService();
            stopAirplay();
            super.onDestroy();
        }

        private void stopDmpService() {
            if ( mStartDmp && mConn != null ) {
                mStartDmp = false;
                getApplicationContext().unbindService ( mConn );
            }
        }

        @Override
        protected void onResume() {
            super.onResume();
            /*mRefreshView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animation.startNow();
                }
            });*/
            showDeviceName();
        }

        public void showDeviceName() {
            String serviceName = mPrefUtils.getString ( SettingsFragment.KEY_DEVICE_NAME, getString ( R.string.config_default_name ) );
            mDeviceName.setText ( serviceName );
        }

        public void startMediaCenterService() {
            boolean startApk = mPrefUtils.getBooleanVal ( DmpStartFragment.KEY_START_SERVICE, false );
            boolean startReboot = mPrefUtils.getBooleanVal ( DmpStartFragment.KEY_BOOT_CFG, false );
            if ( startApk || startReboot ) {
                Intent intent = new Intent ( this, MediaCenterService.class );
                startService ( intent );
            }
        }

        private void stopMediaCenterService() {
            boolean startApk = mPrefUtils.getBooleanVal ( DmpStartFragment.KEY_START_SERVICE, false );
            boolean startReboot = mPrefUtils.getBooleanVal ( DmpStartFragment.KEY_BOOT_CFG, false );
            if ( !startReboot ) {
                Intent intent = new Intent ( this, MediaCenterService.class );
                stopService ( intent );
            }
        }

        /* (non-Javadoc)
         * @see com.droidlogic.mediacenter.DmpFragment.FreshListener#startSearch()
         */
        @Override
        public void startSearch() {
            if ( mService != null )
            { mService.startSearch(); }
        }

        /* (non-Javadoc)
         * @see com.droidlogic.mediacenter.DmpFragment.FreshListener#getFullList()
         */
        @Override
        public List<String> getDevList() {
            if ( mService == null )
            { return null; }
            return mService.getDevList();
        }

        /* (non-Javadoc)
         * @see com.droidlogic.mediacenter.FreshListener#getDevIcon(java.lang.String)
         */
        @Override
        public String getDevIcon ( String path ) {
            if ( mService == null )
            { return null; }
            return mService.getDeviceIcon ( path );
        }

        public List<Map<String, Object>> getBrowseResult ( String didl_str, List<Map<String, Object>> list, int itemTypeDir, int itemImgUnsel ) {
            if ( mService == null )
            { return null; }
            return mService.getBrowseResult ( didl_str, list, itemTypeDir, itemImgUnsel );
        }

        public String actionBrowse ( String mediaServerName, String item_id,
        String flag ) {
            if ( mService == null )
            { return null; }
            return mService.actionBrowse ( mediaServerName, item_id, flag );
        }

        @Override
        public boolean onKeyDown ( int keyCode, KeyEvent event ) {
            if ( keyCode == KeyEvent.KEYCODE_BACK ) {
                mCallbacks = getFragmentManager().findFragmentById ( R.id.frag_detail );
                if ( mCallbacks instanceof Callbacks ) {
                    ( ( Callbacks ) mCallbacks ).onBackPressedCallback();
                } else {
                    stopMediaCenterService();
                    stopDmpService();
                    stopAirplay();
                    MediaCenterActivity.this.finish();
                }
                return true;
            }
            return super.onKeyDown ( keyCode, event );
        }

        private void checkNet() {
            ConnectivityManager mConnectivityManager = ( ConnectivityManager ) this.getSystemService ( Context.CONNECTIVITY_SERVICE );
            NetworkInfo wifiInfo = mConnectivityManager.getNetworkInfo ( ConnectivityManager.TYPE_WIFI );
            NetworkInfo ethInfo = mConnectivityManager.getNetworkInfo ( ConnectivityManager.TYPE_ETHERNET );
            NetworkInfo mobileInfo = mConnectivityManager.getNetworkInfo ( ConnectivityManager.TYPE_MOBILE );
            if ( ethInfo == null && ethInfo == null && mobileInfo == null ) {
                    Intent mIntent = new Intent();
                    mIntent.addFlags ( Intent.FLAG_ACTIVITY_NEW_TASK );
                    mIntent.setClass ( this, DMRError.class );
                    startActivity ( mIntent );
                return;
            }
            if ( ( ethInfo != null && ethInfo.isConnectedOrConnecting() ) ||
                    ( wifiInfo != null && wifiInfo.isConnectedOrConnecting() ) ||
                    ( mobileInfo != null && mobileInfo.isConnectedOrConnecting() ) ) {
                    startMediaCenterService();
                    startDmpService();
                    startAirplay();
                }else {
                    Intent mIntent = new Intent();
                    mIntent.addFlags ( Intent.FLAG_ACTIVITY_NEW_TASK );
                    mIntent.setClass ( this, DMRError.class );
                    startActivity ( mIntent );
                }
            }

        public interface Callbacks {
            public void onBackPressedCallback();
        }

        public void LogStart() {
            if ( !SystemProperties.getBoolean ( "rw.app.dlna.debug", false ) ) {
                org.cybergarage.util.Debug.off(); //LOG OFF
            } else {
                org.cybergarage.util.Debug.on();  //LOG ON
            }
            if ( !SystemProperties.getBoolean ( "rw.app.airplay.debug", false ) ) {
                com.amlogic.util.Debug.Off(); // LOG OFF
            } else {
                com.amlogic.util.Debug.On(); // LOG ON
            }
        }
}
