package com.cameraex;

import java.util.ArrayList;
import java.util.LinkedList;

import robots.parrot.ctrl.Parrot;
import android.os.SystemClock;
import android.util.Log;

public class Buffers extends Thread {
	
	/*
	Class controlling the quadcopter movements
		2 types of movements:
			- Buffered: for controlled trajectories
			- Controlled: for correcting the position and hovering
	 */
	
	static Parrot mParrot = null;
	private MainActivity mMainActivity;
	private PID mPID = null;
	private float t,dt,p1,p2,p3,p4;
	private int p5;
	private LinkedList<params> bufParams = new LinkedList<params>();
	private Object mPauseLock = new Object();
	private Object mSet = new Object();
	private boolean mPaused,  mRunning ,bufCreated,threadRunning = false;
	private float[] lineV  = new float[] { 0.00f, 0.70f, 0.00f, 0.70f, 0.00f, 0.70f, 0.00f, 0.70f, 0.00f, 0.70f, 0.00f,-0.45f};
	private float[] lineH  = new float[] { 0.12f, 0.00f, 0.12f, 0.00f, 0.12f, 0.00f, 0.12f, 0.00f, 0.12f, 0.00f,-0.30f, 0.00f};
	private float[] lineD1 = new float[] {-0.12f, 0.70f,-0.12f, 0.70f,-0.12f, 0.70f,-0.12f, 0.70f,-0.12f, 0.70f, 0.30f,-0.35f};
	private float[] lineD2 = new float[] { 0.12f, 0.70f, 0.12f, 0.70f, 0.12f, 0.70f, 0.12f, 0.70f, 0.12f, 0.70f,-0.30f,-0.35f};
	private float[] lineD3 = new float[] {-0.12f,-0.40f,-0.12f,-0.40f,-0.12f,-0.40f,-0.12f,-0.40f,-0.12f,-0.40f, 0.30f, 0.35f};
	private float[] lineD4 = new float[] { 0.12f,-0.40f, 0.12f,-0.40f, 0.12f,-0.40f, 0.12f,-0.40f, 0.12f,-0.40f,-0.30f, 0.35f};

	private ArrayList<Integer> bufferLines = new ArrayList<Integer>();
	
	/*
	Constructor of the class
	*/
	public Buffers (MainActivity mainActivity) {
		mParrot			= new Parrot ();
		mMainActivity	= mainActivity;
		mRunning		= true;
		mPaused			= true;
		bufCreated		= false;
		threadRunning 	= false;
	}
	
	public void parrotTakeOff () {
		mParrot.takeOff();
	}
	
	public void parrotLand (){
		mPID.onPause();
		mParrot.land();
	}
	
	public void parrotConnect () {
		if (!mParrot.m_bConnected)
		mParrot.connect();
	}
	
	public void setPID (PID pid) {
		mPID = pid;
	}
	
	/*
	Setting the position
	*/
	public void onSet (float a, float b, float c, float d, int command, ArrayList<Integer> aL) {
		synchronized (mSet) {	
			synchronized (mPauseLock) {
				if (mPaused) {
					p1 = a; p2 = b; p3 = c; p4 = d;	//Movement description
					p5 = command;					//Command to execute
					bufferLines = aL;				//Array storing the line primitives describing the figure to draw
					startThread ();
				}
			}
		}
	}
	
	private void startThread () {
		mRunning	= true;
		bufCreated	= false;
		if (!threadRunning) this.start();
		this.onResume();
		threadRunning = true;		
	}
	
	public void onStop () {
		synchronized (mSet) {
			mRunning = false;
			threadRunning = false;
		}
	}
	
	public void onPause() {
		synchronized (mPauseLock) {
			mPaused = true;
		}
	}
	
	public void onResume () {
		synchronized (mPauseLock) {
			mPaused = false;
			mPauseLock.notifyAll();
		}
	}
	
	public boolean onPaused () {
		return mPaused; 
	}
	
	/*
	Thread executing the control or buffered information depending on the set parameters
	Time gap between commands of 140 miliseconds, giving time to the quadcopter to execute them
	*/
	public void run() {
		int command;
		while (mRunning) {
			dt = SystemClock.uptimeMillis() - t;
			if (dt > 140) {
				t = SystemClock.uptimeMillis(); 
				if (p5 > 0)	command = (p5 & 448) >> 6;
				else 		command = (p5 & 448) >> 6 ^ 7;
				switch (command) {
					case 1: //Position control, two commands burst is sent.
						if (movement (p1,p2,p3,p4,2)) this.onPause();
						if (t > 40) t -= 40;	// To speed up the process.
						break;
					case 5: //Drawing the desired figures using primitive buffered lines
						mPID.clearPIDEn();
						if (lines (bufferLines)){
							mPID.setXPos(mPID.getXPos());
							mPID.setPIDEn();
							this.onPause();
						}
						break;
					default:
						this.onPause();
						break;
				}
			}
			synchronized (mPauseLock) {
				while (mPaused) {
					mMainActivity.writeServoMotor(0);
					try {
						mPauseLock.wait();
					} catch (InterruptedException e) {}
				}
			}
		}
		Log.i("END THREAD","BUFFERS");

		
	}
	
	public void onDestroy () {
		this.onStop ();
		if (mParrot != null) {
			mParrot.destroy();
			mParrot = null;
		}
		mMainActivity = null;
	}
	
	private boolean lines (ArrayList<Integer> bL) {
		if (!bufCreated) { 
			if (bL.size() != 0) {
				bufLine (bL.remove(0));
				bufCreated = true;
			}
		}
		if (!bufParams.isEmpty()) {
			params param = bufParams.removeFirst();
			if (mParrot != null) {
				mParrot.executeMoveCompose(param.vX,0,param.vZ,0);
				mMainActivity.writeServoMotor(180);
			}	
		} else {
			if (bL.size() != 0) bufCreated = false;
			else return true;
		}
		return false;
	}
	
	private boolean movement (float vX, float vY, float vZ, float vW, int nCommands) {
		if (!bufCreated) { 
			bufMove (vX,vY,vZ,vW,nCommands);
			bufCreated = true;
		}
		if (!bufParams.isEmpty()) {
			params param = bufParams.removeFirst();
			if (mParrot != null)
			mParrot.executeMoveCompose(param.vX,param.vY,param.vZ,param.vW);
		} else { return true;}
		return false;
	}
	
	public void bufMove (float vX, float vY, float vZ, float vW, int nCommands) {
		for (int i=0;i < nCommands;i++) {
			params param = new params(vX,vY,vZ,vW);
			bufParams.add(param);
		}
	}
	
	private void bufLine (int info) {

	float vX=0,vZ=0;
	int direction;
	
	bufParams.clear();
	direction = info & 7;

	if (info < 0) {
		direction = ((direction - 1) ^ 7) + 2;
	}
	for (int i = 0;i < 6;i ++) {
		switch (direction) {
		case 1:
			vX = lineD1[i*2];
			vZ = lineD1[i*2+1];
			break;
		case 2:
			vX = lineD2[i*2];
			vZ = lineD2[i*2+1];
			break;
		case 3:
			vX = lineD3[i*2];
			vZ = lineD3[i*2+1];
			break;
		case 4:
			vX = lineD4[i*2];
			vZ = lineD4[i*2+1];
			break;
		case 5:
			vX = -lineV[i*2];
			vZ = -lineV[i*2+1];
			break;
		case 6:
			vX = lineH[i*2];
			vZ = lineH[i*2+1];
			break;
		case 7:
			vX = lineV[i*2];
			vZ = lineV[i*2+1];
			break;
		case 8:
			vX = -lineH[i*2];
			vZ = -lineH[i*2+1];
		default:
			break;
		}
		params param = new params(vX,0,vZ,0);
		bufParams.add(param);
	}
}

	/*
	Class storing the parrot commands 
	*/
	public class params {
		public float vX = 0;
		public float vY = 0;
		public float vZ = 0;
		public float vW = 0;
	
		public params (float a, float b, float c, float d) {
			vX = a;
			vY = b;
			vZ = c;
			vW = d;
		}
	}
}
