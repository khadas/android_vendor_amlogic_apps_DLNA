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
import android.os.Handler;
import android.os.Message;
import org.cybergarage.util.Debug;
import android.util.Log;


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
        public static final int WIFI_AP_STATE_ENABLED = 13;
        public static final int WIFI_AP_STATE_FAILED = 14;
        public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
        public static final String WIFI_STAT = "wifi_state";
        private static final String TAG = "DMRBroadcastReceiver";
        private static final int STOPSERVICE = 0;
        private static final int STARTSERVICE = 1;
        Context mContext;
        /* (non-Javadoc)
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive ( Context cxt, Intent intent ) {
            mContext = cxt;
            mPrefUtils = new PrefUtils ( cxt );
            /*restart DMRservice when network config changed*/
            Log.d ( TAG ,">>>>>onReceive : onReceive" + intent.getAction());
            if ( ( ConnectivityManager.CONNECTIVITY_ACTION ).equals ( intent.getAction() ) ) {
                ConnectivityManager cMgr = ( ConnectivityManager ) cxt.getSystemService ( Context.CONNECTIVITY_SERVICE );
                NetworkInfo netInfo = ( NetworkInfo ) intent.getExtras ().getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
                NetworkInfo netInfoWIFI = cMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo netInfoETH = cMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

                if (netInfo != null) {
                    if (netInfo.getState() == NetworkInfo.State.CONNECTED) {
                        Log.d ( TAG ,"TypeName " + netInfo.getTypeName()+ " connected");
                        mHandler.sendEmptyMessage (STOPSERVICE);
                        mHandler.sendEmptyMessageDelayed ( STARTSERVICE, 3000 );
                    } else if (netInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                        Log.d ( TAG ,"TypeName " + netInfo.getTypeName()+ " disconnected");
                        mHandler.sendEmptyMessage (STOPSERVICE);
                    }

                } else {
                    if (netInfoWIFI != null && netInfoETH != null)
                        Log.d ( TAG ," No ActiveNetwork !"+ netInfoWIFI.getState() +"|" +netInfoETH.getState());
                }
            }
            if ( ( WIFI_AP_STATE_CHANGED_ACTION ).equals ( intent.getAction() ) ) {

                int wifi_AP_State =  intent.getIntExtra ( "wifi_state", 14 );
                if ( WIFI_AP_STATE_ENABLED == wifi_AP_State ) {
                    cxt.stopService ( new Intent ( cxt, MediaCenterService.class ) );
                    mHandler.sendEmptyMessage (STOPSERVICE);
                    mHandler.sendEmptyMessageDelayed ( STARTSERVICE, 3000 );
                } else {
                    mHandler.sendEmptyMessage (STOPSERVICE);
                }
            }
            //this broadcast is useless than network change
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
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage ( Message msg ) {
                switch ( msg.what ) {
                    case STOPSERVICE:
                        Intent mNetIntent = new Intent(DmpService.NETWORK_ERROR);
                        mContext.sendBroadcast ( mNetIntent );
                        if ( AirPlayService.STATEDFLAG && mPrefUtils.getBooleanVal ( SettingsPreferences.KEY_BOOT_CFG, false ) ) {
                            Log.d ( TAG , " stopService AirPlayService");
                            mContext.stopService(new Intent(mContext,AirPlayService.class));
                        }
                        break;
                    case STARTSERVICE:
                        if (mPrefUtils.getBooleanVal(DmpStartFragment.KEY_BOOT_CFG,false)) {
                            mContext.startService (new Intent(mContext, MediaCenterService.class));
                            Log.d ( TAG , " startService MediaCenterService");
                        }
                        if (mPrefUtils.getBooleanVal(SettingsPreferences.KEY_BOOT_CFG,false)) {
                            mContext.startService(new Intent(mContext,AirPlayService.class));
                            Log.d ( TAG , " startService AirPlayService");
                        }
                        break;
                }
                // TODO Auto-generated method stub
                super.handleMessage ( msg );
            }
        };
}
