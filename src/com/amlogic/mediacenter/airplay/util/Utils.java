package com.amlogic.mediacenter.airplay.util;
import com.amlogic.mediacenter.R;
import com.amlogic.mediacenter.SettingsFragment;
import com.amlogic.mediacenter.airplay.setting.SettingsPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

public class Utils {

	// SDK Version
	public static int getSDKVersion() {
		// 14 : 4.0,4.0.1,4.0.2
		// 15 : 4.0.3,4.0.4
		// 16 : 4.1,4.1.1
		// 17 : 4.2,4.2.2
		return Build.VERSION.SDK_INT;
	}

	// Network
	public static boolean isNetworkConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info != null) {
			int type = info.getType();
			if (type == ConnectivityManager.TYPE_WIFI || 
				type == ConnectivityManager.TYPE_ETHERNET) {
				return info.isAvailable();
			}			
		} 
		
		return false;
	}

	public static final String SHARED_PREFS_NAME = "com.amlogic.mediacenter_preferences";

	public static SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences(SHARED_PREFS_NAME,
				Context.MODE_PRIVATE);
	}
	
	public static String getSavedDeviceName(Context context) {
		SharedPreferences prefs = Utils.getSharedPreferences(context);
		return "AirPlay-"+prefs.getString(SettingsFragment.KEY_DEVICE_NAME, 
				context.getString(R.string.config_default_name));
	}
}
