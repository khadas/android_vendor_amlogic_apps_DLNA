package com.droidlogic.mediacenter;

import com.hpplay.happyplay.AllCast;
import com.hpplay.happyplay.iControl;

import android.app.Application;

public class MediaCenterApplication extends Application {
        public AllCast mCast;
        public static boolean mIsDaemonRun = false;

        @Override
        public void onCreate() {
            // TODO Auto-generated method stub
            super.onCreate();
        }

        @Override
        public void onTerminate() {
            // TODO Auto-generated method stub
            if ( mCast != null && mIsDaemonRun ) {
                stopDaemon ( mCast );
            }
            super.onTerminate();
        }

        public AllCast getCastInstance ( iControl ic ) {
            if ( mCast == null ) {
                mCast = new AllCast ( this, ic );
            }
            return mCast;
        }

        public boolean isDaemonRun() {
            return mIsDaemonRun;
        }
        public void stopDaemon ( AllCast cast ) {
            if ( mCast == null ) {
                mCast = cast;
            }
            mCast.stopDaemonService();
            mIsDaemonRun = false;
        }

        public void startDaemon ( AllCast cast ) {
            if ( mCast == null ) {
                mCast = cast;
            }
            mCast.startDaemonService();
            mIsDaemonRun = true;
        }
}
