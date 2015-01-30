package com.droidlogic.mediacenter.airplay.proxy;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.droidlogic.mediacenter.airplay.AirplaySettingsActivity;
import com.droidlogic.mediacenter.airplay.ImageActivity;
import com.droidlogic.mediacenter.airplay.MovieActivity;
import com.droidlogic.mediacenter.airplay.MusicActivity;
import com.droidlogic.mediacenter.airplay.util.AirplayMutex;
import com.droidlogic.mediacenter.R;
import com.amlogic.util.Debug;

public class AirplayController {
        private static final String        TAG                   = "AirplayController";
        // Use a layout id for a unique identifier
        private static final int           AIRPLAY_NOTIFICATIONS = R.layout.activity_main;
        private final Context              mContext;
        private static AirplayController   instance;
        private NotificationCompat.Builder mBuilder;
        private NotificationManager        mNotifyManager;
        public static AirplayMutex mSync = new AirplayMutex();
        public interface EventType {
            final long CREATE_EVENT          = 1L;
            final long LAUNCH_SETTINGS       = 1L << 1;
            final long LAUNCH_VIDEO_PLAYER   = 1L << 2;
            final long LAUNCH_IMAGE_PLAYER   = 1L << 3;
            final long EXIT_IMAGE_PLAYER     = 1L << 13;
            final long LAUNCH_AUDIO_PLAYER   = 1L << 4;
            final long EXIT_VIDEO_PLAYER     = 1L << 5;
            final long EXIT_AUDIO_PLAYER     = 1L << 6;
            final long UPDATE_AUDIO_PROGRESS = 1L << 7;
            final long UPDATE_AUDIO_INFO     = 1L << 8;
            final long VIDEO_PLAYER_PLAY     = 1L << 9;
            final long VIDEO_PLAYER_PAUSE    = 1L << 10;
            final long VIDEO_PLAYER_SEEK     = 1L << 11;
            final long VIDEO_PLAYER_NEXT     = 1L << 12;
        }

        public static class EventInfo {
                public long    eventType;
                public String  eventInfo;
                public int     intInfo;
                public float   audioProgressCur;
                public float   audioProgressMax;
                public String  audioArtist;
                public String  audioTitle;
                public boolean isPhoto;

                public EventInfo ( long type, String info, int pos, boolean photo ) {
                    eventType = type;
                    eventInfo = info;
                    intInfo = pos;
                    isPhoto = photo;
                }

                public EventInfo ( long type, int id ) {
                    eventType = type;
                    intInfo = id;
                }

                public EventInfo ( long type ) {
                    eventType = type;
                }

                public EventInfo ( long type, float max, float cur ) {
                    eventType = type;
                    audioProgressCur = cur;
                    audioProgressMax = max;
                }

                public EventInfo ( long type, String artist, String title ) {
                    eventType = type;
                    audioArtist = artist;
                    audioTitle = title;
                }
        }

        public static AirplayController getInstance ( Context context ) {
            if ( instance == null ) {
                instance = new AirplayController ( context );
            }
            return instance;
        }

        private AirplayController ( Context context ) {
            mContext = context;
        }

        public void sendEvent ( Object sender, final long event ) {
            this.sendEvent ( sender, new EventInfo ( event ) );
        }

        public void sendEvent ( Object sender, final EventInfo event ) {
            // Launch Settings
            Debug.i ( TAG, "sendEvent, " + event.eventType );
            if ( event.eventType == EventType.LAUNCH_SETTINGS ) {
                launchSettings();
                return;
            }
            // Launch VideoPlayer
            if ( event.eventType == EventType.LAUNCH_VIDEO_PLAYER ) {
                Debug.i ( TAG, "====launch video wait lock..." );
                mSync.lock();
                Debug.i ( TAG, "====launch video wait lock ok" );
                launchVideoPlayer ( event.eventInfo, event.intInfo, event.isPhoto );
                return;
            }
            if ( event.eventType == EventType.EXIT_VIDEO_PLAYER ) {
                Debug.i ( TAG, "====exit video wait lock..." );
                mSync.lock();
                Debug.i ( TAG, "====exit video wait lock" );
                exitVideoPlayer();
                return;
            }
            if ( event.eventType == EventType.VIDEO_PLAYER_PLAY ) {
                onVideoPlayerPlayPause ( true );
                return;
            }
            if ( event.eventType == EventType.VIDEO_PLAYER_PAUSE ) {
                onVideoPlayerPlayPause ( false );
                return;
            }
            if ( event.eventType == EventType.VIDEO_PLAYER_SEEK ) {
                onVideoPlayerSeek ( event.intInfo );
                return;
            }
            // Launch ImagePlayer
            if ( event.eventType == EventType.LAUNCH_IMAGE_PLAYER ) {
                launchImagePlayer ( event.intInfo );
                stopDreaming();
                return;
            }
            // Launch ImagePlayer
            if ( event.eventType == EventType.EXIT_IMAGE_PLAYER ) {
                exitImagePlayer();
                stopDreaming();
                return;
            }
            // Launch AudioPlayer
            if ( event.eventType == EventType.LAUNCH_AUDIO_PLAYER ) {
                launchAudioPlayer();
                stopDreaming();
                return;
            }
            if ( event.eventType == EventType.EXIT_AUDIO_PLAYER ) {
                exitAudioPlayer();
                return;
            }
            if ( event.eventType == EventType.UPDATE_AUDIO_PROGRESS ) {
                updateAudioProgress ( event.audioProgressMax, event.audioProgressCur );
                return;
            }
            if ( event.eventType == EventType.UPDATE_AUDIO_INFO ) {
                updateAudioInfo ( event.audioArtist, event.audioTitle );
                return;
            }
        }

        private void launchVideoPlayer ( String info, int pos, boolean isPhoto ) {
            if ( !MovieActivity.isRunning ) {
                // Intent intent = new Intent(Intent.ACTION_VIEW);
                // intent.setDataAndType(Uri.parse(info), "video/*");
                Debug.i ( TAG, "==>launchVideoPlayer" );
                Intent intent = new Intent ( Intent.ACTION_VIEW );
                intent.setClass ( mContext, MovieActivity.class );
                intent.setFlags ( Intent.FLAG_ACTIVITY_NEW_TASK
                                  | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                  | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                intent.setData ( Uri.parse ( info ) );
                intent.putExtra ( "start-position", pos );
                intent.putExtra ( "isphoto", isPhoto );
                mContext.startActivity ( intent );
            } else {
                Debug.i ( TAG, "==>VideoPlayer play next" );
                AirplayBroadcastFactory.sendMoviePlayerNextBroadcast ( mContext,
                        info, pos, isPhoto );
                mSync.unlock();
            }
        }

        private void exitVideoPlayer() {
            if ( MovieActivity.isRunning == false ) {
                Debug.i ( TAG, "==>exitVideoPlayer, unlock" );
                mSync.unlock();
            }
            AirplayBroadcastFactory.sendMoviePlayerControlBroadcast ( mContext,
                    AirplayBroadcastFactory.PLAY_STATE_STOP );
        }

        private void stopDreaming() {
            IDreamManager mDreamManager = IDreamManager.Stub.asInterface (
                                              ServiceManager.getService ( DreamService.DREAM_SERVICE ) );
            if ( mDreamManager != null ) {
                try {
                    if ( mDreamManager.isDreaming() ) {
                        mDreamManager.awaken();
                    }
                } catch ( RemoteException e ) {
                    // we tried
                }
            }
        }
        private void onVideoPlayerPlayPause ( boolean play ) {
            if ( play )
                AirplayBroadcastFactory.sendMoviePlayerControlBroadcast ( mContext,
                        AirplayBroadcastFactory.PLAY_STATE_PLAY );
            else
                AirplayBroadcastFactory.sendMoviePlayerControlBroadcast ( mContext,
                        AirplayBroadcastFactory.PLAY_STATE_PAUSE );
        }

        private void onVideoPlayerSeek ( int pos ) {
            AirplayBroadcastFactory.sendMoviePlayerSeekBroadcast ( mContext, pos );
        }

        private void launchImagePlayer ( int id ) {
            if ( !ImageActivity.isRunning ) {
                Intent intent = new Intent ( Intent.ACTION_VIEW );
                intent.setClass ( mContext, ImageActivity.class );
                intent.setFlags ( Intent.FLAG_ACTIVITY_NEW_TASK
                                  | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                  | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                intent.putExtra ( "image_id", id );
                mContext.startActivity ( intent );
            } else {
                AirplayBroadcastFactory.sendRefreshImageBroadcast ( mContext, id );
            }
        }

        private void exitImagePlayer() {
            AirplayBroadcastFactory.sendExitImageBroadcast ( mContext );
        }

        private void launchAudioPlayer() {
            NotificationManager nM = ( NotificationManager ) mContext
                                     .getSystemService ( Context.NOTIFICATION_SERVICE );
            mBuilder = new NotificationCompat.Builder ( mContext );
            mBuilder.setContentTitle ( mContext.getText ( R.string.app_name ) )
            .setContentText ( mContext.getText ( R.string.audio_play ) )
            .setSmallIcon ( R.drawable.ic_launcher );
            nM.notify ( AIRPLAY_NOTIFICATIONS, mBuilder.build() );
            if ( !MusicActivity.isRunning() ) {
                Intent intent = new Intent();
                intent.setClass ( mContext, MusicActivity.class );
                intent.setFlags ( Intent.FLAG_ACTIVITY_NEW_TASK
                                  | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                  | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                mContext.startActivity ( intent );
            } else {
                AirplayBroadcastFactory.sendPlayNextBroadcast ( mContext );
            }
        }

        private void exitAudioPlayer() {
            NotificationManager nM = ( NotificationManager ) mContext
                                     .getSystemService ( Context.NOTIFICATION_SERVICE );
            nM.cancel ( AIRPLAY_NOTIFICATIONS );
            mBuilder = null;
            AirplayBroadcastFactory.sendMusicExitBroadcast ( mContext );
        }

        private void updateAudioProgress ( float max, float cur ) {
            final int MAX = ( int ) max;
            final int CUR = ( int ) cur;
            final NotificationManager nM = ( NotificationManager ) mContext
                                           .getSystemService ( Context.NOTIFICATION_SERVICE );
            if ( mBuilder != null ) {
                new Thread ( new Runnable() {
                    @Override
                    public void run() {
                        int incr;
                        for ( incr = CUR; incr <= MAX; incr += 1 ) {
                            if ( mBuilder == null )
                            { break; }
                            mBuilder.setProgress ( MAX, incr, false );
                            nM.notify ( AIRPLAY_NOTIFICATIONS, mBuilder.build() );
                            try {
                                Thread.sleep ( 1 * 1000 );
                            } catch ( InterruptedException e ) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        // exitAudioPlayer();
                    }
                } ).start();
            }
        }

        private void updateAudioInfo ( String artist, String title ) {
            NotificationManager nM = ( NotificationManager ) mContext
                                     .getSystemService ( Context.NOTIFICATION_SERVICE );
            if ( mBuilder != null ) {
                mBuilder.setContentText ( title + " - " + artist );
                nM.notify ( AIRPLAY_NOTIFICATIONS, mBuilder.build() );
            }
            AirplayBroadcastFactory.sendUpdateInfoBroadcast ( mContext, title, artist );
        }

        private void launchSettings() {
            Intent intent = new Intent ( Intent.ACTION_VIEW );
            intent.setClass ( mContext, AirplaySettingsActivity.class );
            intent.setFlags ( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                              | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            mContext.startActivity ( intent );
        }
}
