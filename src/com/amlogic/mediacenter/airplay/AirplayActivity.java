package com.amlogic.mediacenter.airplay;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.amlogic.mediacenter.airplay.proxy.AirplayBroadcastFactory;
import com.amlogic.mediacenter.airplay.proxy.AirplayController;
import com.amlogic.mediacenter.airplay.proxy.AirplayProxy;
import com.amlogic.mediacenter.airplay.proxy.AirplayController.EventType;
import com.amlogic.mediacenter.airplay.view.AirplayPopup;
import com.amlogic.mediacenter.R;
public class AirplayActivity extends Activity implements AirplayPopup.Listener {

	private static final String TAG = "AirPlay";

	private Context mContext;

	private AirplayProxy mAirplayProxy;
	private AirplayController mController;
	private AirplayBroadcastFactory mBrocastFactory;

	private PopupMenu mAirplayPopup;
	private MenuItem mToggleAirplay;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = AirplayActivity.this;

		if (savedInstanceState != null) {

		}
		setContentView(R.layout.activity_airmain);

		initData();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// test
		mAirplayProxy.addDevice("test@99");
		mAirplayProxy.addDevice("test@88");

	}

	@Override
	protected void onPause() {
		super.onPause();

		mBrocastFactory.unRegisterListener();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mToggleAirplay = menu.findItem(R.id.menu_toggleAirplay);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_toggleAirplay:
			onToggleAirplayPopup(findViewById(R.id.menu_toggleAirplay));
			return true;

		case R.id.menu_toggleSetting:
			onToggleSettings();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void onToggleSettings() {
		mController.sendEvent(this, EventType.LAUNCH_SETTINGS);
	}

	private void onToggleAirplayPopup(View view) {
		final ArrayList<String> players = mAirplayProxy.getDeviceList();
		mAirplayPopup = AirplayPopup.createPopupMenu(mContext, view, players,
				this);

		mAirplayPopup.show();
	}

	@Override
	public void onAirplayDeviceChosen(String dev) {
		// TODO Auto-generated method stub
		Toast.makeText(mContext, "onAirplayDeviceChosen: " + dev,
				Toast.LENGTH_SHORT).show();

		updateToggleAirplayLabel(dev);

		// test
		mAirplayProxy.removeDevice("test@88");
		mAirplayProxy.removeDevice("test@77");
		// test
		AirplayBroadcastFactory.sendDeviceStateBroadcast(mContext, dev);

	}

	private void initData() {
		mAirplayProxy = AirplayProxy.getInstance(this);
		mController = AirplayController.getInstance(this);
		mBrocastFactory = new AirplayBroadcastFactory(this);
	}

	private void updateToggleAirplayLabel(String label) {
		if (mToggleAirplay != null) {
			mToggleAirplay.setTitle(label);
		}
	}

}
