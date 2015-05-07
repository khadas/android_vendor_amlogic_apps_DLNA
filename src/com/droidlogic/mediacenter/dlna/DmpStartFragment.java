/**
 * @Package com.droidlogic.mediacenter.dlna
 * @Description Copyright (c) Inspur Group Co., Ltd. Unpublished Inspur Group
 *              Co., Ltd. Proprietary & Confidential This source code and the
 *              algorithms implemented therein constitute confidential
 *              information and may comprise trade secrets of Inspur or its
 *              associates, and any use thereof is subject to the terms and
 *              conditions of the Non-Disclosure Agreement pursuant to which
 *              this source code was originally received.
 */
package com.droidlogic.mediacenter.dlna;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import org.cybergarage.util.Debug;
import com.droidlogic.mediacenter.airplay.setting.SettingsPreferenceFragment;
import com.droidlogic.mediacenter.R;

/**
 * @ClassName DmpStartFragment
 * @Description TODO
 * @Date 2013-9-11
 * @Email
 * @Author
 * @Version V1.0
 */
public class DmpStartFragment extends SettingsPreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
        private static final String TAG               = "SettingsPreferences";
        public static final String  KEY_START_SERVICE = "start_dmp";
        public static final String  KEY_BOOT_CFG      = "boot_dmp";
        private SwitchPreference    mStartServicePref;
        private SwitchPreference    mBootCfgPref;
        /* (non-Javadoc)
         * @see android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange(android.preference.Preference, java.lang.Object)
         */
        @Override
        public boolean onPreferenceChange ( Preference preference, Object newValue ) {
            return true;
        }

        /* (non-Javadoc)
         * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
         */
        @Override
        public void onSharedPreferenceChanged ( SharedPreferences sharedPreferences,
                                                String key ) {
            Debug.d ( "startfragment", "onSharedPreferenceChanged:" + key );
            Intent intent = new Intent ( getActivity(), MediaCenterService.class );
            if ( key.equals ( KEY_START_SERVICE ) ) {
                if ( mStartServicePref.isChecked() ) {
                    getActivity().startService ( intent );
                } else {
                    if ( !mBootCfgPref.isChecked() ) {
                        getActivity().stopService ( intent );
                    }
                }
            } else if ( key.equals ( KEY_BOOT_CFG ) ) {
                if ( mBootCfgPref.isChecked() ) {
                    getActivity().startService ( intent );
                } else {
                    if ( !mStartServicePref.isChecked() ) {
                        getActivity().stopService ( intent );
                    }
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        private void setPreferenceListeners ( OnPreferenceChangeListener listener ) {
            mStartServicePref.setOnPreferenceChangeListener ( listener );
            mBootCfgPref.setOnPreferenceChangeListener ( listener );
        }
        @Override
        public void onCreate ( Bundle icicle ) {
            super.onCreate ( icicle );
            addPreferencesFromResource ( R.xml.settings_dlna );
            mStartServicePref = ( SwitchPreference ) findPreference ( KEY_START_SERVICE );
            mBootCfgPref = ( SwitchPreference ) findPreference ( KEY_BOOT_CFG );
        }
        @Override
        public void onStart() {
            super.onStart();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener ( this );
            setPreferenceListeners ( this );
        }

        @Override
        public void onStop() {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener ( this );
            setPreferenceListeners ( null );
            super.onStop();
        }

}
