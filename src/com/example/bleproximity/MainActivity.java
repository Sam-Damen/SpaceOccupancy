package com.example.bleproximity;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
	
	//For debugging
	protected static final String TAG = "BeaconActivity";
	
	
	//BLE variables
	private BluetoothAdapter mBLEAdapter;
	private HashMap<String, BLEBeacon> mBeacons;
	private BeaconListAdapter mAdapter;
	
	//MQTT Variables
    private MqttClient mqttClient;
    private String mDeviceID;
    
    private static final String MQTT_HOST = "tcp://test.mosquitto.org:1883";
    //private static final String MQTT_HOST = "tcp://broker.mqttdashboard.com:8000";
    private static final String DEVICE_ID_FORMAT = "andr_%s";
    private static final String MQTT_TOPIC = "uq/beaconTracker/raw";

	
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
		list.setOnItemClickListener(beaconClickedHandler);
		setContentView(list);
	
	
		/*
		 * Get the Bluetooth Adapter
		 */
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBLEAdapter = manager.getAdapter();
		
		//Organises found Beacons by their address, only allow 1 instance of each beacon found
		mBeacons = new HashMap<String, BLEBeacon>();
		
		/*
		 * Check Network Connectivity
		 */
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connMgr.getActiveNetworkInfo();
		
		if(info != null && info.isConnected()) {
			//Get Device ID for MQTT client ID
			
			mDeviceID = String.format(DEVICE_ID_FORMAT, 
					Secure.getString(getContentResolver(), Secure.ANDROID_ID));
			
		} else {
			Toast.makeText(this, "No Network Connection", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		

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
			Toast.makeText(this, "No BLE Support", Toast.LENGTH_SHORT).show();
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
		
		//scanRecord holds the raw beacon data
		new MQTTClass().execute(bytesToHex(scanRecord));
		
	}
	
	/*
	 * Helper to parse out ble data
	 */
	private static String bytesToHex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b: data) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
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
	 * Handle Clicks of the List of Beacons
	 */
	private OnItemClickListener beaconClickedHandler = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Log.i(TAG, "List Data: "+ parent.getItemAtPosition(position));
			

		}
	};
	
	
	/*
	 * Private thread to perform Network Operations
	 */
	
	private class MQTTClass extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String... data) {
			
			String message;
			message = mDeviceID + "-" + data[0];
			
	        try {
	            mqttClient = new MqttClient(MQTT_HOST, mDeviceID, new MemoryPersistence());

	            mqttClient.connect();
	            
	            
	            //Publish Raw BLE Data + Phone ID to MQTT Broker
	            MqttTopic topic = mqttClient.getTopic(MQTT_TOPIC);
	            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
	            topic.publish(mqttMessage);
	            
	            return null;

	        } catch (MqttException e) {
	        	e.printStackTrace();
	        	return null;
	        }			
		}
	}	
	
	
	/*
	 * Custom List/array adapter to display the found Beacons
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

		
		switch (item.getItemId()) {
		
		case R.id.action_settings:

			return true;
			
		case R.id.clear_beacons:
			
			
			//Clear the entire HashMap of beacon data
			mBeacons.clear();
			//Update the view
			mAdapter.clear();
			mAdapter.addAll(mBeacons.values());
			mAdapter.notifyDataSetChanged();

			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		
		}		
	}

}
