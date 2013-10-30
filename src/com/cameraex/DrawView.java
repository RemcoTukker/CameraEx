package com.cameraex;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceView;

public class DrawView extends SurfaceView{
	private Paint textPaint;
	private Paint mGRectangle;
	private Paint mBRectangle;
	private Paint mKRectangle;

	public Bitmap mBitmap = null;
	public ArrayList<Rect> gRectangles = new ArrayList<Rect>();
	public ArrayList<Rect> bRectangles = new ArrayList<Rect>();
	public ArrayList<Rect> kRectangles = new ArrayList<Rect>();


	 public DrawView(Context context,int camResW,int camResH,int scale) {
		 super(context);
		 // Create out paint to use for drawing
		 	textPaint = new Paint();
		 	textPaint.setARGB(255, 200, 0, 0);
		 	textPaint.setTextSize(60);
		 
		    mGRectangle = new Paint();
		    mGRectangle.setStyle(Paint.Style.STROKE);
	        mGRectangle.setStrokeWidth(2);
	        mGRectangle.setColor(Color.GREEN);
	        
		    mBRectangle = new Paint();
		    mBRectangle.setStyle(Paint.Style.STROKE);
	        mBRectangle.setStrokeWidth(2);
	        mBRectangle.setColor(Color.CYAN);
	        
		    mKRectangle = new Paint();
		    mKRectangle.setStyle(Paint.Style.STROKE);
	        mKRectangle.setStrokeWidth(2);
	        mKRectangle.setColor(Color.BLACK);
			 
	     //   src = new Rect(0, 0, camResW, camResH);
		//	dst = new Rect(0,0, camResW*scale,camResH*scale);
	        
		 // This call is necessary, or else the 
		 // draw method will not be called. 
		 setWillNotDraw(false);
	 }
	 
	 
	 @Override
	 protected void onDraw(Canvas canvas){
		 // A Simple Text Render to test the display
		 
		// if (mBitmap != null) {
		//	 canvas.drawBitmap(mBitmap, src, dst, null);
		// }
		 synchronized(gRectangles) {
			 for (Rect rect : gRectangles) {
				 canvas.drawRect(rect, mGRectangle);
			 }
		 }
		 synchronized(bRectangles) {
			 for (Rect rect : bRectangles) {
				 canvas.drawRect(rect, mBRectangle);
			 }
		 }
		 synchronized(kRectangles) {
			 for (Rect rect : kRectangles) {
				 canvas.drawRect(rect, mKRectangle);
			 }
		 }
	 }
	 
	 
}