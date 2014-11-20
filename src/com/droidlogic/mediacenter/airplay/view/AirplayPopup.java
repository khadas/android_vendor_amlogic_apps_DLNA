package com.droidlogic.mediacenter.airplay.view;

import java.util.ArrayList;
import com.droidlogic.mediacenter.R;
import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

public class AirplayPopup
{
        public static final String TAG = "AirplayerPopup";
        
        public static PopupMenu createPopupMenu ( Context context, View anchorView,
                final ArrayList<String> players, final Listener listener )
        {
            final Context mContext = context;
            final PopupMenu popupMenu = new PopupMenu ( context, anchorView );
            popupMenu.inflate ( R.menu.popup );
            final PopupMenu.OnMenuItemClickListener clickListener = new PopupMenu.OnMenuItemClickListener()
            {
                public boolean onMenuItemClick ( MenuItem item )
                {
                    listener.onAirplayDeviceChosen ( item.getTitle().toString() );
                    return true;
                }
            };
            popupMenu.setOnMenuItemClickListener ( clickListener );
            
        for ( String player : players )
            {
                popupMenu.getMenu().add ( player );
            }
            
            return popupMenu;
        }
        
        public interface Listener
        {
            void onAirplayDeviceChosen ( String dev );
        }
}
