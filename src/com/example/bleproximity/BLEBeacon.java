package com.example.bleproximity;

import java.util.Locale;


/*
 * Simple type to define beacon address and rssi
 */
class BLEBeacon {
	
	private int mSignal;
	private String mAddress;
	
	public BLEBeacon(String deviceAddr, int rssi) {
		mSignal = rssi;
		mAddress = deviceAddr;		
	}
	
	public int getSignal() {
		return mSignal;
	}
	
	public String getAddress() {
		return mAddress;
	}
	

	
	@Override
	public String toString() {
		return String.format("%s %ddBm", mAddress, mSignal);
		
	}
	
	
	
	
}