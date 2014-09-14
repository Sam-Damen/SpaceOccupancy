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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class BeaconActivity extends Activity implements BluetoothAdapter.LeScanCallback{
	
	//For debugging
	protected static final String TAG = "BeaconActivity";	
	
	//BLE variables
	private BluetoothAdapter mBLEAdapter;
	private HashMap<String, BLEBeacon> mBeacons;	
	private ArrayAdapter<String> mAdapter;
	private static final String DEVICE_ID_FORMAT = "ID_%s";
	private int autoScan = 1;
	
	//MQTT Variables
    private MqttClient mqttClient;
    private String mDeviceID;    
    private static final String MQTT_HOST = "tcp://winter.ceit.uq.edu.au:1883";    
    private static final String MQTT_TOPIC = "uq/beaconTracker/raw";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Use a cyclic progress to show scan is happening
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		setContentView(R.layout.activity_beacons);
				
		//Keep Track of found Beacons by their address, only allow 1 instance of each beacon found
		mBeacons = new HashMap<String, BLEBeacon>();
		
		/*
		 * Get the Bluetooth Adapter
		 */
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBLEAdapter = manager.getAdapter();
		
		
		/*
		 * Display beacons in list
		 */
		
		ListView list = (ListView) findViewById(R.id.listview);
		mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		list.setAdapter(mAdapter);
		
		mDeviceID = String.format(DEVICE_ID_FORMAT, Secure.getString(getContentResolver(), Secure.ANDROID_ID));
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
		
		if(autoScan == 1) {
			//Turn off scan for 5 sec
			mHandler.postDelayed(mStartRun, 5000);
		}
	}
	
	private void startScan() {
		//Start a Scan for LE devices
		if(autoScan == 1) {
			mBLEAdapter.startLeScan(this);
			setProgressBarIndeterminateVisibility(true);
			//Auto Publish Data
			for (BLEBeacon beacon : mBeacons.values()) {
				new MQTTClass().execute(bytesToHex(beacon.getData(), beacon.getSignal()));
			}
			//Turn on scan for 5 sec
			mHandler.postDelayed(mStopRun, 5000);
		}
		
	}	
	
	
	/*
	 * LE Scan Callback
	 * Finds and stores Data for all beacons
	 */
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte [] scanRecord) {
		Log.i(TAG, "New BLE Device: " + device.getName() + " @ " + rssi);
		
		if (parseUuids(scanRecord)) {
			//Create a new beacon and then pass it to handler to update map
			BLEBeacon beacon = new BLEBeacon(device.getAddress(),device.getName(), rssi, scanRecord);		
			mHandler.sendMessage(Message.obtain(null, 0, beacon));
		}
	}
	
	/*
	 * Filter out all BLE devices except iBeacon
	 */
	private boolean parseUuids(byte[] scanData) {
		int startByte = 2;
		boolean iBeaconFound = false;
		while (startByte <= 5) {
			if (((scanData[(startByte + 2)] & 0xFF) == 2) && ((scanData[(startByte + 3)] & 0xFF) == 21)) {
				iBeaconFound = true;
				break;
			}
			//Add in other beacons that are acceptable, estimote?
			startByte++;
		}
		
		if (iBeaconFound) {
			return true;
		} else {
			return false;
		}
	 }
		
	
	/*
	 * Helper to parse out ble data
	 */
	private static String bytesToHex(byte[] data, int rssi) {
		
		StringBuilder sb = new StringBuilder();
		//Length of first advertising information
		int adLength = data[0];
		//Length of second advertising information
		int ad2Length;
		int i;
		
		//Find Length of BLE packet
		for (i = 0; i < adLength + 1; i++) {
			sb.append(String.format("%02X ", data[i]));
		}
		
		//Get length of rest of packet
		ad2Length = data[i];
		
		//Move one position over, account for the total length and 2 bytes skipped
		for (i = i + 0; i < ad2Length + adLength + 2; i++) {
			sb.append(String.format("%02X ", data[i]));
		}
		
		//Append the RSSI also
		sb.append(String.format("%d", rssi));

		return sb.toString();
	}
	
	
	
	/*
	 * Handler to pass scan results to main to update list view
	 */
	private Handler mHandler = new Handler() {
		
		//private String sBeacon;
		
		@Override
		public void handleMessage(Message msg) {
			BLEBeacon beacon = (BLEBeacon) msg.obj;
			
			//Store Beacon data (keep only 1 copy of each)
			mBeacons.put(beacon.getAddress(), beacon);
			
			mAdapter.setNotifyOnChange(false);
			mAdapter.clear();
						
			//For all the addresses in the Hashmap
			for (String addr : mBeacons.keySet()) {
				mAdapter.add(mBeacons.get(addr).toString());				
			}			
			mAdapter.notifyDataSetChanged();
			
		}
	};
	
	
	/*
	 * Private thread to perform Network Operations
	 */
	
	private class MQTTClass extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String... data) {
			
			String message;
			message = mDeviceID  + ":" + data[0];
			
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
	 * Handle Button Presses
	 */
	public void manualScan(View view) {
		
		if (autoScan == 1) {
			//First turn off autoscanning
			Toast.makeText(this, "Auto Mode OFF", Toast.LENGTH_SHORT).show();
			autoScan = 0;
			setProgressBarIndeterminateVisibility(false);
		}		
		
		mBLEAdapter.startLeScan(this);
		setProgressBarIndeterminateVisibility(true);
		//Turn on scan for 5 sec
		mHandler.postDelayed(mStopRun, 5000);			
	}
	
	public void publishData(View view) {
		
		//Send the data to the MQTT task
		for (BLEBeacon beacon : mBeacons.values()) {
			new MQTTClass().execute(bytesToHex(beacon.getData(), beacon.getSignal()));
		}
	}
	
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.beaconlist, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.

		
		switch (item.getItemId()) {
		
		
		case R.id.clear_beacons:
			//Clear out mBeacons hashMap
			mBeacons.clear();
			//Update the view
			mAdapter.clear();
			mAdapter.notifyDataSetChanged();
			return true;
			
		
		case R.id.auto_scan:
			
			//Toggle autoScan functionality
			autoScan ^= 1;
			
			if (autoScan == 1) {
				Toast.makeText(this, "Auto Mode ON", Toast.LENGTH_SHORT).show();
				startScan();
			} else {
				Toast.makeText(this, "Auto Mode OFF", Toast.LENGTH_SHORT).show();
				stopScan();
			}
			
			
		default:
			return super.onOptionsItemSelected(item);
		
		}		
	}
}


			
	
