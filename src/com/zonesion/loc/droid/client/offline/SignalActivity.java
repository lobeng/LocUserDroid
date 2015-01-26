package com.zonesion.loc.droid.client.offline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.zonesion.loc.droid.client.R;
import com.zonesion.loc.droid.client.utils.WifiScan;
import com.zonesion.loc.droid.client.utils.WifiScan.OnWifiScanResultListener;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class SignalActivity extends Activity implements OnWifiScanResultListener {

	WifiScan mWifiScan;
	HistogramView mHistogramView;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 
		requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
		 getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		 WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
		 
		this.setContentView(R.layout.signal);
		
		mHistogramView = (HistogramView) findViewById(R.id.hv);
		
		WifiManager ws = (WifiManager) getSystemService(this.WIFI_SERVICE);
		//mMyMACAddress = ws.getConnectionInfo().getMacAddress();
		mWifiScan = new WifiScan(ws);
		mWifiScan.setOnWifiScanResultListener(this);
		mWifiScan.setDaemon(true);
		mWifiScan.start();
		mWifiScan.scan(true);
	}
	
	@Override
	protected void onResume() {
	 /**
	  * 设置为横屏
	  */
	 if(getRequestedOrientation()!=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
	  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	 }

	 super.onResume();
	}
	
	public void onDestroy() {
		mWifiScan.scan(false);
		mWifiScan.exit();
		super.onDestroy();
	}

	HashMap<String, ArrayList<Integer>> mSignals = new HashMap<String, ArrayList<Integer>>();
	@Override
	public void onWifiScanResultChanged(List<ScanResult> sr) {
		// TODO Auto-generated method stub
		mHistogramView.addScanResult(sr);
		if (false) {
		for (ScanResult s : sr) {
			/*ArrayList<Integer> ai;
			if (!mSignals.containsKey(s.BSSID)) {
				ai = new ArrayList<Integer>();
				mSignals.put(s.BSSID, ai);
			} else {
				ai = mSignals.get(s.BSSID);
			}
			ai.add(s.level);
			*/
			mHistogramView.addAps(s.BSSID, s.level);
		}
		}
		mHistogramView.postInvalidate();
	}
	
}
