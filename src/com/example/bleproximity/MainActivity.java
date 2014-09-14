package com.example.bleproximity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	//For debugging
	protected static final String TAG = "BeaconActivity";
	
	//BLE Adapter
	private BluetoothAdapter mBLEAdapter;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Use a cyclic progress to show scan is happening
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		setContentView(R.layout.activity_main);
		
		/*
		 * Get Buttons
		 */
		Button viewBeac = (Button) findViewById(R.id.button3);
		viewBeac.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getApplicationContext(), BeaconActivity.class);
				startActivity(intent);
			}
		});
		
		Button usr = (Button) findViewById(R.id.button2);
		usr.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getApplicationContext(), UsersActivity.class);
				startActivity(intent);
			}
		});	
		
		Button space = (Button) findViewById(R.id.button1);
		space.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getApplicationContext(), SpaceActivity.class);
				startActivity(intent);
			}
		});			
		
	
		/*
		 * Get the Bluetooth Adapter
		 */
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBLEAdapter = manager.getAdapter();
		
		/*
		 * Check Network Connectivity
		 */
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connMgr.getActiveNetworkInfo();
		
		if(info != null && info.isConnected()) {
			
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
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
	}
	
	
}
