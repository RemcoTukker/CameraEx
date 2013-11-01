package com.cameraex;

import robots.parrot.ctrl.Parrot;
import android.os.SystemClock;
import android.util.Log;

public class PID extends Thread { 
	/*Drone*/
	static Parrot aRDrone = null;
	/*Control*/
	private volatile float 	xP = 0, xI =  0, xD = 0, xPID, yP = 0, yI =  0, yD = 0, yPID, zP = 0, zI =  0, zD = 0, zPID; 
	private volatile float 	xKP = 0, xKI =  0, xKD = 0, yKP = 0, yKI =  0, yKD = 0, zKP = 0, zKI =  0, zKD = 0;
	private volatile float 	xN = 0, yN =  0, zN = 0;
	private volatile float	xError = 0, xPrevError = 0, yError = 0, yPrevError = 0, zError = 0, zPrevError = 0;
	volatile float	xPosAct = -1, xPosDes = -1, yPosAct = -1, yPosDes = -1, zPosAct = -1, zPosDes = -1;
	volatile float signum; 
	volatile double step = 0;
	/*Flags*/
	private volatile  boolean running = true, stable = false;
	/*Time*/
	private volatile float 	t, dt;
	
	private Object mPauseLock = new Object();
	private Object mY = new Object();
	private Object mEPID = new Object();
	private boolean mPaused;
	
	private boolean enabledPID;
	// Constructor stuff.      

	// This should be after your drawing/update code inside your thread's run() code.

	
	public PID (Parrot parrot) {
		aRDrone = parrot;
	}
	
	private void Set () {
		enabledPID  = true;
		stable		= false;
		running		= true;
		xError 	 	= 0; yError 	= 0; zError 	= 0;
		xPrevError	= 0; yPrevError	= 0; zPrevError	= 0;
		xPosAct = -1; xPosDes = -1; yPosAct = -1; yPosDes = -1; zPosAct = -1; zPosDes = -1;
		xKP = 0.8f; xKI =  0.00001f; xKD = 0.22f;
		yKP = 0.8f; yKI =  0.00001f; yKD = 0.05f;//0.05f;
		zKP = 0.9f; zKI =  0.00001f; zKD = 0.22f;
		xN = 230; yN = 500; zN = 230;
		xP = 0; xI =  0; xD = 0; 
		yP = 0; yI =  0; yD = 0;
		zP = 0; zI =  0; zD = 0;
		step = 0;
		t = SystemClock.uptimeMillis();
	}
	
	public void Stop () {
		running = false;
	}
	
	public void onPause() {
	    /*synchronized (mPauseLock) {
	        mPaused = true;
	    }*/
		this.Set();
	}

	public void onResume() {
		synchronized (mPauseLock) {
			this.Set();
			mPaused = false;
			mPauseLock.notifyAll();
		}
	}
	public void begin () {
		this.Set();
		circle (0);
		yPosDes = 140;
	}
	//public void run() {
	//    this.Set();
		public void runPID (int rotating) {
//			//		ARDRONE MOVEMENT
			//x --> (+) right	(-) left
			//y --> (+) back	(-) front
			//z --> (+) up		(-) down
			//r --> (+) cw		(-) cc
			
			
			Log.i("xPosDes: "+xPosDes,"xPosAct: "+xPosAct);
//			Log.i("zPosDes: "+zPosDes,"zPosAct: "+zPosAct);
			dt = (SystemClock.uptimeMillis() - t) / 1000;
			t = SystemClock.uptimeMillis(); 
//			Log.i("dt: " + dt,"dt "+ dt);
			if (xPosAct > 0 && xPosDes > 0) {
				xError = xPosDes - xPosAct;
				if (stable) {		
					xP  = xError * xKP; 
					xI += 0;
					xD  = xKD * ((xError - xPrevError) / dt);
				}
				stable = true;
				xPrevError = xError;
				xPID = xP + xI + xD;
				xPID /= xN;
				xPID = within (xPID,-1,1,-0.05f,0.05f);
			}
			if (yPosAct > 0 && yPosDes > 0) {
				synchronized (mY) {
				Log.i(".  .  .  .  .  .  .  ",".  .  .  .  .  .  .  ");
				Log.i("yPosDes: "+yPosDes,"yPosAct: "+yPosAct);
				yError = yPosDes - yPosAct;
				if (stable) {
					yP  = yError * yKP; 
					yI += 0;
					yD  = yKD * ((yError - yPrevError) / dt);
				}
				yPID = yP + yI + yD;
				yPID /= yN;
				yPID = -within (yPID,-0.08f,0.08f,-0.04f,0.04f);	// (-)!!!
				Log.i("yPID: " + yPID,"dt: " + dt);
				Log.i("yP: " + yP,"yD: " + yD);
				Log.i("yError: " + yError,"yPrevError: " + yPrevError);
				Log.i(".  .  .  .  .  .  .  ",".  .  .  .  .  .  .  ");
				if (Math.abs(yError) < 20) yPID = 0;
				if (enabledPID && yPID != 0) {
					Log.i("inside PID: " + yPID,"inside PID");
					//rotating = -1;
					switch (rotating){
					case -1:
						aRDrone.executeMoveCompose(0,yPID,0, 0f);
						break;
					case 0:
						aRDrone.executeMoveCompose(0,yPID,0, 0f);
						break;
					case 1:
						aRDrone.executeMoveCompose(0,yPID,0, 0.05f);
						break;
					case 2:
						aRDrone.executeMoveCompose(0,yPID,0, -0.05f);
						break;
						
					}
				}
				yPrevError = yError;
				}
			}
			if (zPosAct > 0 && zPosDes > 0) {
					zError = zPosDes - zPosAct;
					if (stable) {		
						zP  = zError * zKP; 
						zI += 0;
						zD = zKD * ((zError - zPrevError) / dt);
					}
					zPrevError = zError;
					zPID = zP + zI + zD;
					zPID /= zN;
					zPID = within (zPID,-1,1,-0.05f,0.05f);
			}
			if (stable) {
				if (Math.abs(xError) < 5) xPID = 0;
				if (Math.abs(yError) < 20) yPID = 0;
				if (Math.abs(zError) < 5) zPID = 0;
	/*			if (xPID == 0 && zPID == 0) {
					step = (step + 30) % 360;
					circle (step);
					aRDrone.hover();
				}
				*/
				//aRDrone.executeMoveCompose(xPID,-yPID, zPID, 0f);
				
			}
//	    	this.onPause();
//			synchronized (mPauseLock) {
//			    while (mPaused) {
//			        try {
//			            mPauseLock.wait();
//			        } catch (InterruptedException e) {}
//			    }
//			}
			
//		}
//		Log.i("END THREAD","PID");
	}
	
	public void setPIDEn () {
		synchronized (mEPID) {
			enabledPID = true;
		}
	}
	
	public void clearPIDEn () {
		synchronized (mEPID) {
			enabledPID = false;
		}
	}
	
	public float getYPID () {
		synchronized (mY) {
			Log.i("yPID: " + yPID,"GETmethod");
			return yPID;
		}	
	}
		
	private float within(float x, float min, float max, float min2, float max2) {
		if (x < min) return min;
		if (x > max) return max;
		if (x > min2 && x < 0) return min2;
		if (x < max2 && x > 0) return max2;
		return x;
	}

	private float signum (float x) {
		if (x < 0) return -1; else return 1; 
	}
	
	public void circle (double t) {
		xPosDes = 130f + (float)(20*Math.cos((t*Math.PI)/180));
		zPosDes = 60f + (float)(20*Math.sin((t*Math.PI)/180));
	}
	
	
	public float LPF (float a,float b, float c) {
		float as,cs;
		
		if (a < 0) a *= -1;
		as = a * a;
		cs = c * c;
		return as / (as + 2*a*b*c +cs);
		
	}
	public float round(float d, int decimal) {
		return (float)Math.round(d*decimal) / decimal;
	}
	
}
