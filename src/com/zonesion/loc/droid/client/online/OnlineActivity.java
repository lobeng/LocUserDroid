package com.zonesion.loc.droid.client.online;


import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.java_websocket.handshake.ServerHandshake;
//import org.java_websocket.util.Base64.InputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


import com.zonesion.loc.droid.client.utils.Connect;
import com.zonesion.loc.droid.client.utils.ConnectListener;
import com.zonesion.loc.droid.client.utils.WifiScan;
import com.zonesion.loc.droid.client.BuildActivity;
import com.zonesion.loc.droid.client.LocUserDroidActivity;
import com.zonesion.loc.droid.client.R;
import com.zonesion.loc.droid.map.MapView;
import com.zonesion.loc.droid.map.SuperImageView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class OnlineActivity  extends BuildActivity implements ConnectListener, WifiScan.OnWifiScanResultListener {
	
	private static final String TAG = "OnlineActivity";
	private static final String S_CONNECT = "<=>";
	private static final String S_DISCONNECT = "<x>";
	
	TextView mTVConnect;
	WifiScan mWifiScan;
	String mMyMACAddress;
	Connect mConnect;
	int mConnectStatus = 0;
	float mCurX = Float.NaN, mCurY = Float.NaN, mCurZ = Float.NaN;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE); 
        mTVConnect = (TextView) findViewById(R.id.tvConnect);
        mTVConnect.setText(S_DISCONNECT);
        
	  	WifiManager ws = (WifiManager) getSystemService(this.WIFI_SERVICE);
		mMyMACAddress = ws.getConnectionInfo().getMacAddress();
		mWifiScan = new WifiScan(ws);
		mWifiScan.setOnWifiScanResultListener(this);
		mWifiScan.setDaemon(true);
		mWifiScan.start();
		mWifiScan.scan(false);
		
		try { 
			URI uri = new URI( "ws://"+LocUserDroidActivity.sServer+":5001/online/"+mBuildingId);
			mConnect = new Connect(uri, this);
			mConnect.connect();
			mConnectStatus = 1;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	boolean savescan;
	public void onPause() {
		savescan = mWifiScan.isscan();
		if (savescan) {
			mWifiScan.scan(false);
		}
		super.onPause();
	}
	public void onResume() {
		super.onResume();
		if (savescan) {
			mWifiScan.scan(true);
		}
	}
	
	public void onDestroy() {
		if (mConnectStatus != 0) {
			mConnect.close();
		}
		mWifiScan.exit();
		super.onDestroy();
	}
	@Override
	public void onCurrentFloorChange() {
		mIVMap.setCurPoint(Float.NaN, Float.NaN);
	}
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, Menu.FIRST + 1, 5, "我的位置");
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	        case Menu.FIRST + 1:
	        	mIVMap.setCurPoint(mCurX, mCurY);
	        	if (Float.isNaN(mCurX) || Float.isNaN(mCurY) || Float.isNaN(mCurZ)) {
	        		Toast.makeText(getApplicationContext(), "很抱歉，暂时无法获取到您的位置！",
	        			     Toast.LENGTH_SHORT).show();
	        	} else {
	        		mIVMap.setCurPoint(mCurX, mCurY);
	        		this.setFloor((int)mCurZ);
	        	}
	        	break;
	        }
			return true;
	 }
	
	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		// TODO Auto-generated method stub
		Log.d(TAG, "connect onClose()");
		mConnectStatus = 0;
		//mTVConnect.setText(S_DISCONNECT);
		mHandler.obtainMessage(2, S_DISCONNECT).sendToTarget();
		mWifiScan.scan(false);
	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG, "connect onOpen()");
		mConnectStatus = 2;
		//mTVConnect.setText(S_CONNECT);
		mHandler.obtainMessage(2, S_CONNECT).sendToTarget();
		mWifiScan.scan(true);
	}

	@Override
	public void onMessage(String arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG, "connect onMessage() "+arg0);
		mHandler.obtainMessage(1, arg0).sendToTarget();
	}

	@Override
	public void onError(Exception arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG, "connect onError()");
		mConnectStatus = -1;
	}

	@Override
	public void onWifiScanResultChanged(List<ScanResult> sr) {
		// TODO Auto-generated method stub
		if (sr == null)
			return;
		StringBuffer rs = new StringBuffer();
		long tm = System.currentTimeMillis();
		rs.append("t=" + tm + ";");
		rs.append("id=" + mMyMACAddress + ";");
		rs.append(String.format("pos=NaN,NaN,NaN;"));
		rs.append("degree=NaN");
		for (ScanResult r : sr) {
			rs.append(String.format(";%s=%d,%d,2", r.BSSID,
					r.level, r.frequency));
		}
		rs.append("\n");
		if (mConnectStatus != 2) return;
		mConnect.send(rs.toString());
	}
	
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			if (m.what == 2) {
				mTVConnect.setText(m.obj.toString());	
			}
			if (m.what==1) {
				
				//TextView tv = (TextView) findViewById(R.id.tvInfo);
				//tv.setText(m.obj.toString());			
				String co = m.obj.toString();
				co = co.substring(1, co.length()-1);
				
				String[] v = co.split(",");
				mCurX = Float.parseFloat(v[0]);
				mCurY = Float.parseFloat(v[1]);
				mCurZ = Float.parseFloat(v[2]);
				if (mCurZ == mCurrentFloorId) {
					mIVMap.setCurPoint(mCurX, mCurY);
				} else mIVMap.setCurPoint(Float.NaN, Float.NaN);
				
			}
		}
	};
}
