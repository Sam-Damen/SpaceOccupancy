package com.example.bleproximity;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;


public class BeaconCallBack implements MqttCallback {
	
	
	private ContextWrapper context;
	

	public BeaconCallBack(ContextWrapper context) {

	        this.context = context;
	}

	    @Override
	    public void connectionLost(Throwable cause) {
	        //We should reconnect here
	    }

	    @Override
	    public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {

	    	Log.i("BeaconActivity", "GOT MESSAGE");
	    	
	    	context.getSystemService(Context.NOTIFICATION_SERVICE);
	    }

	    @Override
	    public void deliveryComplete(MqttDeliveryToken token) {
	        //We do not need this because we do not publish
	    }
}


