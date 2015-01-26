package com.zonesion.loc.droid.client.offline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;

import org.java_websocket.handshake.ServerHandshake;
import org.pi4.locutil2.GeoPosition;

import com.zonesion.loc.droid.client.BuildActivity;
import com.zonesion.loc.droid.client.LocUserDroidActivity;
import com.zonesion.loc.droid.client.R;
import com.zonesion.loc.droid.client.online.OnlineActivity;
import com.zonesion.loc.droid.client.utils.Connect;
import com.zonesion.loc.droid.client.utils.ConnectListener;
import com.zonesion.loc.droid.client.utils.WifiScan;
import com.zonesion.loc.droid.map.SuperImageView.OnDoubleClickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class OfflineActivity extends BuildActivity implements ConnectListener,
		WifiScan.OnWifiScanResultListener {

	private static final String TAG = "OnlineActivity";
	private static final String S_CONNECT = "<=>";
	private static final String S_DISCONNECT = "<x>";

	private static int sMaxSamples = 25;
	// Button mBTCollect;
	TextView mTVConnect;
	WifiScan mWifiScan;
	String mMyMACAddress;
	Connect mConnect;
	int mConnectStatus = 0;

	FileOutputStream mFileOutputStream;
	OutputStreamWriter mOutputStreamWriter;

	SQLiteDatabase mSQLiteDatabase;
	 
	
	float mCurrentX = Float.NaN, mCurrentY = Float.NaN;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		// mBTCollect = (Button) findViewById(R.id.btTitleRightButton);
		// mBTCollect.setVisibility(View.VISIBLE);
		mTVConnect = (TextView) findViewById(R.id.tvConnect);
		mTVConnect.setText(S_DISCONNECT);
		WifiManager ws = (WifiManager) getSystemService(this.WIFI_SERVICE);
		mMyMACAddress = ws.getConnectionInfo().getMacAddress();
		mWifiScan = new WifiScan(ws);
		mWifiScan.setOnWifiScanResultListener(this);
		mWifiScan.setDaemon(true);
		mWifiScan.start();
		mWifiScan.scan(false);

		mIVMap.setOnDoubleClickListener(new OnDoubleClickListener() {
			@Override
			public void onDoubleClick(float x, float y) {
				// TODO Auto-generated method stub
				// System.out.println("onDoubleClick("+x+","+y+")");
				if (mCollectNumber >= 0) {
					Toast.makeText(getApplicationContext(), "正在采集，请稍后",
							Toast.LENGTH_SHORT).show();
					return;
				}
				Matrix m = new Matrix();
				if (mIVMap.getImageMatrix().invert(m)) {
					float[] p = new float[2];
					p[0] = x;
					p[1] = y;
					m.mapPoints(p);
					// System.out.println("invert("+p[0]+","+p[1]+")");
					mCurrentX = p[0];
					mCurrentY = p[1];
					mIVMap.setCurPoint(p[0], p[1]);

					// mIVMap.getImageMatrix().mapPoints(p);
					// System.out.println("xxxxx("+p[0]+","+p[1]+")");
				}
			}
		});

		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Toast.makeText(getApplicationContext(),
					"警告，没有检测到SD卡，采集数据将不能保存到本地！", Toast.LENGTH_SHORT).show();
			return;
		}
		String mSaveDir = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/wifi-loc/offline";
		File f = new File(mSaveDir);
		if (!f.isDirectory()) {
			f.mkdirs();
		}
		String sf = mSaveDir + "/" + mBuildingId + ".trace";
		f = new File(sf);
		try {
			mFileOutputStream = new FileOutputStream(sf, true);
			mOutputStreamWriter = new OutputStreamWriter(mFileOutputStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String dbf = mSaveDir + "/" + mBuildingId + ".db";
		mSQLiteDatabase = openOrCreateDatabase(dbf, Context.MODE_PRIVATE, null);
		
		boolean create_table = true;
		String sql = "select count(*) as c from sqlite_master where type ='table' and name ='trace' ";
        Cursor cursor = mSQLiteDatabase.rawQuery(sql, null);
        if(cursor.moveToNext()){
        	 int count = cursor.getInt(0);
             if(count>0){
            	 create_table = false;
             }
        }
        cursor.close();
		if (create_table) {
			sql = "CREATE TABLE trace (_key INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"data VARCHAR, upload INTEGER, t VARCHAR, pos VARCHAR)";
			mSQLiteDatabase.execSQL(sql); 
		}
		
		sql = "select distinct pos from trace;";
		Cursor cr = mSQLiteDatabase.rawQuery(sql, null);
		while (cr.moveToNext()) {
			String co = cr.getString(0);
			co = co.substring(1, co.length() - 1);

			String[] v = co.split(",");
			GeoPosition p = new GeoPosition(Float.parseFloat(v[0]),
					Float.parseFloat(v[1]), Float.parseFloat(v[2]));
			mPositions.add(p);
		}
		
		mColDialog = new ProgressDialog(OfflineActivity.this);
		mColDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);  
		mColDialog.setTitle("采集信号");
		mColDialog.setIndeterminate(false); 
		mColDialog.setCancelable(false);
		
		try {
			URI uri = new URI("ws://" + LocUserDroidActivity.sServer
					+ ":5001/offline/" + mBuildingId);
			mConnect = new Connect(uri, this);
			mConnect.connect();
			mConnectStatus = 1;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	ProgressDialog mColDialog;
	private void startCollect() {
		if (mCollectNumber >= 0) {
			Toast.makeText(getApplicationContext(), "正在采集，请稍后",
					Toast.LENGTH_SHORT).show();
			return;
		}

		if (Float.isNaN(mCurrentX) || Float.isNaN(mCurrentY)
				|| mCurrentFloorId < 0) {
			Toast.makeText(getApplicationContext(), "请双击屏幕，设置采集点",
					Toast.LENGTH_SHORT).show();
			return;
		}
		
		mColDialog.setMax(sMaxSamples);
		mColDialog.setProgress(0);

		mColDialog.show();
		
		mCollectNumber = 0;
		mWifiScan.scan(true);
	}
 
	private void upload() {
		if (mConnectStatus != 2) {
			Toast.makeText(getApplicationContext(), "与服务器连接断开，请检查网络设置！",
					Toast.LENGTH_SHORT).show();	
			return;
		}
		ProgressDialog xh_pDialog = new ProgressDialog(OfflineActivity.this);
		xh_pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);  
		xh_pDialog.setTitle("上传进度");
		xh_pDialog.setIndeterminate(false); 
		xh_pDialog.setCancelable(false);
		xh_pDialog.show();  
		UploadThread ut = new UploadThread(xh_pDialog);
		ut.execute(null);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, Menu.FIRST + 1, 5, "采集");
		menu.add(Menu.NONE, Menu.FIRST + 2, 5, "上传");
		menu.add(Menu.NONE, Menu.FIRST + 3, 5, "实时热点");
		// menu.add(Menu.NONE, Menu.FIRST + 2, 5, "设置");//.setIcon(
		// android.R.drawable.ic_menu_edit);
		// menu.add(Menu.NONE, Menu.FIRST + 3, 5, "关于");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST + 1:
			startCollect();
			break;
		case Menu.FIRST + 2:
			upload();
			break;
		case Menu.FIRST + 3:
			Intent it = new Intent(this, SignalActivity.class);
			startActivity(it);
			break;
		}
		return true;
	}

	int mCollectNumber = -1;

	public void onDestroy() {
		if (mConnectStatus != 0) {
			mConnect.close();
		}
		mWifiScan.exit();
		try {
			if (mSQLiteDatabase != null) {
				mSQLiteDatabase.close();
			}
			if (mOutputStreamWriter != null) {
				mOutputStreamWriter.close();
			}
			if (mFileOutputStream != null) {
				mFileOutputStream.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		super.onDestroy();
	}

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		// TODO Auto-generated method stub
		Log.d(TAG, "connect onClose()");
		mConnectStatus = 0;
		// mTVConnect.setText(S_DISCONNECT);
		mHandler.obtainMessage(2, S_DISCONNECT).sendToTarget();
	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG, "connect onOpen()");
		mConnectStatus = 2;
		// mTVConnect.setText(S_CONNECT);
		mHandler.obtainMessage(2, S_CONNECT).sendToTarget();
	}

	@Override
	public void onMessage(String arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG, "connect onMessage() " + arg0);
		if (arg0.startsWith("t=")) {
			String t = arg0.substring(2);
			//String sql = "DELETE FROM trace WHERE t='"+t+"'";
			String sql = "UPDATE trace SET upload=1 WHERE t='"+t+"'";
			mSQLiteDatabase.execSQL(sql);
		} else
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
		rs.append(String.format("pos=%f,%f,%d;", mCurrentX, mCurrentY,
				mCurrentFloorId));
		rs.append("degree=NaN");
		for (ScanResult r : sr) {
			rs.append(String.format(";%s=%d,%d,2", r.BSSID, r.level,
					r.frequency));
		}
		rs.append("\n");

		try {
			if (mOutputStreamWriter != null) {
				mOutputStreamWriter.write(rs.toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		GeoPosition pos = new GeoPosition(mCurrentX, mCurrentY, mCurrentFloorId);
		if (true || mConnectStatus != 2) {
			mSQLiteDatabase.execSQL("INSERT INTO trace VALUES (NULL, ?, ?,?,?)", 
					new Object[]{rs.toString(), 0, ""+tm, pos.toString()});			
		} else {
			mConnect.send(rs.toString()); 
		}
		mCollectNumber++;
		mColDialog.setProgress(mCollectNumber);
		if (mCollectNumber >= sMaxSamples) {
			mWifiScan.scan(false);
			mCollectNumber = -1;
			mPositions.add(pos);
			mIVMap.addPosition(mCurrentX, mCurrentY);
			mCurrentX = mCurrentY = Float.NaN;
			mIVMap.setCurPoint(mCurrentX, mCurrentY);
			mColDialog.dismiss();
			mColDialog.setProgress(0);
		}
	}

	HashSet<GeoPosition> mPositions = new HashSet<GeoPosition>();

	@Override
	public void onCurrentFloorChange() {
		mCurrentX = Float.NaN;
		mCurrentY = Float.NaN;
		mIVMap.setCurPoint(Float.NaN, Float.NaN);
		mIVMap.clrPosition();
		for (GeoPosition g : mPositions) {
			if (g.getZ() == mCurrentFloorId) {
				mIVMap.addPosition((float) g.getX(), (float) g.getY());
			}
		}
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			if (m.what == 2) {
				mTVConnect.setText(m.obj.toString());
			}
			if (m.what == 1) {
				String co = m.obj.toString();
				co = co.substring(1, co.length() - 1);

				String[] v = co.split(",");
				GeoPosition p = new GeoPosition(Float.parseFloat(v[0]),
						Float.parseFloat(v[1]), Float.parseFloat(v[2]));
				mPositions.add(p);
				// mIVMap.setCurPoint(Float.parseFloat(v[0]),
				// Float.parseFloat(v[1]));
				if (Float.parseFloat(v[2]) == mCurrentFloorId)
					mIVMap.addPosition(Float.parseFloat(v[0]),
							Float.parseFloat(v[1]));
			}
		}
	};
	
	class UploadThread extends AsyncTask<String, String, Integer>{

		ProgressDialog mDlg;
		public UploadThread(ProgressDialog dlg) {
			super();
			mDlg = dlg;
		}
		@Override  
	    protected void onPostExecute(Integer res) {
			mDlg.dismiss();
			if (res < 0) {
				Toast.makeText(getApplicationContext(), "没有数据需要上传！",
						Toast.LENGTH_SHORT).show();
			} else
			if (res < 1) {
				Toast.makeText(getApplicationContext(), "与服务器连接断开，上传未完成！",
						Toast.LENGTH_SHORT).show();	
			}
		}
		@Override
		protected Integer doInBackground(String... arg0) {
			// TODO Auto-generated method stub
		
			String sql = "select _key,data from trace where upload=0;";
			Cursor cr = mSQLiteDatabase.rawQuery(sql, null);
			int upcnt = 0, tcnt = cr.getCount();
			mDlg.setMax(cr.getCount());
			while (cr.moveToNext() && mConnectStatus==2) {
				mDlg.setProgress(++upcnt);
				String data = cr.getString(1);
				mConnect.send(data);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//sql = "UPDATE trace SET upload=1 WHERE _key="+cr.getInt(0);
				if (false) {
				sql = "DELETE FROM trace WHERE _key="+cr.getInt(0);
				mSQLiteDatabase.execSQL(sql);
				}
			}
			cr.close();
			int r = -1;
			if (tcnt > 0) r = upcnt / tcnt;
			return r;
		}
		
	}
}
