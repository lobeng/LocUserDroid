package com.zonesion.loc.droid.client.utils;

import java.util.List;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

public class WifiScan extends Thread {

	private static final String TAG = "WifiScan";
	private boolean mRunning = true;

	WifiManager mWifiManager;

	public WifiScan(WifiManager wifiManager) {
		super();

		mWifiManager = wifiManager;
	}

	public final void exit() {
		mRunning = false;
	}

	public interface OnWifiScanResultListener {
		public void onWifiScanResultChanged(List<ScanResult> sr);
	}

	private OnWifiScanResultListener mOnWifiScanResultListener;

	public void setOnWifiScanResultListener(OnWifiScanResultListener li) {
		mOnWifiScanResultListener = li;
	}

	private boolean mScan = false;

	public void scan(boolean f) {
		mScan = f;
	}
	public boolean isscan() {
		return mScan;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Log.d(TAG, "start....");
		int scan = 0;
		List<ScanResult> lastR = null;
		while (mRunning) {
			while (mRunning && !mScan) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
			if (!mRunning)
				break;
			scan++;
			long tm = System.currentTimeMillis();
			lastR = mWifiManager.getScanResults();
			List<ScanResult> scanResults;
			do {
				mWifiManager.startScan();
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {

				}
				scanResults = mWifiManager.getScanResults();
			} while (mRunning
					&& lastR.toString().equals(scanResults.toString()));
			if (!mRunning)
				break;

			lastR = scanResults;
			if (mOnWifiScanResultListener != null) {
				mOnWifiScanResultListener.onWifiScanResultChanged(scanResults);
			}
			if (scanResults != null && scanResults.size() != 0) {
				/*
				 * mTracOut.append("t="+tm+";");
				 * mTracOut.append("id="+mWifiManager
				 * .getConnectionInfo().getMacAddress()+";");
				 * mTracOut.append(String.format("pos=NaN,NaN,NaN;"));
				 * mTracOut.append("degree=NaN");
				 * 
				 * for (ScanResult r : scanResults) {
				 * 
				 * 
				 * mTracOut.append(String.format(";%s=%d,%d,2", r.BSSID,
				 * r.level, r.frequency)); } mTracOut.append("\n");
				 */
			}
		}
		Log.d(TAG, "exit....");
	}
}