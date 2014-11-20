package com.droidlogic.mediacenter.airplay.proxy;

import com.droidlogic.mediacenter.airplay.setting.SettingsPreferences;
import com.droidlogic.mediacenter.airplay.util.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootCompletedReceiver extends BroadcastReceiver
{
        private AirplayProxy mAirplayProxy;
        
        @Override
        public void onReceive ( Context context, Intent intent )
        {
            if ( intent.getAction().equals ( Intent.ACTION_BOOT_COMPLETED ) )
            {
                SharedPreferences prefs = Utils.getSharedPreferences ( context );
                boolean autostart  = prefs.getBoolean ( SettingsPreferences.KEY_BOOT_CFG, false );
                SharedPreferences.Editor editor = prefs.edit();
                
                if ( autostart )
                {
                    editor.putBoolean ( SettingsPreferences.KEY_START_SERVICE, true );
                    editor.commit();
                    mAirplayProxy = AirplayProxy.getInstance ( context );
                    //mAirplayProxy.startBackgroundService();
                    mAirplayProxy.startAirReceiver();
                }
                else
                {
                    editor.putBoolean ( SettingsPreferences.KEY_START_SERVICE, false );
                    editor.commit();
                }
            }
        }
        
}
