package com.amlogic.mediacenter.airplay;

import android.app.Application;

import com.amlogic.mediacenter.airplay.proxy.AirplayController;
import com.amlogic.mediacenter.airplay.proxy.AirplayProxy;

public class AirplayApplication extends Application {
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		AirplayProxy.getInstance(this);
		AirplayController.getInstance(this);
	}
}
