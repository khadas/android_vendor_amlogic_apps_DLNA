package com.droidlogic.mediacenter.airplay.proxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;

public class AirplayBroadcastReceiver extends BroadcastReceiver
{
        private static final boolean DEBUG = true;
        private static final String TAG = "AirplayBroadcastReceiver";
        
        private IAirplayListener mListener;
        
        public void setListener ( IAirplayListener listener )
        {
            mListener = listener;
        }
        
        @Override
        public void onReceive ( Context context, Intent intent )
        {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            
            if ( action == null || mListener == null )
            { return; }
            
            if ( ConnectivityManager.CONNECTIVITY_ACTION.equals ( action ) )
            {
                Bundle extras = intent.getExtras();
                
                if ( extras != null )
                {
                    NetworkInfo networkInfo =
                        ( NetworkInfo ) extras.get ( ConnectivityManager.EXTRA_NETWORK_INFO );
                        
                    if ( networkInfo == null ) { return; }
                    
                    State state = networkInfo.getState();
                    int networkType = networkInfo.getType();
                    
                    if ( networkType == ConnectivityManager.TYPE_WIFI ||
                            networkType == ConnectivityManager.TYPE_ETHERNET )
                    {
                        if ( state == State.CONNECTED )
                        {
                            mListener.onNetworkStateChange ( true );
                        }
                        else if ( state == State.DISCONNECTED )
                        {
                            mListener.onNetworkStateChange ( false );
                        }
                    }
                }
            }
            else if ( Intent.ACTION_MEDIA_EJECT.equals ( action )
                      || Intent.ACTION_MEDIA_UNMOUNTED.equals ( action )
                      || Intent.ACTION_MEDIA_MOUNTED.equals ( action ) )
            {
                mListener.onStorageStateChange();
            }
            else if ( AirplayBroadcastFactory.ACTION_DEVICE_STATE.equals ( action ) )
            {
                mListener.onAirplayDeviceStateChange();
            }
            else if ( AirplayBroadcastFactory.ACTION_PLAY_STATE.equals ( action ) )
            {
                mListener.onAirplayPlayStateChange();
            }
        }
        
}
