package com.zonesion.loc.droid.map;

import java.util.HashSet;

import com.zonesion.loc.droid.client.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.AttributeSet;

public class MapView extends SuperImageView {

	Bitmap mLocIcon;
	Bitmap mPosIcon;
	float curX = Float.NaN, curY=Float.NaN;
	
	class Point {
		float x;
		float y;
		@Override
		public int hashCode() {
		  return ("____Point__"+(x+y)+"____Point__#@#%@".toString()).hashCode();
		}
		Point(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
	
	HashSet<Point> mPositions = new HashSet<Point>(); 
	
	public void addPosition(float x, float y) {
		mPositions.add(new Point(x, y));
	}
	public void clrPosition() {
		mPositions.clear();
	}
	
	public MapView(Context context) {
		super(context);
		init();
	}

	public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	private void init() {
		mLocIcon = BitmapFactory.decodeResource(getResources(),
				R.drawable.loc0216);
		mPosIcon = BitmapFactory.decodeResource(getResources(),
				R.drawable.loc0116);
	}
	public void setCurPoint(float x, float y) {
		if (x != curX || y != curY) {
			curX = x;
			curY = y;
			postInvalidate();
		}
	}
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		float[] p = new float[2];
		
		for (Point pt : mPositions) {
			p[0] = pt.x;
			p[1] = pt.y;
			getImageMatrix().mapPoints(p);
			canvas.drawBitmap(mPosIcon, p[0]-mPosIcon.getWidth()/2, p[1]-mPosIcon.getHeight()/2, null);
		}
		if (!Float.isNaN(curX) && !Float.isNaN(curY)) {
			p[0] = curX;
			p[1] = curY;
			getImageMatrix().mapPoints(p);
			canvas.drawBitmap(mLocIcon, p[0]-mLocIcon.getWidth()/2, p[1]-mLocIcon.getHeight()/2, null);
		}
	}

}
