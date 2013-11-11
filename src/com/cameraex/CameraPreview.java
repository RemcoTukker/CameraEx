package com.cameraex;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

 
public class CameraPreview extends SurfaceView  implements SurfaceHolder.Callback ,PreviewCallback{

	private SurfaceHolder mHolder;
	private Camera mCamera;
	ImgProcss mImgProcss;
	Bitmap newBmp;
	private int camResW=320,camResH=240,scale=3;

	public CameraPreview(Context context, ImgProcss imgProcss) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
		mImgProcss = imgProcss;
		Log.i("CAMERAPREVIEW","ESO");

	}

	public void surfaceCreated(SurfaceHolder holder) {
	// The Surface has been created, now tell the camera where to draw the preview.
		Log.i("SURFACECREATED","ESO");
		try {
			mCamera = Camera.open();
			try {
				Camera.Parameters mparameters = mCamera.getParameters();
				mparameters.setPreviewSize(camResW, camResH);
				mparameters.setPreviewFormat(ImageFormat.NV21);
				Log.i("mparameters.getMinExposureCompensation(): " + mparameters.getMinExposureCompensation(),"mparameters.getMaxExposureCompensation(): " + mparameters.getMaxExposureCompensation());
				mparameters.setExposureCompensation(5);
				mCamera.setParameters(mparameters);
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
				mCamera.setPreviewCallback(this);
			} catch (IOException e) {
				Log.d("CameraView", "Error setting camera preview: " + e.getMessage());
			}
		} catch (Exception e){ e.printStackTrace();}  	
	}
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null){ mHolder.removeCallback(this);mCamera.setPreviewCallback(null);mCamera.stopPreview();mCamera.release();mCamera = null;}
	}
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {


		if (mHolder.getSurface() == null){
			return;
		}
		try { mCamera.stopPreview(); } catch (Exception e){ /* ignore: tried to stop a non-existent preview*/ }
		try {
			Log.i("w: " + w,"h: "+h);
			Camera.Parameters mparameters = mCamera.getParameters();
			//size-values=3264x2448,3264x1836,3264x2176,2592x1944,2048x1536,1920x1080,1600x1200,1280x960,1280x720,720x480,800x480,640x480,352x288,320x240,176x144
			mparameters.setPreviewSize(camResW, camResH);
			mparameters.setPreviewFormat(ImageFormat.NV21);
			mCamera.setParameters(mparameters);
			mCamera.startPreview();
			mCamera.setPreviewCallback(this);
		} catch (Exception e){
			Log.d("CameraView", "Error starting camera preview: " + e.getMessage());
		}
	}
	public void onPause() {
		if (mCamera != null){ mHolder.removeCallback(this); mHolder = null; mCamera.setPreviewCallback(null);mCamera.stopPreview();mCamera.release();mCamera = null;mImgProcss.Stop(); mImgProcss = null;}
	}
	public void onResume(ImgProcss imgProcss) {
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.		
		mImgProcss = imgProcss;
		mHolder = getHolder();
		mHolder.addCallback(this);
		this.surfaceCreated(mHolder);
	}
	@Override  
	public void onPreviewFrame(byte[] data, Camera camera) {
		mImgProcss.Set(data,camResW,camResH,5,scale);
		data = null;
		return;
	}



}