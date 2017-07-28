/******************************************************************
*
*Copyright (C) 2016  Amlogic, Inc.
*
*Licensed under the Apache License, Version 2.0 (the "License");
*you may not use this file except in compliance with the License.
*You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing, software
*distributed under the License is distributed on an "AS IS" BASIS,
*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*See the License for the specific language governing permissions and
*limitations under the License.
******************************************************************/
package com.droidlogic.mediacenter.airplay;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.util.Log;
import com.droidlogic.mediacenter.MediaCenterApplication;
import com.droidlogic.mediacenter.dlna.LoadingDialog;
import com.droidlogic.mediacenter.R;
import android.graphics.Matrix;
import android.graphics.Rect;

public class Photo extends Activity {
        private ImageView flipper;
        private TextView mClientName;
        final static String TAG = "PhotoPlayer";
        private static final int STOPHANDLE = 2;
        private static final int SHOWLOADING = 3;
        private String fromsource = "AIRPLAY";
        private Bitmap mBitmap = null;
        private String imagefile = "";
        //private boolean mHasNext = false;
        private AlphaAnimation aa = null;
        private String mAppleSessionId = "";
        private IntentFilter mFilter = null;
        private FrameLayout container;
        private LoadingDialog        mLoadingDialog;
        private PlaybackReceiver sReceiver = new PlaybackReceiver() ;
        private static final float   TOPSIZE             = 2048.0f;

        public class PlaybackReceiver extends BroadcastReceiver {
                @Override
                public void onReceive ( Context context, Intent intent ) {
                    processExtraData ( intent );
                }
        }
        private void registerReceivers() {
            mFilter = new IntentFilter();
            mFilter.addAction ( "displayimage" );
            mFilter.addAction ( "stopimage" );
            registerReceiver ( sReceiver, mFilter );
        }

        private void unRegisterReceivers() {
            unregisterReceiver ( sReceiver );
        }

        @Override
        public void onCreate ( Bundle savedInstanceState ) {
            super.onCreate ( savedInstanceState );
            requestWindowFeature ( Window.FEATURE_NO_TITLE );
            getWindow().setFlags ( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            container = new FrameLayout ( this );
            android.view.ViewGroup.LayoutParams lp1 = new LayoutParams ( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT );
            container.setLayoutParams ( lp1 );
            container.setBackgroundColor ( Color.BLACK );
            flipper = new ImageView ( this );
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams ( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, Gravity.CENTER );
            flipper.setLayoutParams ( lp );
            flipper.setScaleType ( ScaleType.FIT_CENTER );
            flipper.setBackgroundColor ( Color.BLACK );
            container.addView ( flipper );
            mClientName = new TextView ( this );
            lp = new FrameLayout.LayoutParams ( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.BOTTOM );
            mClientName.setLayoutParams ( lp );
            mClientName.setTextColor ( Color.WHITE );
            //container.addView(mClientName);
            setContentView ( container );
            aa = new AlphaAnimation ( 0.0f, 1.0f );
            aa.setDuration ( 500 );
            Intent intent = getIntent();
            processExtraData ( intent );
        }

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage ( Message msg ) {
                switch ( msg.what ) {
                    case 1:
                        if ( mBitmap != null && ( flipper != null ) ) {
                            showImage();
                        } else {
                            stop();
                        }
                        break;
                    case STOPHANDLE:
                        stop();
                        break;
                    case SHOWLOADING:
                        showLoading();
                        break;
                }
            }
        };
        @Override
        protected void onNewIntent ( Intent intent ) {
            super.onNewIntent ( intent );
        }
        private void showImage() {
            if ( mBitmap != null ) {
                int height = mBitmap.getHeight();
                int width = mBitmap.getWidth();
                float reSize = 1.0f;
                if ( width > TOPSIZE || height > TOPSIZE ) {
                    if ( height > width ) {
                        reSize = TOPSIZE / height;
                    } else {
                        reSize = TOPSIZE / width;
                    }
                    Matrix matrix = new Matrix();
                    matrix.postScale ( reSize, reSize );
                    mBitmap = Bitmap.createBitmap ( mBitmap, 0, 0, width,
                                                    height, matrix, true );
                    flipper.setImageBitmap ( mBitmap );
                } else {
                    flipper.setImageBitmap ( mBitmap );
                }
                flipper.startAnimation ( aa );
                System.gc();
            } else {
                flipper.setImageResource ( R.drawable.ic_missing_thumbnail_picture );
                Toast.makeText ( getApplicationContext(), R.string.disply_err,
                                 Toast.LENGTH_SHORT ).show();
            }
            hideLoading();
        }
        private void processExtraData ( Intent intent ) {
            Bundle bundle = intent.getExtras();
            String mAction = intent.getAction();
            Log.d ( "tt", "processExtraData=======" + intent.getAction() );
            if ( mAction == null ) {
                flipper = null;
                finish();
                return;
            }
            if ( bundle != null ) {
                imagefile = bundle.getString ( "IMAGE", "" );
                mAppleSessionId = bundle.getString ( "SESSIONID", "" );
                mClientName.setText ( bundle.getString ( "SOURCE", "" ) );
            }
            if ( mAction.equals ( "stopimage" ) ) {
                stop();
            } else if ( mAction.equals ( "displayimage" ) ) {
                if ( imagefile.equals ( "" ) ) {
                    //invalid
                    stop();
                    return;
                }
                if ( imagefile.toLowerCase().startsWith ( "http://" ) ) { //network image url
                    showLoading();
                    new Thread() {
                        public void run() {
                            if ( mBitmap != null ) {
                                mBitmap = null;
                            }
                            Message msg = new Message();
                            mBitmap = getBitmapFromUrl ( imagefile );
                            msg.what = 1;
                            mHandler.sendMessage ( msg );
                        }
                    } .start();
                } else { //local image file
                    showLoading();
                    new Thread() {
                        public void run() {
                            if ( mBitmap != null ) {
                                //mBitmap.recycle();
                                mBitmap = null;
                            }
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile ( imagefile, options );
                            int pow = 0;
                            while ( options.outHeight >> pow > 1920 || options.outWidth >> pow > 1080 ) {
                                pow += 1;
                            }
                            options.inSampleSize = 1 << pow;
                            options.inJustDecodeBounds = false;
                            mBitmap = BitmapFactory.decodeFile ( imagefile, options );
                            Message msg = new Message();
                            msg.what = 1;
                            mHandler.sendMessage ( msg );
                        }
                    } .start();
                }
            } else {
                stop();
            }
        }

        private Bitmap getBitmapFromUrl ( String imgUrl ) {
            URL url = null;
            Bitmap bp = null;
            try {
                url = new URL ( imgUrl );
                HttpURLConnection httpConnection = ( HttpURLConnection ) url.openConnection();
                httpConnection.setReadTimeout ( 5000 );
                httpConnection.setRequestMethod ( "GET" );
                httpConnection.connect();
                InputStream is = httpConnection.getInputStream();
                //bp = BitmapFactory.decodeStream(is);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream ( is, null, options );
                is.close();
                httpConnection.disconnect();
                int pow = 0;
                while ( options.outHeight >> pow > 1920 || options.outWidth >> pow > 1080 ) {
                    pow += 1;
                }
                options.inSampleSize = 1 << pow;
                options.inJustDecodeBounds = false;
                //
                url = new URL ( imgUrl );
                httpConnection = ( HttpURLConnection ) url.openConnection();
                httpConnection.setReadTimeout ( 5000 );
                httpConnection.setRequestMethod ( "GET" );
                httpConnection.connect();
                InputStream is1 = httpConnection.getInputStream();
                //
                bp = BitmapFactory.decodeStream ( is1, null, options );
                is1.close();
                httpConnection.disconnect();
            } catch ( MalformedURLException e ) {
                //e.printStackTrace();
                return null;
            } catch ( IOException e ) {
                //e.printStackTrace();
                return null;
            }
            return bp;
        }

        private View addTextView ( int id ) {
            ImageView iv = new ImageView ( this );
            iv.setImageResource ( id );
            return iv;
        }

        private View addTextView ( Bitmap bp ) {
            ImageView iv = new ImageView ( this );
            iv.setImageBitmap ( bp );
            iv.setScaleType ( ScaleType.CENTER_INSIDE );
            return iv;
        }

        private View getTextView ( Bitmap bp ) {
            ImageView iv = new ImageView ( this );
            iv.setImageBitmap ( bp );
            iv.setScaleType ( ScaleType.CENTER_INSIDE );
            return iv;
        }
        private void stop() {
            hideLoading();
            mBitmap = null;
            aa = null;
            flipper = null;
            finish();
        }

        @Override
        protected void onPause() {
            super.onPause();
            hideLoading();
            unRegisterReceivers();
            MediaCenterApplication.setPhoto ( false );
            finish();
        }

        @Override
        protected void onResume() {
            super.onResume();
            MediaCenterApplication.setPhoto ( true );
            registerReceivers();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onBackPressed() {
            super.onBackPressed();
            flipper = null;
            finish();
        }
        private void hideLoading() {
            android.util.Log.d ( "tt", "hideLoading" );
            if ( mLoadingDialog != null ) {
                mLoadingDialog.stopAnim();
                mLoadingDialog.dismiss();
            }
        }
        private void showLoading() {
            android.util.Log.d ( "tt", "showLoading" );
            if ( mLoadingDialog == null ) {
                mLoadingDialog = new LoadingDialog ( this,
                                                     LoadingDialog.TYPE_LOADING, this.getResources().getString (
                                                             R.string.loading ) );
                mLoadingDialog.setCancelable ( true );
                mLoadingDialog.show();
            } else {
                mLoadingDialog.show();
            }
        }


}
