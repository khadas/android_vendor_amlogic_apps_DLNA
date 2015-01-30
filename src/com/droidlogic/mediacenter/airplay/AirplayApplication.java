package com.droidlogic.mediacenter.airplay;

import android.app.Application;

import com.droidlogic.mediacenter.airplay.proxy.AirplayController;
import com.droidlogic.mediacenter.airplay.proxy.AirplayProxy;

public class AirplayApplication extends Application {
        @Override
        public void onCreate() {
            // TODO Auto-generated method stub
            super.onCreate();
            AirplayProxy.getInstance ( this );
            AirplayController.getInstance ( this );
        }
}
