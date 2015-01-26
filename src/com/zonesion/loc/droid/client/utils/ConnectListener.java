package com.zonesion.loc.droid.client.utils;

import org.java_websocket.handshake.ServerHandshake;

public interface ConnectListener {
	public void onClose(int arg0, String arg1, boolean arg2) ;
	public void onOpen(ServerHandshake arg0);
	public void onMessage(String arg0);
	public void onError(Exception arg0);
}
