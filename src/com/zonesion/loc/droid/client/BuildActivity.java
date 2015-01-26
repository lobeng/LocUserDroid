package com.zonesion.loc.droid.client;
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
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class BuildActivity  extends Activity {
	
	private static final String TAG = "BuildActivity";
	
	//WifiScan mWifiScan;
	//String mMyMACAddress;
	//Connect mConnect;
	
	//int mConnectStatus = 0;
	protected int mCurrentFloorId = -1;
	
	protected String mBuildingId;
	protected String mBuildingName;
	
	protected TextView mTVTitleLeft;
	protected TextView mTVTitleCenter;
	protected MapView mIVMap;

	protected class Floor {
		public int mId;
		public String mName;
		public String mMapFile;
		public Bitmap mMap;
	}
	protected HashMap<Integer, Floor> mFloors = new HashMap<Integer, Floor> ();
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
     
        setContentView(R.layout.map);
     
        mTVTitleLeft = (TextView)findViewById(R.id.tvTitleLeft); 
        mTVTitleCenter = (TextView)findViewById(R.id.tvTitleCenter);
        mIVMap = (MapView) findViewById(R.id.ivMap);
        //mIVMap.setCurPoint(100, 100);
        String bcfg = this.getIntent().getStringExtra("building");
        System.out.println(bcfg);
        try {
			JSONObject x = new JSONObject(bcfg);
			mBuildingId = x.get("id").toString();
			mBuildingName = x.get("name").toString();
			mTVTitleCenter.setText(mBuildingName);
			
			JSONArray floors = x.getJSONArray("floors");
			for (int i=0; i<floors.length(); i++) {
				JSONObject f = (JSONObject) floors.opt(i);
				Floor fr = new Floor();
				fr.mId = f.getInt("floor");
				fr.mName = f.getString("name");
				fr.mMapFile = f.getString("map");
				mFloors.put(fr.mId, fr);
			}
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
		for (int fid : mFloors.keySet()) {
			HttpGetFloorMapTask task = new HttpGetFloorMapTask();  
			task.execute(mFloors.get(fid));
		}
		
		mTVTitleLeft.setOnClickListener(onTitleClick);
		mTVTitleCenter.setOnClickListener(onTitleClick);
	}
	View.OnClickListener onTitleClick = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			String[] items = new String[mFloors.keySet().size()];
			final int[] itkey = new int[mFloors.keySet().size()];
			int i=0;
			for (int k : mFloors.keySet()) {
				items[i] = mFloors.get(k).mName;
				itkey[i] = k;
				i++;
			}                    
			AlertDialog dlg = new AlertDialog.Builder(BuildActivity.this)  
            .setTitle(mBuildingName)  
            .setIcon(android.R.drawable.ic_dialog_alert)  
            .setItems(items, new DialogInterface.OnClickListener() {  

                @Override 
                public void onClick(DialogInterface dialog, int which) {  
                  //  DialogExercise.this.mSelectedItem = which;
                	int key = itkey[which];
                	if (mCurrentFloorId != key) {
                		mTVTitleLeft.setText(mFloors.get(key).mName);
                		mIVMap.setImageBitmap(mFloors.get(key).mMap);
                		mCurrentFloorId = key;
                		onCurrentFloorChange();
                	}
                }  
            }).create();
			dlg.show();
		}
	};
	
	public void onDestroy() {
		super.onDestroy();
	}
	
	
	public void setFloor(int fid) {
		if (mFloors.containsKey(fid)) {
			mTVTitleLeft.setText(mFloors.get(fid).mName);
			mIVMap.setImageBitmap(mFloors.get(fid).mMap);
			mCurrentFloorId = fid;
		}
	}
	public void onCurrentFloorChange() {
		
	}
	class HttpGetFloorMapTask extends AsyncTask<Floor,Integer,Bitmap> 
	{
		Floor mfloor;
		@Override  
	    protected void onPostExecute(Bitmap res) {
			if (res != null) {
				mfloor.mMap = res;
				if (mCurrentFloorId == -1){
					mCurrentFloorId = mfloor.mId;
					mTVTitleLeft.setText(mfloor.mName);
					mIVMap.setImageBitmap(res);
					onCurrentFloorChange();
				}
			}
	    }  
		@Override
		protected Bitmap doInBackground(Floor... arg0) {
			// TODO Auto-generated method stub
			 String result = null;
		     BufferedReader reader = null;
		     mfloor = arg0[0];
		     try {
		    	 String s = "http://"+LocUserDroidActivity.sServer+":8080/static/"+mBuildingId+"/"+arg0[0].mMapFile;
		    	  URL url = new URL(s);  
		            HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
		            //conn.setConnectTimeout(5000);  
		            conn.setRequestMethod("GET");  
		            conn.setDoInput(true);  
		            if (conn.getResponseCode() == 200) {  
		
		                InputStream is = conn.getInputStream();
		                Bitmap x = BitmapFactory.decodeStream(is);
		                is.close();
		         
		                return x;
		              
		            }  
		        } catch (Exception e) {
		            e.printStackTrace();
		            return null;
		        } 
		        return null;
		}
		
	}
}

