package com.zonesion.loc.droid.client.offline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.wifi.ScanResult;
import android.util.AttributeSet;
import android.view.View;

public class HistogramView extends View {
	private static final String TAG = "HistogramView";

	public HistogramView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public HistogramView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	public HistogramView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	int  O_OFFSET_X = 130, O_OFFSET_Y = 150;
	void drawCoordinate(Canvas canvas) {
		
		O_OFFSET_X = 150;
		O_OFFSET_Y = getHeight()/5*2;
		
		Paint paint = new Paint(); //设置一个笔刷大小是3的黄色的画笔    
        paint.setColor(Color.GRAY);  
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(0, getHeight()-O_OFFSET_Y, getWidth(), getHeight()-O_OFFSET_Y, paint);
        canvas.drawLine(O_OFFSET_X, 0, O_OFFSET_X, getHeight(), paint);
        
        int dis = (getWidth() - O_OFFSET_X - 10) / 10 / 10;
        
        canvas.drawText("-100", O_OFFSET_X+4, getHeight()-O_OFFSET_Y+15, paint);
        for (int i=1; i<=100; i++) {
        	int h;
        	if (i%10 == 0) h = 6;
        	else if (i%5==0) h = 3;
        	else h = 2;
        	canvas.drawLine(O_OFFSET_X+i*dis, getHeight()-O_OFFSET_Y-h, O_OFFSET_X+i*dis, getHeight()-O_OFFSET_Y, paint);
        	if (i%10 == 0) {
        		canvas.drawText(""+(i-100), O_OFFSET_X+i*dis-10, getHeight()-O_OFFSET_Y+15, paint);
        	}
        }
      
	}
	

	int[] colors = new int[]{0xffff0000, 0xff00ff00, 0xff0000ff,0xffffff00, 0xffff00ff, 0xff00ffff,
			0xff7b68ee, Color.DKGRAY, Color.LTGRAY, 0xffcc9999, 0xff00cccc, 0xff6666cc, 0xff999933, 
			0xffcccc00, 0xff666600, 0xffccccff};  
	
	
	
	
	void drawSamples(int idx, String mac, ArrayList<Integer> rssis, Canvas canvas) {
		Paint paint = new Paint(); //设置一个笔刷大小是3的黄色的画笔    
        paint.setColor(colors[idx]);  
        paint.setStyle(Paint.Style.STROKE);
        int[] a = new int[100];
        int max = 0, rs=-100;
        for (Integer rssi : rssis) {
        	if (rssi>-100 && rssi<0) {
        		a[-rssi] ++;
        		if (a[-rssi] > max) {
        			max = a[-rssi];
        			rs = rssi;
        		}
        	}
        } 
        //int dis = (getWidth() - O_OFFSET_X - 50) / 10 / 10;
        int dis = (getWidth() - O_OFFSET_X - 10) / 10 / 10;
       
        int one_single_height = O_OFFSET_Y/25;
        
        for (int i=1; i<100; i++) {
        	if (a[i]>0 || a[i-1]>0) {
        		float x1 =  O_OFFSET_X+(100*dis) - ((i-1)*dis);
        		float y1 = getHeight()-O_OFFSET_Y -     a[i-1] * one_single_height;
        		float y2 = getHeight()-O_OFFSET_Y -     a[i] * one_single_height;
        		canvas.drawLine(x1, y1, x1-dis, y2, paint);
        	}
        } 
        
        canvas.drawText(mac, 20, getHeight()-O_OFFSET_Y + 30 + (idx*20), paint);
        Rect r = new Rect();
        r.left = O_OFFSET_X;
        r.top = getHeight()-O_OFFSET_Y + 20 + (idx*20);
        r.right = r.left + (100 + rs)*dis;
        r.bottom = r.top + 10;
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(r, paint);
	}
	
	private HashMap<String, ArrayList<Integer>> mAps = new HashMap<String, ArrayList<Integer>>();
	
	void clearAps() {
		synchronized(mAps) {
			mAps.clear();
		}
	}
	void addAps(String mac, int rssi) {
		synchronized(mAps) {
			ArrayList<Integer> rs;
			if (mAps.containsKey(mac)) {
				rs = mAps.get(mac);
				
			} else {
				rs = new ArrayList<Integer>();
				mAps.put(mac, rs);
			}
			rs.add(rssi);
		}
	}
	ArrayList mLastScans = new ArrayList(); 
	
	void addScanResult(List<ScanResult> sr) {
		
		mLastScans.add(sr);
		for (ScanResult r: sr) {
			addAps(r.BSSID, r.level);
		}
	
		if (mLastScans.size()>25){
			sr = (List<ScanResult>) mLastScans.get(0);
			mLastScans.remove(0);
			for (ScanResult r : sr) {
				ArrayList<Integer> a = mAps.get(r.BSSID);
				a.remove(new Integer(r.level));
				if (a.size() == 0) {
					mAps.remove(r.BSSID);
				}
			}
		}
	}
	
	
	ArrayList<String> sort() {
		ArrayList<String> srt = new ArrayList();
		ArrayList<Integer> srs = new ArrayList();
		
		for (String key : mAps.keySet()) {
			ArrayList<Integer> ai = mAps.get(key);
			byte[] a = new byte[100];
			int max = 0;
			int mrs = 0;
			for (Integer rssi : ai) {
		        	if (rssi>-100 && rssi<0) {
		        		a[-rssi] ++;
		        		if (a[-rssi] > max) {
		        			max = a[-rssi];
		        			mrs = rssi;
		        		}
		        	}
		    }
			if (srt.size() == 0) {
				srs.add(mrs);
				srt.add(key);
			} else if (mrs <= srs.get(srs.size()-1)) {
				srs.add(mrs);
				srt.add(key);
			} else {
			for (int i=0; i<srt.size(); i++) {
				if (mrs > srs.get(i)) {
					srt.add(i, key);
					srs.add(i, mrs);
					break;
				}
			}
			}
			
		}
		return srt;
	}
	
	@Override     
    protected void onDraw(Canvas canvas) {
		drawCoordinate(canvas);
		synchronized(mAps) {
			int idx = 0;
			
			
			ArrayList<String> as = sort();
			for (idx=0; idx<as.size(); idx++) {
				if (idx < colors.length) {
					String key = as.get(idx);
					
					drawSamples(idx, key, mAps.get(key), canvas);
				}
			}
			/*
			for (String key : mAps.keySet()) {
				if (key.equalsIgnoreCase("00:15:6D:ED:45:2E")
						|| key.equalsIgnoreCase("00:15:6D:ED:45:2A")
						|| key.equalsIgnoreCase("00:15:6D:ED:45:5a")) {
					drawSamples(idx++, key, mAps.get(key), canvas);
				}
			}*/
		
			/*
			for (String key : mAps.keySet()) {
				if (idx < colors.length) {
					drawSamples(idx++, key, mAps.get(key), canvas);
				}
			}*/
		}
	}
}
