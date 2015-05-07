/**
 * @Package com.droidlogic.mediacenter
 * @Description
 *
 * Copyright (c) Inspur Group Co., Ltd. Unpublished
 *
 * Inspur Group Co., Ltd.
 * Proprietary & Confidential
 *
 * This source code and the algorithms implemented therein constitute
 * confidential information and may comprise trade secrets of Inspur
 * or its associates, and any use thereof is subject to the terms and
 * conditions of the Non-Disclosure Agreement pursuant to which this
 * source code was originally received.
 */
package com.droidlogic.mediacenter.dlna;

import com.droidlogic.mediacenter.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * @ClassName DMRError
 * @Description TODO
 * @Date 2013-9-5
 * @Email
 * @Author
 * @Version V1.0
 */
public class DMRError extends Activity implements OnClickListener {
        private Button btn;
        @Override
        protected void onCreate ( Bundle arg0 ) {
            super.onCreate ( arg0 );
            setContentView ( R.layout.dmr_error_dialog );
            btn = ( Button ) findViewById ( R.id.btn_ok );
            btn.setOnClickListener ( this );
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
        }

        @Override
        protected void onResume() {
            super.onResume();
        }

        @Override
        protected void onStop() {
            super.onStop();
            exitApp();
        }

        /* (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick ( View arg0 ) {
            exitApp();
        }

        private void exitApp() {
            ActivityManager activityMgr = ( ActivityManager ) getSystemService ( ACTIVITY_SERVICE );
            activityMgr.forceStopPackage ( getPackageName() );
            this.stopService ( new Intent ( DMRError.this, MediaCenterService.class ) );
        }
}
