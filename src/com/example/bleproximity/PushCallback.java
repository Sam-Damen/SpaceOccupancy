package com.example.bleproximity;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import android.content.ContextWrapper;
import android.util.Log;

public class PushCallback implements MqttCallback {
	
	private ContextWrapper context;
	
	//Constructor
	public PushCallback(ContextWrapper context) {
		
		this.context = context;
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// reconnect here

	}

	@Override
	public void deliveryComplete(MqttDeliveryToken arg0) {
		//No publishing so not needed

	}

	@Override
	public void messageArrived(MqttTopic arg0, MqttMessage msg)
			throws Exception {
		// Generate a toast notification when we receive a message
		Log.i("MQTT Sub", msg.toString());
		//Toast.makeText(this, msg.toString(), Toast.LENGTH_SHORT).show();

	}

}
