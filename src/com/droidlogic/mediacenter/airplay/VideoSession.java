package com.droidlogic.mediacenter.airplay;

import android.net.Uri;

public class VideoSession {
        private static VideoSession sInstance = null;
        public String mCurrentSessionID = "";
        public int mCurrentPosition = 0;
        public int mCurrentDuration = 0;
        public int mStartPosition = 0;
        public Uri mUri = null;
        public boolean isPlaying = false;
        public static VideoSession getInstance() {
            if ( sInstance == null ) {
                sInstance = new VideoSession();
            }
            return sInstance;
        }

}
