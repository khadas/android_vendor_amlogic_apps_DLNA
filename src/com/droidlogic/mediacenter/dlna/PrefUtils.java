/**
 * @Package com.droidlogic.mediacenter
 * @Description Copyright (c) Inspur Group Co., Ltd. Unpublished Inspur Group
 *              Co., Ltd. Proprietary & Confidential This source code and the
 *              algorithms implemented therein constitute confidential
 *              information and may comprise trade secrets of Inspur or its
 *              associates, and any use thereof is subject to the terms and
 *              conditions of the Non-Disclosure Agreement pursuant to which
 *              this source code was originally received.
 */
package com.droidlogic.mediacenter.dlna;

import com.droidlogic.mediacenter.airplay.util.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;

/**
 * @ClassName PrefUtils
 * @Description TODO
 * @Date 2013-8-26
 * @Email
 * @Author
 * @Version V1.0
 */
public class PrefUtils
{
        private Context             mContent;
        private SharedPreferences          mPrefs;
        public static final Boolean DEBUG              = false;
        public static final String  TAG                = "DLNA";
        /*
        public static final String  AUTOENABLEWHENBOOT = "boot_cfg_dmp";
        public static final String  AUTOENABLEWHENAPK  = "start_service_dmp";*/
        public static final String FISAT_START = "first_start";
        public static final String SERVICE_NAME = "saved_device_name";
        public PrefUtils ( Context cxt )
        {
            mContent = cxt;
            mPrefs = mContent.getSharedPreferences ( Utils.SHARED_PREFS_NAME, Context.MODE_PRIVATE );
        }
        
        public void setString ( String key, String Str )
        {
            SharedPreferences.Editor mEditor = mPrefs.edit();
            mEditor.putString ( key, Str );
            mEditor.commit();
        }
        
        public void setBoolean ( String key, boolean defVal )
        {
            SharedPreferences.Editor mEditor = mPrefs.edit();
            mEditor.putBoolean ( key, defVal );
            mEditor.commit();
        }
        public boolean getBooleanVal ( String key, boolean defVal )
        {
            return mPrefs.getBoolean ( key, defVal );
        }
        public String getString ( String key, String defVal )
        {
            return mPrefs.getString ( key, defVal );
        }
}
