package com.example.bleproximity;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SpaceActivity extends Activity {
	
	//For debugging
	protected static final String TAG = "BeaconActivity";
	
	//MQTT Variables
    private MqttClient mqttClient;
    private static final String MQTT_HOST = "tcp://winter.ceit.uq.edu.au:1883";    
    private static final String MQTT_TOPIC = "uq/beaconTracker/space";	
	
	EditText mEdit;
	private String space;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_space);
		
		/*
		 * Get EditText
		 */
		mEdit = (EditText) findViewById(R.id.editText2);
		
		/*
		 * Get Button
		 */
		Button search = (Button) findViewById(R.id.button1);
		search.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//Get text from input
				//If null use own phoneID
				if(  mEdit.getText().toString().matches("") ) {
					space = "Laptop";
				} else {
					space = mEdit.getText().toString();
				}
				//Start MQTT Service to receive messages
				//startMQTTService();
				//Send MQTT Message
				new MQTTClass().execute(space);
			}
		});
		
		
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();

	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//Stop Mqtt communication
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	
	
	/*
	 * Private thread to perform Network Operations
	 */
	
	private class MQTTClass extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String... data) {
			
			String message;
			message =  data[0];
			
	        try {
	        	//may refuse due to same client id?
	            mqttClient = new MqttClient(MQTT_HOST, data[0], new MemoryPersistence());

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
