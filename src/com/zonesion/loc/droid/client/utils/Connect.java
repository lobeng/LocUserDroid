package com.zonesion.loc.droid.client.utils;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;


import android.util.Log;

public class Connect extends WebSocketClient {
	private static final String TAG = "Connect";
	
	private ConnectListener mConnectListener;
	
	public Connect(URI serverURI, ConnectListener li) {
		super(serverURI);
		// TODO Auto-generated constructor stub
		mConnectListener = li;
	}
	
	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		// TODO Auto-generated method stub
		mConnectListener.onClose(arg0, arg1, arg2);
	}

	@Override
	public void onError(Exception arg0) {
		// TODO Auto-generated method stub
		
		mConnectListener.onError(arg0);
		
	}

	@Override
	public void onMessage(String arg0) {
		// TODO Auto-generated method stub
		
		mConnectListener.onMessage(arg0);
		
	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		// TODO Auto-generated method stub
		
		mConnectListener.onOpen(arg0);
		
	}
}
