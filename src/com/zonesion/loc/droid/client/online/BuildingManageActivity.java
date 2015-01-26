package com.zonesion.loc.droid.client.online;

import com.zonesion.loc.droid.client.R;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class BuildingManageActivity extends Activity implements OnClickListener{
	private static final String TAG = "BuildingManageActivity";
	
	Button mBTAddFloor;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.building_manage);
        
        mBTAddFloor = (Button) findViewById(R.id.bt_add_floor);
        mBTAddFloor.setOnClickListener(this);
	}
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if (arg0 == mBTAddFloor) {
	         Intent i = new Intent(Intent.ACTION_PICK,
	        	android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
	         startActivityForResult(i, RESULT_LOAD_IMAGE);

		}
		
	}
	
	static int RESULT_LOAD_IMAGE = 1001;
	 @Override
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		 super.onActivityResult(requestCode, resultCode, data);
		 Log.d(TAG, "xxxxxxxxxx");
	     if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
	    	 Uri selectedImage = data.getData();
	    	
	    	 String[] filePathColumn = { MediaStore.Images.Media.DATA };
	    	 Cursor cursor = getContentResolver().query(selectedImage,
	    			 filePathColumn, null, null, null);
	    	 cursor.moveToFirst();
	    	 int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	    	 String picturePath = cursor.getString(columnIndex);
	    	 cursor.close();
	    	 
	    	 Log.d(TAG, "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx "+picturePath);
	     }
	 }

	 
	
}
