package com.zonesion.loc.droid.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

//import com.zonesion.loc.droid.client.online.BuildingManageActivity;
import com.zonesion.loc.droid.client.offline.OfflineActivity;
import com.zonesion.loc.droid.client.online.OnlineActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class LocUserDroidActivity extends Activity implements OnClickListener,
		OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "LocUserDroidActivity";

	public static String sServer = "192.168.0.12";
	
	Button mBTNCreatBuilding;
	ListView mLVBuildings;
	SimpleAdapter mSABuilding;
	ArrayList<HashMap<String, String>> mBuildinglist = new ArrayList<HashMap<String, String>>();
	HashMap<String, JSONObject> mBuildingMap = new HashMap<String, JSONObject>(); 

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//mBTNCreatBuilding = (Button) findViewById(R.id.btn_creat_building);
		//mBTNCreatBuilding.setOnClickListener(this);

		mLVBuildings = (ListView) findViewById(R.id.listview_buildings);
		mLVBuildings.setOnItemClickListener(this);
		mLVBuildings.setOnItemLongClickListener(this);

		/*
		 * for(int i=0;i<30;i++) { HashMap<String, String> map = new
		 * HashMap<String, String>(); map.put("ItemTitle",
		 * "This is Title....."); map.put("ItemText", "This is text.....");
		 * mylist.add(map); }
		 */
		// 生成适配器，数组===》ListItem
		mSABuilding = new SimpleAdapter(this, // 没什么解释
				mBuildinglist,// 数据来源
				R.layout.list_item_building,// ListItem的XML实现

				// 动态数组与ListItem对应的子项
				new String[] { "name", "id" },

				// ListItem的XML文件里面的两个TextView ID
				new int[] { R.id.ItemTitle, R.id.ItemText });
		// 添加并且显示
		mLVBuildings.setAdapter(mSABuilding);
		
		
		HttpGetTask task = new HttpGetTask();  
	    task.execute("http://"+sServer+":8080/buildings"); 
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if (arg0 == mBTNCreatBuilding) {
			createNewBuilding();
		}
	}

	private void createNewBuilding() {
		final EditText et = new EditText(this);
		Builder dlg = new AlertDialog.Builder(this);
		dlg.setTitle("请输入建筑物名称").setIcon(android.R.drawable.ic_dialog_info)
				.setView(et)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						String name = et.getText().toString().trim();
						if (name.length() > 0) {
							int id = (int) Math.abs(Math.random() * 10000);
						
							String info;
							info = String.format("id=%d;name=%s\n", id, name);
							Log.d(LocUserDroidActivity.TAG, info);
							try {
								FileOutputStream outStream = LocUserDroidActivity.this
										.openFileOutput("index.txt",
												Context.MODE_WORLD_READABLE
														| Context.MODE_APPEND);
								outStream.write(info.getBytes());
								outStream.close();
								HashMap<String, String> map = new HashMap<String, String>();
								map.put("name", name);
								map.put("id", "" + id);
								mBuildinglist.add(map);
								mSABuilding.notifyDataSetChanged();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}).setNegativeButton("取消", null).show();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		
		HashMap<String, String> info = mBuildinglist.get(arg2);
		Log.d(TAG, "" + arg2 + " " + arg3+"id="+info.get("id"));
		int id = Integer.parseInt(info.get("id"));
		String name = info.get("name");
		
		JSONObject b = mBuildingMap.get(info.get("id"));
		Intent it = new Intent(this, OnlineActivity.class);
		//Intent it = new Intent(this, OfflineActivity.class);
		it.putExtra("building", b.toString());
		this.startActivity(it);
	}


	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub

		HashMap<String, String> info = mBuildinglist.get(arg2);
		Log.d(TAG, "" + arg2 + " " + arg3+"id="+info.get("id"));
		int id = Integer.parseInt(info.get("id"));
		String name = info.get("name");
		
		JSONObject b = mBuildingMap.get(info.get("id"));
		//Intent it = new Intent(this, OnlineActivity.class);
		Intent it = new Intent(this, OfflineActivity.class);
		it.putExtra("building", b.toString());
		this.startActivity(it);
		return false;
	}
	
	
	class HttpGetTask extends AsyncTask<String,Integer,String> 
	{

		@Override  
	    protected void onPostExecute(String res) {
		   if (res == null) return;
	       System.out.println(res);
	       JSONTokener jsonParser = new JSONTokener(res);
	    try {
			JSONArray jsonObjs = new JSONArray(res);
			for (int i=0; i<jsonObjs.length(); i++ ) {
				JSONObject x = (JSONObject) jsonObjs.opt(i);
				
				System.out.println(x.get("name"));
				System.out.println(x.get("id"));
				//System.out.println(x.get("floors").toString());
				
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("name", x.get("name").toString());
				map.put("id", "" + x.get("id").toString());
				//map.put("floors", x.get("floors").toString());
				
				mBuildingMap.put(x.get("id").toString(), x);
				
				mBuildinglist.add(map);
				mSABuilding.notifyDataSetChanged();
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	       
	    }  
		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			 String result = null;
		     BufferedReader reader = null;
		     try {
		            HttpClient client = new DefaultHttpClient();
		            HttpGet request = new HttpGet();
		            request.setURI(new URI(arg0[0]));
		            HttpResponse response = client.execute(request);
		            reader = new BufferedReader(new InputStreamReader(response
		                    .getEntity().getContent()));
		 
		            StringBuffer strBuffer = new StringBuffer("");
		            String line = null;
		            while ((line = reader.readLine()) != null) {
		                strBuffer.append(line);
		            }
		            result = strBuffer.toString();
		 
		        } catch (Exception e) {
		            e.printStackTrace();
		            return null;
		        } finally {
		            if (reader != null) {
		                try {
		                    reader.close();
		                    reader = null;
		                } catch (IOException e) {
		                    e.printStackTrace();
		                }
		            }
		        }
		 
		        return result;
		}
		
	}
}


