package com.amlogic.appinstall;


import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView.BufferType;
import android.widget.TextView;
import android.widget.Toast;


class APKFileter implements FileFilter
{
	public boolean accept(File arg0) {
		if(arg0.isDirectory() == true)
			return true;
			
		String filename = arg0.getName();
		String filenamelowercase = filename.toLowerCase();
		return filenamelowercase.endsWith(".apk");	
	}
}

public class main extends Activity {
	protected String mScanRoot = null;
    protected ArrayList<APKInfo> m_ApkList = new ArrayList<APKInfo>();
    protected ListView m_list = null;
    protected PackageAdapter pkgadapter = null;
    protected EditText editdir = null;
    protected ProgressDialog mProcDiag = null;
    public final static int END_SCAN = 0;
    public final static int NEW_DIR = 1;
    public final static int NEW_APK = 2;
    public final static int SEL_APK = 3;
    private boolean bstopscan = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //init the listview
    	m_list = (ListView)findViewById(R.id.APKList);
    	m_list.setChoiceMode(ListView.CHOICE_MODE_NONE);
    	
    	
    	
        RetainData hdata = (RetainData)getLastNonConfigurationInstance();
        if(hdata == null)
        {
        	mScanRoot = "/mnt/sdcard";
        	pkgadapter = new PackageAdapter(this,R.layout.listitem,R.id.appname,R.id.apk_filepath,R.id.InstallState,R.id.APKIcon,0,m_ApkList,m_list);
            scan();
        }
        else
        {
        	pkgadapter = hdata.pkgadapter;
        	mScanRoot = hdata.pCurPath;
        }

        m_list.setAdapter(pkgadapter);
        m_list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
				public void onItemClick(AdapterView<?> parent, View view,int position, long id) 
				{
					APKInfo apkinfo = m_ApkList.get(position);
					if(apkinfo.isInstalled() == true)
						uninstall_apk(apkinfo.getPkgName());
					else
						install_apk(apkinfo.filepath);
				}
        });
        
        //change dir button
        editdir = (EditText)findViewById(R.id.Dir);
	    editdir.setText("/mnt/sdcard",TextView.BufferType.EDITABLE);
        
        ImageButton chgdir = (ImageButton)findViewById(R.id.ChangeDir);
        chgdir.setOnClickListener(new View.OnClickListener() 
        {
			public void onClick(View v) 
			{
				String dirpath = main.this.editdir.getText().toString();
				File pfile = new File(dirpath);
				if( pfile!=null && pfile.isDirectory()==true )
				{
					if((mScanRoot == null) || (mScanRoot.compareTo(dirpath) != 0))
					{
						mScanRoot = dirpath;
						scan();
					}
					else
						showmsg("same dir path");
				}
				else
				{
					showmsg("invalid dir path");
				}
			}
		});

        
        //exit button
        Button hexit = (Button)findViewById(R.id.exit);
        hexit.setOnClickListener(new View.OnClickListener() 
	        {
				public void onClick(View v) 
				{
					// TODO Auto-generated method stub
					main.this.finish();
				}
			}
        );
    }

    
    public void onResume()
    {
    	super.onResume();
		pkgadapter.notifyDataSetChanged();
    }
    
    class RetainData extends Object
    {
    	PackageAdapter pkgadapter;
    	String		   pCurPath;
    }
    
    public Object onRetainNonConfigurationInstance()
    {
    	RetainData hdata = new RetainData();
    	hdata.pkgadapter = pkgadapter;
    	hdata.pCurPath = mScanRoot;
    	return hdata;
    }
    
    
    public Handler mainhandler = new Handler()
    {
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch(msg.what)
			{
				case END_SCAN:
					bstopscan = true;
					if(mProcDiag != null)
						mProcDiag.hide();
					pkgadapter.notifyDataSetChanged();
					break;
				case NEW_DIR:
				case NEW_APK:
					showProcessDiag(msg.arg1,msg.arg2);
					break;
				case SEL_APK:
					Log.v("sel_apk",String.valueOf(msg.arg1));
					boolean bsel = false;
					if(msg.arg2 == 1)
						bsel = true;
					m_list.setItemChecked(msg.arg1, bsel);
					break;
				default:
					break;
			}
		}
    };
    
    //option menu
    protected final int MENU_INSTALL = 0;
    protected final int MENU_SELECT = 1;
    protected int m_selmode = 0;
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_INSTALL, 0, "Install/Uninstall");
        menu.add(0, MENU_SELECT, 0, "Select/Unselect all");
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
	        case MENU_INSTALL:
	            return true;
	        case MENU_SELECT:
	        	if(m_selmode == 0)//select all
	        	{
	        		Log.v("menu","select all");
	        		m_selmode = 1;
	        		int i = 0;
	        		for(;i<m_list.getCount();i++)
	        		{
	        			m_list.setItemChecked(i,true);
	        		}
	        		
	        	}
	        	else//unselect all
	        	{
	        		Log.v("menu","unselect all");
	        		m_selmode = 0;
	        		int i = 0;
	        		for(;i<m_list.getCount();i++)
	        		{
	        			m_list.setItemChecked(i,false);
	        		}
	        	}
	        	m_list.invalidate();
	            return true;
        }
        return false;
    }
    

    //>>>>>>>>>>>>>>>>>>>>>>>user functions
    protected void showmsg(String msgcontent)
    {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(this, msgcontent, duration);
		toast.show();	
    }
    protected void showProcessDiag(int dirs,int apks)
    {
    	if(mProcDiag == null)
    	{
	    	mProcDiag = new ProgressDialog(this)
	    	{
	    		public boolean onTouchEvent (MotionEvent event)
	    		{
	        		Message endmsg = new Message();
	        		endmsg.what = END_SCAN;
	        		mainhandler.sendMessage(endmsg);
	    			return true;
	    		}
	    	};
	    	mProcDiag.setCancelable(false);
	    	String msg = "Scanning ...\n";
	    	msg += "dir : "+String.valueOf(dirs)+"\n";
	    	msg += "apk  : "+String.valueOf(apks)+"\n";
	    	mProcDiag.setMessage(msg);
	    	mProcDiag.show();	
    	}
    	else
    	{
	    	String msg = "Scanning ...\n";
	    	msg += "dir : "+String.valueOf(dirs)+"\n";
	    	msg += "apk  : "+String.valueOf(apks)+"\n";
	    	mProcDiag.setMessage(msg);
    	}
    }

    protected void scan()
    {
    	showProcessDiag(0,0);
    	bstopscan = false;
    	new scanthread().start();
    }

    public void install_apk(String apk_filepath)
    {
    	Intent installintent = new Intent();
    	installintent.setComponent(new ComponentName("com.android.packageinstaller","com.android.packageinstaller.PackageInstallerActivity"));
    	installintent.setAction(Intent.ACTION_VIEW);
    	installintent.setData(Uri.fromFile(new File(apk_filepath)));
    	startActivity(installintent);
    }
    
    public void uninstall_apk(String apk_pkgname)
    {
    	Intent uninstallintent = new Intent();
    	uninstallintent.setComponent(new ComponentName("com.android.packageinstaller","com.android.packageinstaller.UninstallerActivity"));
    	uninstallintent.setAction(Intent.ACTION_VIEW);
    	uninstallintent.setData(Uri.fromParts("package",apk_pkgname,null));
    	startActivity(uninstallintent);
    }
    
    
    class scanthread extends Thread
    {
    	public void run()
    	{
    		scandir(mScanRoot);
    		Message endmsg = new Message();
    		endmsg.what = END_SCAN;
    		mainhandler.sendMessage(endmsg);
    	}
        protected void scandir(String directory)
        {
        	int dirs = 0,apks = 0;
        	//clear the apklist
        	m_ApkList.clear();
        	
        	//to scan dirs
        	ArrayList<String> pdirlist = new ArrayList<String>();
        	pdirlist.add(directory);
        	
        	while(pdirlist.isEmpty() == false && (bstopscan==false) )
        	{
        		dirs++;
        		Message dirmsg = new Message();
        		dirmsg.what = NEW_DIR;
        		dirmsg.arg1 = dirs;
        		dirmsg.arg2 = apks;
        		mainhandler.sendMessage(dirmsg);
        		
        		String headpath = pdirlist.remove(0);
        		File pfile = new File(headpath);
            	//list files and dirs in this directory
        		File[] files = pfile.listFiles(new APKFileter());
        		if(files != null && (files.length > 0) )
        		{
    	    		int i = 0;
    	    		for(;(i<files.length)&&((bstopscan==false));i++)
    	    		{
    	    			File pcurfile = files[i];
    	    			if(pcurfile.isDirectory())
    	    				pdirlist.add(pcurfile.getAbsolutePath());
    	    			else
    	    			{
    	    				APKInfo apkinfo = new APKInfo(main.this,pcurfile.getAbsolutePath());
    	    				if(apkinfo.beValid() == true)
    	    				{
    	    					m_ApkList.add(apkinfo);
    	    					apks++;
    	    		    		Message apkmsg = new Message();
    	    					apkmsg.what = NEW_APK;
    	    					apkmsg.arg1 = dirs;
    	    					apkmsg.arg2 = apks;
    	    	        		mainhandler.sendMessage(apkmsg);
    	    				}
    	    			}
    	    		}
        		}
        	}
        }
    }
    
}
