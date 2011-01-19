package com.gsoft.appinstall;

import java.util.ArrayList;
import android.content.res.AssetManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

class APKInfo extends Object
{
	static protected PackageManager pkgmgr = null;

	APKInfo(Context pcontext, String apkpath)
	{
		pkgmgr = pcontext.getPackageManager();
		filepath = apkpath;
		pPkgInfo = pkgmgr.getPackageArchiveInfo(filepath, PackageManager.GET_ACTIVITIES);
		if(pPkgInfo!=null)
		{
			//get package's name in system
			String[] curpkgnames = pkgmgr.canonicalToCurrentPackageNames(new String[]{pPkgInfo.packageName});
			if( (curpkgnames!=null) && (curpkgnames.length > 0) && (curpkgnames[0]!=null) )
				pCurPkgName = curpkgnames[0];
			else
				pCurPkgName = pPkgInfo.packageName;	
			
			pPkgRes = null;
			AssetManager assmgr = new AssetManager();
			if(0 != assmgr.addAssetPath(filepath))
				pPkgRes = new Resources(assmgr, pcontext.getResources().getDisplayMetrics(), pcontext.getResources().getConfiguration());
		}
	}
	
	public boolean beValid()
	{
		if(pPkgInfo != null && pPkgRes!=null)
			return true;
		else
			return false;
	}
	
	public String filepath;
	public PackageInfo pPkgInfo;
	
	public String  pCurPkgName = null;
	public Resources pPkgRes = null;
	public CharSequence  pAppName = null;
	public Drawable pAppIcon = null;

	
	public boolean isInstalled()
	{
		ApplicationInfo appinfo = null;
        try {
        	appinfo = pkgmgr.getApplicationInfo(pCurPkgName,PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
        	appinfo = null;
        }
        
        if(appinfo == null)
        	return false;
        else
        	return true;
	}
	
	public CharSequence getApplicationName()
	{
		if(pAppName == null)
		{
			if(pPkgRes!=null)
			{
				try
				{
					pAppName = pPkgRes.getText(pPkgInfo.applicationInfo.labelRes);
				}
				catch (Resources.NotFoundException resnotfound)
				{
					pAppName = pCurPkgName;
				}
			}
			else
				pAppName = pCurPkgName;
		}
		return pAppName;
	}

	public Drawable getApkIcon()
	{
		if(pAppIcon == null)
		{
			if(pPkgRes!=null)
			{
				try
				{
					pAppIcon = pPkgRes.getDrawable(pPkgInfo.applicationInfo.icon);
				}
				catch (Resources.NotFoundException resnotfound)
				{
					pAppIcon = pkgmgr.getApplicationIcon(pPkgInfo.applicationInfo);
				}
			}
			else
				pAppIcon = pkgmgr.getApplicationIcon(pPkgInfo.applicationInfo);
		}
		return pAppIcon;
	}
	
}


public class PackageAdapter extends BaseAdapter {
	
	protected int m_Layout_APKListItem;
	protected int m_TextView_AppName;
	protected int m_TextView_FileName;
	protected int m_ImgView_APPIcon;
	protected int m_CheckBox_InstallState;
	protected int m_CheckBox_SelState;
    private   LayoutInflater mInflater;
    protected ArrayList<APKInfo>  m_apklist = null;
    protected ListView m_list = null;

	PackageAdapter(Context context,int Layout_Id,int text_app_id,int text_filename_id,int checkbox_id,int img_appicon_id,int checkbox_sel_id,ArrayList<APKInfo> apklist,ListView list)
	{
        mInflater = LayoutInflater.from(context);
        m_Layout_APKListItem = Layout_Id;
        m_TextView_FileName = text_filename_id;
        m_TextView_AppName = text_app_id;
        m_CheckBox_InstallState = checkbox_id;
        m_ImgView_APPIcon = img_appicon_id;
        m_CheckBox_SelState = checkbox_sel_id;
        m_apklist = apklist;
        m_list = list;
	}
	
	public int getCount() {
		if(m_apklist != null)
			return m_apklist.size();
		else
			return 0;
	}

	public Object getItem(int position) {
		if(m_apklist != null)
			return m_apklist.get(position);
		else
			return null;
	}

	public long getItemId(int position) {
		return position;
	}
	
	
	class SelStateListener implements CompoundButton.OnCheckedChangeListener
	{
		int PosInList = 0;
		SelStateListener(int pos)
		{
			PosInList = pos;
		}

		public void onCheckedChanged (CompoundButton buttonView, boolean isChecked)
		{
			if(m_list.isItemChecked(PosInList) != isChecked)
				m_list.setItemChecked(PosInList, isChecked);
		}
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View layoutview = null;
		if(convertView==null)
		{
			layoutview = mInflater.inflate(m_Layout_APKListItem,parent,false);
		}
		else
		{
			layoutview = convertView;
		}
		TextView FileName = (TextView)layoutview.findViewById(m_TextView_FileName);
		TextView AppName = (TextView)layoutview.findViewById(m_TextView_AppName);
		CheckBox InstallState = (CheckBox)layoutview.findViewById(m_CheckBox_InstallState);
		ImageView Appicon = (ImageView)layoutview.findViewById(m_ImgView_APPIcon);
		CheckBox SelState = (CheckBox)layoutview.findViewById(m_CheckBox_SelState);
		SelState.setOnCheckedChangeListener(new SelStateListener(position));
		
		APKInfo pinfo = (APKInfo)getItem(position);
		FileName.setText(pinfo.filepath);
		AppName.setText(pinfo.getApplicationName());
		InstallState.setChecked(pinfo.isInstalled());
		Appicon.setImageDrawable(pinfo.getApkIcon());
		SelState.setChecked(m_list.isItemChecked(position));

		return layoutview;
	}
	
    public boolean hasStableIds() {
        return true;
    }

}
