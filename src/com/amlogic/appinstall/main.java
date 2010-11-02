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
import android.view.View;
import android.widget.AdapterView;
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
    final static int END_MSG_CODE = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //init the listview
    	m_list = (ListView)findViewById(R.id.APKList);
    	
        RetainData hdata = (RetainData)getLastNonConfigurationInstance();
        if(hdata == null)
        {
        	mScanRoot = "/mnt/sdcard";
        	pkgadapter = new PackageAdapter(this,R.layout.listitem,R.id.appname,R.id.apk_filepath,R.id.InstallState,R.id.APKIcon,m_ApkList);
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
        Button hexit = (Button)findViewById(R.id.Exit);
        hexit.setText("Exit");
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
				case 0:
					if(mProcDiag!=null)
						mProcDiag.hide();
					
					pkgadapter.notifyDataSetChanged();
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
    };
    

    //>>>>>>>>>>>>>>>>>>>>>>>user functions
    protected void showmsg(String msgcontent)
    {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(this, msgcontent, duration);
		toast.show();	
    }

    protected void scan()
    {
    	if(mProcDiag == null)
    	{
	    	mProcDiag = new ProgressDialog(this);
	    	mProcDiag.setMessage("Scanning ...");
	    	mProcDiag.setCancelable(false);
    	}
    	mProcDiag.show();
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
    		endmsg.what = END_MSG_CODE;
    		mainhandler.sendMessage(endmsg);
    	}
        protected void scandir(String directory)
        {
        	//clear the apklist
        	m_ApkList.clear();
        	//to scan dirs
        	ArrayList<String> pdirlist = new ArrayList<String>();
        	pdirlist.add(directory);
        	
        	while(pdirlist.isEmpty() == false)
        	{
        		String headpath = pdirlist.remove(0);
        		File pfile = new File(headpath);
            	//list files and dirs in this directory
        		File[] files = pfile.listFiles(new APKFileter());
        		if(files != null && (files.length > 0) )
        		{
    	    		int i = 0;
    	    		for(;i<files.length;i++)
    	    		{
    	    			File pcurfile = files[i];
    	    			if(pcurfile.isDirectory())
    	    				pdirlist.add(pcurfile.getAbsolutePath());
    	    			else
    	    			{
    	    				APKInfo apkinfo = new APKInfo(main.this,pcurfile.getAbsolutePath());
    	    				if(apkinfo.beValid() == true)
    	    					m_ApkList.add(apkinfo);
    	    			}
    	    		}
        		}

        	}
        }
    }
    
}
