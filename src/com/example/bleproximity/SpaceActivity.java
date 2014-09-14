package com.example.bleproximity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class SpaceActivity extends Activity {
	
	//For debugging
	protected static final String TAG = "BeaconActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_users);

	}
	
	
	@Override
	protected void onResume() {
		super.onResume();


	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	
	
	/*
	 * Private thread to perform Network Operations
	 */
/*	
	private class MQTTClass extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String... data) {
			
			String message;
			message = mDeviceID + Integer.toString(beaconRssi) + ":" + data[0];
			
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
*/	
	
	/*
	 * Handle Button Presses
	 */
	
	
	
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
			
		default:
			return super.onOptionsItemSelected(item);
		
		}		
	}

}
