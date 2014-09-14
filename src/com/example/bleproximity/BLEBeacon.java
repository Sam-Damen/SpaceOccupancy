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
	private byte[] mData;
	
	public BLEBeacon(String deviceAddr, String name,  int rssi, byte[] data) {
		mSignal = rssi;
		mAddress = deviceAddr;		
		mName = name;
		mData = data;
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
	
	public byte[] getData() {
		return mData;
	}
	

	
	@Override
	public String toString() {
		return String.format("%s%ddBm", mAddress, mSignal);
		
	}
	
	
	
	
}