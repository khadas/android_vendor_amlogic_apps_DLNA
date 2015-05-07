/**
 * @Package com.droidlogic.mediacenter
 * @Description
 *
 * Copyright (c) Inspur Group Co., Ltd. Unpublished
 *
 * Inspur Group Co., Ltd.
 * Proprietary & Confidential
 *
 * This source code and the algorithms implemented therein constitute
 * confidential information and may comprise trade secrets of Inspur
 * or its associates, and any use thereof is subject to the terms and
 * conditions of the Non-Disclosure Agreement pursuant to which this
 * source code was originally received.
 */
package com.droidlogic.mediacenter;


import com.droidlogic.mediacenter.airplay.setting.SettingsPreferences;
import com.droidlogic.mediacenter.airplay.util.Utils;
import com.droidlogic.mediacenter.dlna.DmpService;
import com.droidlogic.mediacenter.dlna.DmpStartFragment;
import com.droidlogic.mediacenter.dlna.MediaCenterService;
import com.droidlogic.mediacenter.dlna.PrefUtils;
import com.droidlogic.mediacenter.airplay.AirPlayService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import org.cybergarage.util.Debug;
/**
 * @ClassName DMRBroadcastReceiver
 * @Description TODO
 * @Date 2013-9-4
 * @Email
 * @Author
 * @Version V1.0
 */
public class DMRBroadcastReceiver extends BroadcastReceiver {
        private PrefUtils mPrefUtils;

        private static final String TAG = "DMRBroadcastReceiver";
        /* (non-Javadoc)
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive ( Context cxt, Intent intent ) {
            mPrefUtils = new PrefUtils ( cxt );
            /*restart DMRservice when network config changed*/
            if ( ( ConnectivityManager.CONNECTIVITY_ACTION ).equals ( intent.getAction() ) ) {
                NetworkInfo netInfo = ( NetworkInfo ) intent.getExtra ( WifiManager.EXTRA_NETWORK_INFO, null );
                ConnectivityManager cMgr = ( ConnectivityManager ) cxt.getSystemService ( Context.CONNECTIVITY_SERVICE );
                if ( ( cMgr.getNetworkInfo ( ConnectivityManager.TYPE_WIFI ) != null ) && !cMgr.getNetworkInfo ( ConnectivityManager.TYPE_WIFI ).isConnectedOrConnecting()
                        || ( ( cMgr.getNetworkInfo ( ConnectivityManager.TYPE_ETHERNET ) != null ) ) && !cMgr.getNetworkInfo ( ConnectivityManager.TYPE_ETHERNET ).isConnectedOrConnecting() ) {
                    Intent mNetIntent = new Intent ( DmpService.NETWORK_ERROR );
                    cxt.sendBroadcast ( mNetIntent );
                    //cxt.stopService(new Intent(cxt,MediaCenterService.class));
                    Debug.d ( TAG , ">>>>>onReceive :network disconnected" );
                    if ( AirPlayService.STATEDFLAG &&  ( mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_BOOT_CFG, false ) || mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_START_SERVICE, false ) ) ) {
                        cxt.stopService ( new Intent ( cxt, AirPlayService.class ) );
                    }
                } if ( ( netInfo != null ) && ( netInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED ) ) {
                    cxt.stopService ( new Intent ( cxt, MediaCenterService.class ) );
                    if ( mPrefUtils.getBooleanVal ( DmpStartFragment.KEY_BOOT_CFG, false ) || mPrefUtils.getBooleanVal ( DmpStartFragment.KEY_START_SERVICE, false ) ) {
                        cxt.startService ( new Intent ( cxt, MediaCenterService.class ) );
                    }
                    if ( mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_BOOT_CFG, false ) || mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_START_SERVICE, false ) ) {
                        cxt.startService ( new Intent ( cxt, AirPlayService.class ) );
                    }
                    Debug.d ( TAG , ">>>>>onReceive :network connected" );
                }
            }
            if ( ( WifiManager.WIFI_AP_STATE_CHANGED_ACTION ).equals ( intent.getAction() ) ) {
                int wifi_AP_State =  intent.getIntExtra ( WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED );
                if ( WifiManager.WIFI_STATE_ENABLED == wifi_AP_State ) {
                    cxt.stopService ( new Intent ( cxt, MediaCenterService.class ) );
                    if ( mPrefUtils.getBooleanVal ( DmpStartFragment.KEY_BOOT_CFG, false ) || mPrefUtils.getBooleanVal ( DmpStartFragment.KEY_START_SERVICE, false ) ) {
                        cxt.startService ( new Intent ( cxt, MediaCenterService.class ) );
                    }
                    if ( AirPlayService.STATEDFLAG &&  mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_BOOT_CFG, false ) || mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_START_SERVICE, false ) ) {
                        cxt.startService ( new Intent ( cxt, AirPlayService.class ) );
                    }
                } else {
                    Intent mNetIntent = new Intent ( DmpService.NETWORK_ERROR );
                    cxt.sendBroadcast ( mNetIntent );
                    cxt.stopService ( new Intent ( cxt, AirPlayService.class ) );
                }
            }
            /*this broadcast is useless than network change*/
            if ( intent.getAction().equals ( Intent.ACTION_BOOT_COMPLETED ) ) {
                //SharedPreferences prefs = Utils.getSharedPreferences(cxt);
                //boolean autostart  = prefs.getBoolean(SettingsPreferences.KEY_BOOT_CFG, false);
                //SharedPreferences.Editor editor = prefs.edit();
                //boolean autostart = mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_BOOT_CFG, false );
                //if ( autostart ) {
                //cxt.startService(new Intent(cxt,AirPlayService.class));
                //}
            }
        }
}
