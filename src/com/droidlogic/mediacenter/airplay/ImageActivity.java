package com.droidlogic.mediacenter.airplay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;

import com.droidlogic.mediacenter.airplay.image.ContentFragment;
import com.droidlogic.mediacenter.airplay.proxy.AirplayBroadcastFactory;

import nz.co.iswe.android.airplay.network.http.HttpRequestEvent;
import com.droidlogic.mediacenter.R;

public class ImageActivity extends Activity {
        private Context             mContext;
        public static boolean       isRunning = false;
        private int                 mImageId  = -1;
        private UpdateImageReceiver mUpdateImageReceiver;
        private PowerManager.WakeLock mWakeLock;
        @Override
        public void onCreate ( Bundle savedInstanceState ) {
            super.onCreate ( savedInstanceState );
            mContext = ImageActivity.this;
            Bundle extras = getIntent().getExtras();
            if ( extras != null ) {
                // The activity theme is the only state data that the activity needs
                // to restore. All info about the content displayed is managed by
                // the fragment
                mImageId = extras.getInt ( "image_id" );
            } else if ( savedInstanceState != null ) {
                // If there's no restore state, get the theme from the intent
                mImageId = savedInstanceState.getInt ( "image_id" );
            }
            setContentView ( R.layout.content_activity );
            if ( extras != null ) {
                ContentFragment frag = ( ContentFragment ) getFragmentManager()
                                       .findFragmentById ( R.id.content_frag );
                frag.updateContentAndRecycleBitmap ( mImageId );
            }
            mUpdateImageReceiver = new UpdateImageReceiver();
        }

        @Override
        protected void onResume() {
            super.onResume();
            isRunning = true;
            mUpdateImageReceiver.register();
            /* enable backlight */
            PowerManager pm = ( PowerManager ) getSystemService ( Context.POWER_SERVICE );
            mWakeLock = pm.newWakeLock ( PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "ImageActivity" );
            mWakeLock.acquire();
        }

        @Override
        protected void onPause() {
            super.onPause();
            isRunning = false;
            mUpdateImageReceiver.unregister();
            mWakeLock.release();
        }

        @Override
        protected void onDestroy() {
            HttpRequestEvent.postSlideshowEvent ( "stopped" );
            super.onDestroy();
        }

        @Override
        protected void onSaveInstanceState ( Bundle outState ) {
            super.onSaveInstanceState ( outState );
            outState.putInt ( "image_id", mImageId );
        }

        private class UpdateImageReceiver extends BroadcastReceiver {
                public void register() {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction ( AirplayBroadcastFactory.ACTION_REFRESH_IMAGE );
                    filter.addAction ( AirplayBroadcastFactory.ACTION_EXIT_IMAGE );
                    mContext.registerReceiver ( this, filter );
                }

                public void unregister() {
                    mContext.unregisterReceiver ( this );
                }

                @Override
                public void onReceive ( Context context, Intent intent ) {
                    if ( AirplayBroadcastFactory.ACTION_REFRESH_IMAGE.equals ( intent
                            .getAction() ) ) {
                        int imgId = intent.getIntExtra (
                                        AirplayBroadcastFactory.REFRESH_IMAGE, mImageId );
                        mImageId = imgId;
                        ContentFragment frag = ( ContentFragment ) getFragmentManager()
                                               .findFragmentById ( R.id.content_frag );
                        frag.updateContentAndRecycleBitmap ( mImageId );
                    } else if ( AirplayBroadcastFactory.ACTION_EXIT_IMAGE.equals ( intent
                                .getAction() ) ) {
                        finish();
                    }
                }
        }
}
