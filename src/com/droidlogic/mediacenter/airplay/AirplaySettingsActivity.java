package com.droidlogic.mediacenter.airplay;


import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.droidlogic.mediacenter.airplay.setting.SettingsPreferences;
import android.view.Menu;
import android.view.MenuItem;

public class AirplaySettingsActivity extends PreferenceActivity
{

        private static final String TAG = "AirplaySettingsActivity";
        
        @Override
        public void onCreate ( Bundle icicle )
        {
            super.onCreate ( icicle );
            getFragmentManager().beginTransaction()
            .replace ( android.R.id.content, new SettingsPreferences() )
            .commit();
        }
        
        @Override
        public void onResume()
        {
            super.onResume();
        }
        
        @Override
        public void onPause()
        {
            super.onPause();
        }
        
        @Override
        public boolean onCreateOptionsMenu ( Menu menu )
        {
            return true;
        }
        
        @Override
        public boolean onOptionsItemSelected ( MenuItem item )
        {
            return super.onOptionsItemSelected ( item );
        }
        
}
