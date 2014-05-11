package com.example.bleproximity;

import android.annotation.SuppressLint;



/*
 * Simple type to define beacon address and rssi
 */
 @SuppressLint("DefaultLocale")
class BLEBeacon {
	
	private int mSignal;
	private String mAddress;
	private String mName;
	
	public BLEBeacon(String deviceAddr, String name,  int rssi) {
		mSignal = rssi;
		mAddress = deviceAddr;		
		mName = name;
	}
	
	public int getSignal() {
		return mSignal;
	}
	
	public String getAddress() {
		return mAddress;
	}
	
	public String getName() {
		return mName;
	}
	

	
	@Override
	public String toString() {
		return String.format("%s                             %ddBm", mAddress, mSignal);
		
	}
	
	
	
	
}