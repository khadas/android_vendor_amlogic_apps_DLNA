/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.droidlogic.mediacenter.airplay.image;

import nz.co.iswe.android.airplay.video.HttpVideoHandler;
import android.app.ActionBar;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.amlogic.util.Debug;
import com.amlogic.airplay.CacheBuffer;

/**
 * Fragment that shows the content selected from the TitlesFragment. When
 * running on a screen size smaller than "large", this fragment is hosted in
 * ContentActivity. Otherwise, it appears side by side with the TitlesFragment
 * in MainActivity.
 */
import com.droidlogic.mediacenter.R;
public class ContentFragment extends Fragment {
        private final String TAG = "ContentFragment";
        private View mContentView;
        private int mImageId = -1;
        private boolean mSystemUiVisible = true;

        // The bitmap currently used by ImageView
        private Bitmap mBitmap = null;

        /**
         * This is where we initialize the fragment's UI and attach some event
         * listeners to UI components.
         */
        @Override
        public View onCreateView ( LayoutInflater inflater, ViewGroup container,
                                   Bundle savedInstanceState ) {
            mContentView = inflater.inflate ( R.layout.content_welcome, null );
            // Show/hide the system status bar when single-clicking a photo.
            mContentView.setOnClickListener ( new OnClickListener() {
                public void onClick ( View view ) {
                    /*  if (mSystemUiVisible) {
                            setSystemUiVisible(false);
                        } else {
                            setSystemUiVisible(true);
                        }*/
                }
            } );
            return mContentView;
        }

        /**
         * This is where we perform additional setup for the fragment that's either
         * not related to the fragment's layout or must be done after the layout is
         * drawn.
         */
        @Override
        public void onActivityCreated ( Bundle savedInstanceState ) {
            super.onActivityCreated ( savedInstanceState );
            // Current position and UI visibility should survive screen rotations.
            if ( savedInstanceState != null ) {
                setSystemUiVisible ( savedInstanceState.getBoolean ( "systemUiVisible" ) );
                mImageId = savedInstanceState.getInt ( "image_id" );
                updateContentAndRecycleBitmap ( mImageId );
            }
        }

        @Override
        public boolean onOptionsItemSelected ( MenuItem item ) {
            return super.onOptionsItemSelected ( item );
        }

        @Override
        public void onSaveInstanceState ( Bundle outState ) {
            super.onSaveInstanceState ( outState );
            outState.putInt ( "image_id", mImageId );
            outState.putBoolean ( "systemUiVisible", mSystemUiVisible );
        }

        /**
         * Toggle whether the system UI (status bar / system bar) is visible. This
         * also toggles the action bar visibility.
         *
         * @param show
         *            True to show the system UI, false to hide it.
         */
        void setSystemUiVisible ( boolean show ) {
            mSystemUiVisible = show;
            Window window = getActivity().getWindow();
            WindowManager.LayoutParams winParams = window.getAttributes();
            View view = getView();
            ActionBar actionBar = getActivity().getActionBar();
            if ( actionBar == null )
            { return; }
            if ( show ) {
                // Show status bar (remove fullscreen flag)
                window.setFlags ( 0, WindowManager.LayoutParams.FLAG_FULLSCREEN );
                // Show system bar
                view.setSystemUiVisibility ( View.STATUS_BAR_VISIBLE );
                // Show action bar
                actionBar.show();
            } else {
                // Add fullscreen flag (hide status bar)
                window.setFlags ( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                  WindowManager.LayoutParams.FLAG_FULLSCREEN );
                // Hide system bar
                view.setSystemUiVisibility ( View.STATUS_BAR_HIDDEN );
                // Hide action bar
                actionBar.hide();
            }
            window.setAttributes ( winParams );
        }

        /**
         * Sets the current image visible.
         */
        public void updateContentAndRecycleBitmap ( int imageId ) {
            mImageId = imageId;
            if ( mBitmap != null ) {
                // This is an advanced call and should be used if you
                // are working with a lot of bitmaps. The bitmap is dead
                // after this call.
                mBitmap.recycle();
            }
            // Get the bitmap that needs to be drawn and update the ImageView
            CacheBuffer buffer = HttpVideoHandler.getImageContent ( imageId );
            Debug.i ( TAG, "AssetKey=" + buffer.getAssetKey() + ",buffer.size=" + buffer.size() );
            mBitmap = BitmapFactory.decodeByteArray ( buffer.toByteArray(), 0,
                      buffer.toByteArray().length );
            if ( mBitmap != null ) {
                ImageView view = ( ( ImageView ) getView().findViewById ( R.id.image ) );
                view.startAnimation ( AnimationUtils.loadAnimation ( getActivity(), R.anim.fade_in ) );
                view.setImageBitmap ( mBitmap );
            } else
            { Debug.i ( TAG, "FUCK!!!Bitmap decode error" ); }
        }

}
