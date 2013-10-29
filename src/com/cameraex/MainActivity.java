package com.cameraex;

import robots.parrot.ctrl.Parrot;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

public class MainActivity extends Activity{/* implements SurfaceTextureListener{
	
    private Camera 		mCamera;
    private TextureView mTextureView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        setContentView(mTextureView);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
        	Log.i("ierkhphy","pshgposh");
        	mCamera = Camera.open();
        	Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        	mTextureView.setLayoutParams(new FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER));
        	try {
        		mCamera.setPreviewTexture(surface);
        	} catch (IOException t) {}
        } catch (Exception t) {}
        mCamera.startPreview();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, the Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Update your view here!
    }


    public synchronized void onFrameAvailable(SurfaceTexture surface) {
    	Log.i("dslihfdsfp","fodfjfo");
    }
}  */
	
	private Buffers mBuffers;
	
	static Parrot aRDrone;
	private PID mPID;
	private Slope mSlope;
    private ImgProcss mImgProcss;
    private CameraPreview mCameraPreview;
    private DrawView mDrawView;
    private int camResW=320,camResH=240,scale=3;
    FrameLayout mFrameLayout;
    volatile float touched_x, touched_y;
    private boolean mPIDIsRunning,loaded = false,connected = false,mBuffersIsRunning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //size-values=3264x2448,3264x1836,3264x2176,2592x1944,2048x1536,1920x1080,1600x1200,1280x960,1280x720,720x480,800x480,640x480,352x288,320x240,176x144
        getWindow().setLayout(camResW*scale, camResH*scale);//.setLayoutParams(new FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER)
       // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);   
       
        setContentView(R.layout.activity_main);
        loaded = false;
        connected = false;
        
    }

    public void Load(){
    	Log.i("MA LOAD","MA LOAD");
    	if (!loaded) {
        mFrameLayout =(FrameLayout)findViewById(R.id.frameLayout);
        //FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mFrameLayout.getLayoutParams();
       // Log.i("a: "+params.width,"b: "+params.height);
        /*
        params.width = camResW*3;
        params.height = camResH*3;
        mFrameLayout.setLayoutParams(params);
        */
			aRDrone = new Parrot ();
			mPID = new PID (aRDrone);
			mBuffers = new Buffers (aRDrone,mPID);
	        mDrawView = new DrawView(this,camResW,camResH,scale);
	        mImgProcss	= new ImgProcss (mDrawView, mPID);
	        mCameraPreview = new CameraPreview(this, mImgProcss);   	    
	        mFrameLayout.addView(mCameraPreview);
	        mFrameLayout.addView(mDrawView);
	        mPIDIsRunning = false;
	        mBuffersIsRunning = false;
	        mSlope = new Slope ();
	        loaded = true;
    	}
 
    }
    
    protected void onPause() {
    	super.onPause();
    	Log.i("MA PAUSE","MA PAUSE");
        if (mCameraPreview != null){
        	mCameraPreview.onPause();
        	mCameraPreview = null;
        }
        if (mPID != null) {
//        	mPID.Stop();
        	mPID = null;
        }
        if (aRDrone != null) {
        	aRDrone.destroy();
        	aRDrone = null;
        }
        if (mBuffers != null) {
        	mBuffers.onStop();
        	mBuffers = null;
        }
        mImgProcss = null;
        mDrawView = null;
        Log.i("onpause","main");
        loaded = false;
        connected = false;
    }
 
    protected void onResume(){
    	super.onResume();
        Load();
    }
    
    /*Take Off / Land*/
    public void takeOff (View view) {
		Log.i("gjjdjgh","diofhdoigdiogadofugadfulabgfldubgf");
	    ToggleButton State = (ToggleButton) findViewById(R.id.takeOff);
	    if (State.isChecked()) {
	    	aRDrone.takeOff ();
			//startPID ();
			Log.i("difheosgihodsgldsiogfsiuogfaeogaeuof","diofhdoigdiogadofugadfulabgfldubgf");
	    }else{
			mPID.onPause();
	    	aRDrone.land();
			Log.i("weiogqiyoaeghwiyethailekthaeigtaelgtl","eaiogtwyeiugtwuegfeiugfqeuigfqiegfie");
	    }
    }
    
	/*public void startPID() {
	    if (!mPIDIsRunning) {
	        mPID.start();
	        mPIDIsRunning = true;
	    } else {
	    	mPID.onResume();
	    }
	}*/
    
    
    public void connectP (View view) {
    	if (!aRDrone.m_bConnected) {
	        aRDrone.connect();
    	}       
    }
    
    public boolean onTouchEvent(MotionEvent event) {
    	
    	int eventAction = event.getAction();

    	touched_x = event.getRawX();
    	touched_y = event.getRawY();
    	if (touched_x > 1080) touched_x = 1080; 
    	touched_x -= 120;
    	if (touched_x < 0) touched_x = 0;
    	if (touched_y > 742) touched_y = 742; 
    	touched_y -= 22;
    	if (touched_y < 0) touched_y = 0;	
    	
    	switch (eventAction) {
	        case MotionEvent.ACTION_DOWN: 
	            // finger touches the screen
	        	mPID.begin();
	        	mSlope.onSet(touched_x, touched_y);
	            break;
	
	        case MotionEvent.ACTION_MOVE:
	            // finger moves on the screen

	        	mSlope.update(touched_x, touched_y);
	            break;
	
	        case MotionEvent.ACTION_UP:   
	            // finger leaves the screen
	        	
	        	startBuffers (2, 5, 20, mSlope.getSlopes(touched_x, touched_y));
	        	Log.i("info: "+mSlope.getSlopes(touched_x, touched_y),"wrfaee");
	            break;
	        default:
	        	break;
    }
    	
    //	if (aRDrone.m_bConnected){
     //   	startBuffers (3,5,20);

    //		mPID.begin ();
    //	}
    	
    	return true;
    }

    
	public void startBuffers(int a, int b, int c, int d) {
	    mBuffers.onPause();
    	mBuffers.Set(a, b, c, d,mSlope.getBufferLines());
		if (!mBuffersIsRunning) {
	    	mBuffers.start();
	        mBuffersIsRunning = true;
	    } else {
	    	mBuffers.onResume();
	    }
	}
    
}

