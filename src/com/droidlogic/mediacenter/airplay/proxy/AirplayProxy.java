package com.droidlogic.mediacenter.airplay.proxy;

import java.util.ArrayList;

import nz.co.iswe.android.airplay.video.HttpVideoHandler;
import nz.co.iswe.android.airplay.network.http.HttpRequestEvent;
import nz.co.iswe.android.airplay.audio.RaopAudioHandler;

import android.content.Context;
import android.content.Intent;

import com.droidlogic.mediacenter.airplay.service.AirReceiverService;
import com.amlogic.util.Debug;

public class AirplayProxy
{
        private static final String TAG           = "AirplayProxy";
        private static AirplayProxy instance;
        private Context             mContext;
        public static boolean       isPlaying     = false;
        public static boolean       isReadyToPlay = false;
        
        private AirplayProxy ( Context context )
        {
            mContext = context;
        }
        
        public static synchronized AirplayProxy getInstance ( Context context )
        {
            if ( instance == null )
            {
                instance = new AirplayProxy ( context );
            }
            
            return instance;
        }
        
        private ArrayList<String> mDeviceList = new ArrayList<String>();
        private String            mSelectedDevice;
        
        public synchronized ArrayList<String> getDeviceList()
        {
            return mDeviceList;
        }
        
        public String getSelectedDevice()
        {
            return mSelectedDevice;
        }
        
        public void setSelectedDevice ( String selectedDevice )
        {
            mSelectedDevice = selectedDevice;
        }
        
        public synchronized void addDevice ( String d )
        {
            if ( !mDeviceList.contains ( d ) )
            {
                mDeviceList.add ( d );
                AirplayBroadcastFactory.sendDeviceStateBroadcast ( mContext, d );
            }
        }
        
        public synchronized void removeDevice ( String d )
        {
            if ( mDeviceList.contains ( d ) )
            {
                mDeviceList.remove ( d );
                
                if ( mSelectedDevice != null && mSelectedDevice.equals ( d ) )
                {
                    mSelectedDevice = null;
                }
                
                AirplayBroadcastFactory.sendDeviceStateBroadcast ( mContext, d );
            }
        }
        
        public synchronized void clearDeviceList()
        {
            mDeviceList = new ArrayList<String>();
            mSelectedDevice = null;
            AirplayBroadcastFactory.sendDeviceStateBroadcast ( mContext, "" );
        }
        
        public void startBackgroundService()
        {
            Debug.i ( TAG, "===startBackgroundService" );
            mContext.startService ( new Intent (
                                        AirReceiverService.ACTION_START_AIRRSERVICE ) );
        }
        
        public void startAirReceiver()
        {
            mContext.startService ( new Intent (
                                        AirReceiverService.ACTION_START_AIRRECEIVER ) );
        }
        
        public void stopAirReceiver()
        {
            mContext.startService ( new Intent (
                                        AirReceiverService.ACTION_STOP_AIRRECEIVER ) );
        }
        
        public void resetAirReceiver()
        {
            mContext.startService ( new Intent (
                                        AirReceiverService.ACTION_RESET_AIRRECEIVER ) );
        }
        
        public void postMoviePlayerState ( String state )
        {
            Debug.i ( TAG, "postMoviePlayerState--------------" + state );
            
            if ( HttpVideoHandler.VIDEO_PLAY.equals ( state ) )
            { isPlaying = true; }
            else
            { isPlaying = false; }
            
            HttpRequestEvent.postEvent ( state );
            setReadyToPlay ( true );
        }
        
        public void postMusicPlayState ( String cmd )
        {
            RaopAudioHandler.sendAudioCmd ( cmd );
        }
        
        private int mDuration    = 0;
        private int mCurPosition = 0;
        
        public synchronized void updateDuration ( int duration )
        {
            mDuration = duration;
        }
        
        public synchronized void updateCurPosition ( int positon )
        {
            mCurPosition = positon;
        }
        
        public String getCurPosition()
        {
            return String.valueOf ( mCurPosition / 1000 );
        }
        
        public String getDuration()
        {
            return String.valueOf ( mDuration / 1000 );
        }
        
        public double getRate()
        {
            return isPlaying == true ? 1.0 : 0.0;
        }
        
        public void setReadyToPlay ( boolean ready )
        {
            isReadyToPlay = ready;
        }
        
        public boolean getReadyToPlay()
        {
            return isReadyToPlay;
        }
        
        public void onDeviceNameChanged()
        {
            resetAirReceiver();
        }
}
