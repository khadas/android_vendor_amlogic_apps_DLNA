package com.droidlogic.mediacenter.airplay.proxy;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;

public class AirplayBroadcastFactory
{

        private Context mContext;
        private AirplayBroadcastReceiver mReceiver;
        
        public AirplayBroadcastFactory ( Context context )
        {
            mContext = context;
        }
        
        public void registerListener ( IAirplayListener listener )
        {
            if ( mReceiver == null )
            {
                mReceiver = new AirplayBroadcastReceiver();
                mContext.registerReceiver ( mReceiver, new IntentFilter (
                                                ConnectivityManager.CONNECTIVITY_ACTION ) );
                //          mContext.registerReceiver(mReceiver, new IntentFilter(
                //                  ACTION_DEVICE_STATE));
                //
                //          mContext.registerReceiver(mReceiver, new IntentFilter(
                //                  ACTION_PLAY_STATE));
                //
                //          mContext.registerReceiver(mReceiver, new IntentFilter(
                //                  Intent.ACTION_MEDIA_EJECT));
                //          mContext.registerReceiver(mReceiver, new IntentFilter(
                //                  Intent.ACTION_MEDIA_UNMOUNTED));
                //          mContext.registerReceiver(mReceiver, new IntentFilter(
                //                  Intent.ACTION_MEDIA_MOUNTED));
                mReceiver.setListener ( listener );
            }
        }
        
        public void unRegisterListener()
        {
            if ( mReceiver != null )
            {
                mContext.unregisterReceiver ( mReceiver );
                mReceiver = null;
            }
        }
        
        public static final String ACTION_DEVICE_STATE = "com.example.airplay.proxy.ACTION_DEVICE_STATE";
        public static final String DEVICE_STATE_EXTRA = "DEVICE_STATE";
        public static final String ACTION_PLAY_STATE = "com.example.airplay.proxy.ACTION_PLAY_STATE";
        public static final String PLAY_STATE_EXTRA = "PLAY_STATE";
        
        public static void sendDeviceStateBroadcast ( Context context, String state )
        {
            Intent intent = new Intent ( ACTION_DEVICE_STATE );
            intent.putExtra ( DEVICE_STATE_EXTRA, state );
            context.sendBroadcast ( intent );
        }
        
        public static void sendPlayStateBroadcast ( Context context, String state )
        {
            Intent intent = new Intent ( ACTION_PLAY_STATE );
            intent.putExtra ( PLAY_STATE_EXTRA, state );
            context.sendBroadcast ( intent );
        }
        
        public static final String ACTION_PLAYER_CONTROL = "com.example.airplay.proxy.ACTION_PLAYER_CONTROL";
        public static final String PLAY_STATE = "com.example.airplay.proxy.PLAY_STATE";
        public static final String PLAY_STATE_STOP = "STOP";
        public static final String PLAY_STATE_PLAY = "PLAY";
        public static final String PLAY_STATE_PAUSE = "PAUSE";
        
        public static void sendMoviePlayerControlBroadcast ( Context context, String state )
        {
            Intent intent = new Intent ( ACTION_PLAYER_CONTROL );
            intent.putExtra ( PLAY_STATE, state );
            context.sendBroadcast ( intent );
        }
        
        public static final String ACTION_PLAYER_SEEK = "com.example.airplay.proxy.ACTION_PLAYER_SEEK";
        public static final String SEEK_POS = "com.example.airplay.proxy.SEEK_POS";
        public static void sendMoviePlayerSeekBroadcast ( Context context, int pos )
        {
            Intent intent = new Intent ( ACTION_PLAYER_SEEK );
            intent.putExtra ( SEEK_POS, pos );
            context.sendBroadcast ( intent );
        }
        
        public static final String ACTION_PLAYER_NEXT = "com.example.airplay.proxy.ACTION_PLAYER_NEXT";
        public static void sendMoviePlayerNextBroadcast ( Context context, String info, int pos, boolean isPhoto )
        {
            Intent intent = new Intent ( ACTION_PLAYER_NEXT );
            intent.putExtra ( "uri-string", info );
            intent.putExtra ( "start-position", pos );
            intent.putExtra ( "isphoto", isPhoto );
            context.sendBroadcast ( intent );
        }
        
        public static final String ACTION_REFRESH_IMAGE = "com.example.airplay.proxy.ACTION_REFRESH_IMAGE";
        public static final String REFRESH_IMAGE = "REFRESH_IMAGE";
        
        public static void sendRefreshImageBroadcast ( Context context, int id )
        {
            Intent intent = new Intent ( ACTION_REFRESH_IMAGE );
            intent.putExtra ( REFRESH_IMAGE, id );
            context.sendBroadcast ( intent );
        }
        
        public static final String ACTION_EXIT_IMAGE = "com.example.airplay.proxy.ACTION_EXIT_IMAGE";
        public static void sendExitImageBroadcast ( Context context )
        {
            Intent intent = new Intent ( ACTION_EXIT_IMAGE );
            context.sendBroadcast ( intent );
        }
        
        public static final String ACTION_UPDATE_INFO = "com.example.airplay.proxy.ACTION_UPDATE_INFO";
        public static final String ACTION_MUSIC_EXIT = "com.example.airplay.proxy.ACTION_MUSIC_EXIT";
        public static final String ACTION_MUSIC_NEXT = "com.example.airplay.proxy.ACTION_MUSIC_NEXT";
        
        public static void sendUpdateInfoBroadcast ( Context context, String title, String artist )
        {
            Intent intent = new Intent ( ACTION_UPDATE_INFO );
            intent.putExtra ( "title", title );
            intent.putExtra ( "artist", artist );
            context.sendBroadcast ( intent );
        }
        
        public static void sendMusicExitBroadcast ( Context context )
        {
            Intent intent = new Intent ( ACTION_MUSIC_EXIT );
            context.sendBroadcast ( intent );
        }
        
        public static void sendPlayNextBroadcast ( Context context )
        {
            Intent intent = new Intent ( ACTION_MUSIC_NEXT );
            context.sendBroadcast ( intent );
        }
}
