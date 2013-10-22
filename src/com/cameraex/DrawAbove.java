package com.cameraex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class DrawAbove extends View {
	
	Bitmap mBitmap;
	Paint mRectangle;
	Paint mPaintBlack;
	Paint mPaintYellow;
	byte[] mYUVData;
	int[] mRGBData;
	int mImageWidth, mImageHeight;
	int[] mGrayHistogram;
	double[] mGrayCDF;
	int mState;
	
	static final int STATE_ORIGINAL = 0;
	static final int STATE_PROCESSED = 1;

    public DrawAbove(Context context) {
        super(context);
        
        mPaintBlack = new Paint();
        mPaintBlack.setStyle(Paint.Style.FILL);
        mPaintBlack.setColor(Color.BLACK);
        mPaintBlack.setTextSize(25);
        
        mPaintYellow = new Paint();
        mPaintYellow.setStyle(Paint.Style.FILL);
        mPaintYellow.setColor(Color.YELLOW);
        mPaintYellow.setTextSize(25);
        
        mRectangle = new Paint();
        mRectangle.setColor(Color.rgb(81, 252, 31));
        mRectangle.setStrokeWidth(2);
        
        mBitmap = null;
        mYUVData = null;
        mRGBData = null;
        mGrayHistogram = new int[256];
        mGrayCDF = new double[256];
        mState = STATE_ORIGINAL;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
        	int canvasWidth = canvas.getWidth();
        	int canvasHeight = canvas.getHeight();
        	int newImageWidth = 640;
        	int newImageHeight = 480;
        	int marginWidth = (canvasWidth - newImageWidth)/2;
        	        	

        	// Draw bitmap
        	mBitmap.setPixels(mRGBData, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight);
        	Rect src = new Rect(0, 0, mImageWidth, mImageHeight);
        	Rect dst = new Rect(marginWidth, 0, canvasWidth-marginWidth, canvasHeight);
        	canvas.drawBitmap(mBitmap, src, dst, mPaintBlack);
        	
        	// Draw black borders        	        	
        	canvas.drawRect(0, 0, marginWidth, canvasHeight, mPaintBlack);
        	canvas.drawRect(canvasWidth - marginWidth, 0, canvasWidth, canvasHeight, mPaintBlack);
        	
        	canvas.drawRect(0, 0, 10, 10, mRectangle);
        	
        } // end if statement
        
        super.onDraw(canvas);
        
    } // end onDraw method
}