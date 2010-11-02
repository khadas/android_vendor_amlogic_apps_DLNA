package com.amlogic.appinstall;

import java.util.ArrayList;
import android.content.res.AssetManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
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

	public String getPkgName()
	{
		return pCurPkgName;
	}
	
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
    private   LayoutInflater mInflater;
    protected ArrayList<APKInfo>  m_apklist = null;

	PackageAdapter(Context context,int Layout_Id,int text_app_id,int text_filename_id,int checkbox_id,int img_appicon_id,ArrayList<APKInfo> apklist)
	{
        mInflater = LayoutInflater.from(context);
        m_Layout_APKListItem = Layout_Id;
        m_TextView_FileName = text_filename_id;
        m_TextView_AppName = text_app_id;
        m_CheckBox_InstallState = checkbox_id;
        m_ImgView_APPIcon = img_appicon_id;
        m_apklist = apklist;
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
		
		APKInfo pinfo = (APKInfo)getItem(position);
		FileName.setText(pinfo.filepath);
		AppName.setText(pinfo.getApplicationName());
		InstallState.setChecked(pinfo.isInstalled());
		Appicon.setImageDrawable(pinfo.getApkIcon());

		return layoutview;
	}

}
