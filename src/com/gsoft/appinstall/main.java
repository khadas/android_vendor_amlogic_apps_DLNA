package com.gsoft.appinstall;


import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView.BufferType;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageDeleteObserver;
import android.os.PowerManager;
import android.content.Context;

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
	private String TAG = "com.gsoft.appinstall";
	protected String mScanRoot = null;
    protected ArrayList<APKInfo> m_ApkList = new ArrayList<APKInfo>();
    protected CheckAbleList m_list = null;
    protected TextView m_info = null;
    protected PackageAdapter pkgadapter = null;
    protected EditText editdir = null;
    protected ProgressDialog mScanDiag = null;
    protected ProgressDialog mHandleDiag = null;
    private String m_version = "V1.1.0";
    private String m_releasedate = "2010.04.14";
    
    public final static int END_SCAN = 0;
    public final static int NEW_DIR = 1;
    public final static int NEW_APK = 2;
    public final static int SEL_APK = 3;
    public final static int HANDLE_PKG_NEXT = 4;
    public final static int END_HANDLE_PKG = 5;
    private boolean bstopscan = false;
    PowerManager.WakeLock mScreenLock = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //keep system awake
        mScreenLock = ((PowerManager)this.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,TAG);

        m_info = (TextView)findViewById(R.id.ScanInfo);
        m_info.setText("No APK Found!");
        m_info.setVisibility(android.view.View.INVISIBLE);
        //init the listview
    	m_list = (CheckAbleList)findViewById(R.id.APKList);
    	m_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        RetainData hdata = (RetainData)getLastNonConfigurationInstance();
        if(hdata == null)
        {
        	mScanRoot = "/mnt/sdcard";
        	pkgadapter = new PackageAdapter(this,R.layout.listitem,R.id.appname,R.id.apk_filepath,R.id.InstallState,R.id.APKIcon,R.id.Select,m_ApkList,m_list);
            scan();
        }
        else
        {
        	pkgadapter = hdata.pkgadapter;
        	mScanRoot = hdata.pCurPath;
        	m_ApkList = hdata.pApkList;
        	m_list.setAdapter(pkgadapter);
        	if(m_ApkList.isEmpty() == true)
        	{
        		m_info.setVisibility(android.view.View.VISIBLE);
        		findViewById(R.id.Exit).requestFocus();
        		findViewById(R.id.Exit).requestFocusFromTouch();
        	}
        	else
        	{
                m_list.requestFocus();
                m_list.requestFocusFromTouch();
        	}
        }

        m_list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
				public void onItemClick(AdapterView<?> parent, View view,int position, long id) 
				{
					APKInfo apkinfo = m_ApkList.get(position);
					if(apkinfo.isInstalled() == true)
						uninstall_apk(apkinfo.pCurPkgName);
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
        ImageButton hexit = (ImageButton)findViewById(R.id.Exit);
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
    
    public void onPause()
    {
    	//stop scan
		bstopscan = true;
		if(mScanDiag!=null)
		{
			mScanDiag.dismiss();
		}

		//stop install
		m_bStopHandle = true;
    	if(mHandleDiag!=null)
    	{
    		mHandleDiag.dismiss();
    	}
		
    	super.onPause();
    }

    protected void KeepSystemAwake(boolean bkeep)
    {
        if(bkeep == true)
        {
            if(mScreenLock.isHeld() == false)    		
                mScreenLock.acquire();
        }
        else
        {
            if(mScreenLock.isHeld() == true)    		
                mScreenLock.release();
        }
    }
    
    
    class RetainData extends Object
    {
    	PackageAdapter pkgadapter;
    	String		   pCurPath;
    	ArrayList<APKInfo> pApkList;
    }
    
    public Object onRetainNonConfigurationInstance()
    {
    	RetainData hdata = new RetainData();
    	hdata.pkgadapter = pkgadapter;
    	hdata.pCurPath = mScanRoot;
    	hdata.pApkList = m_ApkList;
    	return hdata;
    }
    
    
    public Handler mainhandler = new Handler()
    {
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch(msg.what)
			{
				case END_SCAN:
                    KeepSystemAwake(false);
					if(mScanDiag != null)
					{
						mScanDiag.dismiss();
					}

					if(m_ApkList.size() > 0)
					{
						m_list.setAdapter(pkgadapter);
						m_list.setVisibility(android.view.View.VISIBLE);
						m_info.setVisibility(android.view.View.INVISIBLE);
				        m_list.requestFocus();
				        m_list.requestFocusFromTouch();
					}
					else
					{
						m_list.setVisibility(android.view.View.INVISIBLE);
						m_info.setVisibility(android.view.View.VISIBLE);
		        		findViewById(R.id.Exit).requestFocus();
		        		findViewById(R.id.Exit).requestFocusFromTouch();
					}
					
					//pkgadapter.notifyDataSetChanged();
					break;
				case NEW_DIR:
				case NEW_APK:
					showProcessDiag(msg.arg1,msg.arg2);
					break;
				case SEL_APK:
					boolean bsel = false;
					if(msg.arg2 == 1)
						bsel = true;
					m_list.setItemChecked(msg.arg1, bsel);
					break;
				case HANDLE_PKG_NEXT:
					if(m_bStopHandle == true)
					{
                        KeepSystemAwake(false);
						if(mHandleDiag != null)
							mHandleDiag.dismiss();
						pkgadapter.notifyDataSetChanged();
					}
					else
					{
						m_handleitem++;
						if(m_handleitem < m_checkeditems.length)
						{
							String hanlemsg = null;
							int actionid;
							String actionpara = null;
							APKInfo pinfo = m_ApkList.get((int)m_checkeditems[(int) m_handleitem]);
							if(pinfo != null)
							{
								if(pinfo.isInstalled()==false)
								{
									actionid = 0;
									actionpara = pinfo.filepath;
									hanlemsg = "Installing  \"";
								}
								else
								{
									actionid = 1;
									actionpara = pinfo.pCurPkgName;
									hanlemsg = "Uninstalling  \"";
								}
								
								hanlemsg += pinfo.pAppName+"\"\n";
								showHandleProcessDiag(hanlemsg,(int)m_handleitem+1,m_checkeditems.length);
								new PkgHandleThread().setAction(actionid, actionpara)
													.start();
							}
							else
								Log.d(TAG,"got a null apkinfo, this is strange!");
						}
						else
						{
							m_bStopHandle = true;
							if(mHandleDiag != null)
								mHandleDiag.dismiss();
							pkgadapter.notifyDataSetChanged();
                            KeepSystemAwake(false);
						}
					}
					break;
				default:
					break;
			}
		}
    };
    
    //option menu
    protected final int MENU_INSTALL = 0;
    protected final int MENU_SELECT_ALL = 1;
    protected final int MENU_UNSELECT_ALL = 2;
    protected final int MENU_FRESH = 3;
    protected final int MENU_ABOUT = 4;
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_INSTALL, 0, "Install/Uninstall");
        menu.add(0, MENU_SELECT_ALL, 0, "Select all");
        menu.add(0, MENU_UNSELECT_ALL, 0, "Unselect all");
        menu.add(0, MENU_FRESH, 0, "Fresh");
        menu.add(0, MENU_ABOUT, 0, "About");
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
	        case MENU_INSTALL:
	        	HandleSelectedApks();
	            return true;
	        case MENU_SELECT_ALL:
	        	m_list.setAllItemChecked(true);
	        	return true;
	        case MENU_UNSELECT_ALL:
	        	m_list.setAllItemChecked(false);
	            return true;
	        case MENU_FRESH:
	        	scan();
	        	return true;
	        case MENU_ABOUT:
	        	String aboutinfo = "AppInstaller";
	        	aboutinfo += "\n Version: "+m_version+" ";
	        	aboutinfo += "\n Date: "+m_releasedate+" ";
	        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        	builder.setMessage(aboutinfo);
	        	AlertDialog about = builder.create();
	        	about.show();
	        	return true;
        }
        return false;
    }
    
    
    //user functions
    //===================================================================
    //functions for installing and uninstalling
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
    
    //===================================================================
    //functions for installing and uninstalling in slient mode
    class PkgHandleThread extends Thread
    {
    	int m_action;
    	String m_actionpara;
    	public PkgHandleThread setAction(int actionid,String para)
    	{
    		m_action = actionid;
    		m_actionpara = para;
    		return this;
    	}
    	public void run()
    	{
    		if(m_action == 0)//install pkg
    		{
    			install_apk_slient(m_actionpara);
    		}
    		else//uninstall pkg
    		{
    			uninstall_apk_slient(m_actionpara);
    		}
    	}
    };
    
    
    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String packageName, int returnCode) {
        	Message endmsg = new Message();
    		endmsg.what = HANDLE_PKG_NEXT;
    		endmsg.arg1 = returnCode;
    		endmsg.arg2 = 0;//means install
    		mainhandler.sendMessage(endmsg);
        }
    }
    public void install_apk_slient(String apk_filepath)
    {
    	PackageManager pm = getPackageManager();
        PackageInstallObserver observer = new PackageInstallObserver();
        pm.installPackage(Uri.fromFile(new File(apk_filepath)), observer, 0, null);
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(boolean succeeded) {
        	Message endmsg = new Message();
    		endmsg.what = HANDLE_PKG_NEXT;
    		endmsg.arg1 = 0;
    		if(succeeded == true)
    			endmsg.arg1 = 1;
    		endmsg.arg2 = 1;//means uninstall
    		mainhandler.sendMessage(endmsg);
        }
    }
    public void uninstall_apk_slient(String apk_pkgname)
    {
        PackageDeleteObserver observer = new PackageDeleteObserver();
    	PackageManager pm = getPackageManager();
        pm.deletePackage(apk_pkgname, observer, 0);
    }
    
    private long [] m_checkeditems = null;
    private long m_handleitem;
    public void HandleSelectedApks()
    {
    	m_bStopHandle = false;
    	m_checkeditems = m_list.getCheckedItemIds();
    	if(m_checkeditems.length == 0)
    		showmsg("you havn't select apks to install/uninstall");
    	else
    	{
            KeepSystemAwake(true);
	    	m_handleitem = -1;
	    	Message endmsg = new Message();
			endmsg.what = HANDLE_PKG_NEXT;
			endmsg.arg1 = 0;
			mainhandler.sendMessage(endmsg);
    	}
    }
    
    private boolean m_bStopHandle = false;
    protected void showHandleProcessDiag(String handlemsg,int curpkg,int totalpkg)
    {
    	if(mHandleDiag == null)
    	{
    		if(m_bStopHandle == true)
    			return ;
    		mHandleDiag = new ProgressDialog(this)
	    	{
	    		public boolean onTouchEvent (MotionEvent event)
	    		{
	    			m_bStopHandle = true;	
	    			return true;
	    		}
	    	};
	    	mHandleDiag.setCancelable(false);
    	}

    	String msg = "Handling selected package:  ";
    	msg += String.valueOf(curpkg)+"/";
    	msg += String.valueOf(totalpkg)+"\n";
    	msg += handlemsg;
    	mHandleDiag.setMessage(msg);
    	mHandleDiag.show();
    }
    
    
    //===================================================================
    //functions for scanning apks
    protected void showmsg(String msgcontent)
    {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(this, msgcontent, duration);
		toast.show();	
    }
    protected void showProcessDiag(int dirs,int apks)
    {
    	if(mScanDiag == null)
    	{
    		if(bstopscan == true)
    			return ;

        	mScanDiag = new ProgressDialog(this)
        	{
        		public boolean onTouchEvent (MotionEvent event)
        		{
        			bstopscan = true;
    				mScanDiag.setMessage("stop scanning\n");
        			return true;
        		}
        	};
        	mScanDiag.setCancelable(false);
    	}

    	if(bstopscan == false)
    	{
	    	String msg = "Scanning ...\n";
	    	msg += "dir : "+String.valueOf(dirs)+"\n";
	    	msg += "apk  : "+String.valueOf(apks)+"\n";
	    	mScanDiag.setMessage(msg);
    	}
    	else
    		mScanDiag.setMessage("stop scanning\n");
    	mScanDiag.show();
    }
    

    protected void scan()
    {
        KeepSystemAwake(true);
    	showProcessDiag(0,0);
    	bstopscan = false;
    	new scanthread().start();
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
        		if(pfile.exists() == true)
        		{
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
    
}
