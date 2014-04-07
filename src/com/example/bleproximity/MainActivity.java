package com.example.bleproximity;

import java.util.HashMap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
	
	protected static final String TAG = "BeaconActivity";
	private BluetoothAdapter mBLEAdapter;
	private HashMap<String, BLEBeacon> mBeacons;
	private BeaconListAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Use a cyclic progress to show scan is happening
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		
		/*
		 * Display beacons in list
		 */		
		ListView list = new ListView(this);
		//Custom adapter to show beacon attributes
		mAdapter = new BeaconListAdapter(this);
		list.setAdapter(mAdapter);
		setContentView(list);
		
		/*
		 * Get the Bluetooth Adapter
		 */
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBLEAdapter = manager.getAdapter();
		
		//Organises found Beacons by their address, only allow 1 instance of each beacon found
		mBeacons = new HashMap<String, BLEBeacon>();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * Check and ensure Bluetooth is enabled
		 */
		if (mBLEAdapter == null || ! mBLEAdapter.isEnabled()) {
			Intent enBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enBluetoothIntent);
			finish();
			return;
		}
		
		/*
		 * Check BLE is supported
		 */
		if (! getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			//Make a toast to tell no BLE support
			finish();
			return;
		}
		
		//Begin a Scan for Devices
		startScan();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//Stop scanning
		mHandler.removeCallbacks(mStopRun);
		mHandler.removeCallbacks(mStartRun);
		mBLEAdapter.stopLeScan(this);
	}
	
	private Runnable mStopRun = new Runnable() {
		@Override
		public void run() {
			stopScan();
		}
	};
	
	private Runnable mStartRun = new Runnable() {
		@Override
		public void run() {
			startScan();
		}
	};
	
	/*
	 * These two functions alternate automatically the scan on/off
	 */
	private void stopScan() {
		mBLEAdapter.stopLeScan(this);
		setProgressBarIndeterminateVisibility(false);
		//Turn off scan for 5 sec
		mHandler.postDelayed(mStartRun, 5000);
	}
	
	private void startScan() {
		//Scan for all periherals (can use UUID [] to only find certain)
		mBLEAdapter.startLeScan(this);
		setProgressBarIndeterminateVisibility(true);
		//Turn on scan for 5 sec
		mHandler.postDelayed(mStopRun, 5000);
	}	
	
	
	/*
	 * LE Scan Callback
	 */
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte [] scanRecord) {
		Log.i(TAG, "New BLE Device: " + device.getName() + " @ " + rssi);
		
		//Create a new beacon and then pass it to handler to update map
		BLEBeacon beacon = new BLEBeacon(device.getAddress(), rssi);		
		mHandler.sendMessage(Message.obtain(null, 0, beacon));
		
		//scanRecord holds raw data if needed
	}
	
	
	/*
	 * Handler to pass scan results to main to update list view
	 */
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			BLEBeacon beacon = (BLEBeacon) msg.obj;
			mBeacons.put(beacon.getAddress(), beacon);
			
			//Clear then put new information into mAdapter list to display
			mAdapter.setNotifyOnChange(false);
			mAdapter.clear();
			mAdapter.addAll(mBeacons.values());
			mAdapter.notifyDataSetChanged();
			
		}
	};
	
	
	
	/*
	 * Custom List/ array adapter to display the found Beacons
	 */
	private static class BeaconListAdapter extends ArrayAdapter<BLEBeacon> {
		
		public BeaconListAdapter(Context context) {
			super(context, 0);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = LayoutInflater.from(getContext())
						.inflate(R.layout.item_beacon_list, parent, false);
			}
			
			BLEBeacon beacon = getItem(position);
			
			TextView addressView = (TextView) convertView.findViewById(R.id.text_address);
			addressView.setText(beacon.getAddress());
			
			TextView rssiView = (TextView) convertView.findViewById(R.id.text_rssi);
			rssiView.setText(String.format("%ddBm", beacon.getSignal()));
			
			return convertView;			
		}		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
