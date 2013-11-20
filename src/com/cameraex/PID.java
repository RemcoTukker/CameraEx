package com.cameraex;

import android.os.SystemClock;
import android.util.Log;

public class PID extends Thread { 
	
	private Buffers mBuffers = null;
	/*Control*/
	private volatile float 	xP = 0, xI =  0, xD = 0, xPID, yP = 0, yI =  0, yD = 0, yPID, zP = 0, zI =  0, zD = 0, zPID; 
	private volatile float 	xKP = 0, xKI =  0, xKD = 0, yKP = 0, yKI =  0, yKD = 0, zKP = 0, zKI =  0, zKD = 0;
	private volatile float 	xN = 0, yN =  0, zN = 0;
	private volatile float	xError = 0, xPrevError = 0, yError = 0, yPrevError = 0, zError = 0, zPrevError = 0;
	private volatile float	xPosAct = -1, xPosDes = -1, yPosAct = -1, yPosDes = -1, zPosAct = -1, zPosDes = -1;
	volatile float signum,Alpha = 0.4f;
	volatile float avXPos,avYPos,avZPos;
	volatile double step = 0;
	/*Flags*/
	private volatile  boolean stable = false;
	/*Time*/
	private volatile float 	t, dt;
	
	private Object mX = new Object();
	private Object mY = new Object();
	private Object mZ = new Object();
	private Object mEPID = new Object();
	
	private boolean enabledPID,PIDSet;
	// Constructor stuff.      

	// This should be after your drawing/update code inside your thread's run() code.

	
	public PID (Buffers buffers) {
		mBuffers = buffers;
		PIDSet = false;
	}
	
	private void Set () {
		enabledPID  = true;
		stable		= false;
		xError		= 0; yError 	= 0; zError 	= 0;
		xPrevError	= 0; yPrevError	= 0; zPrevError	= 0;
		xPosAct = -1; xPosDes = -1; yPosAct = -1; yPosDes = -1; zPosAct = -1; zPosDes = -1;
		avXPos = -1; avYPos = -1;; avZPos = -1;
		xKP = 0.8f; xKI =  0.0f; xKD = 0.15f;
		//yKP = 0.8f; yKI =  0.0f; yKD = 0.1f;//0.05f; without structure
		yKP = 0.9f; yKI =  0.0f; yKD = 0.1f;//0.05f;
		zKP = 0.9f; zKI =  0.0f; zKD = 0.22f;
		//xN = 1000f; yN = 650f; zN = 230f;  //without structure
		xN = 1000f; yN = 435f; zN = 230f;
		xP = 0; xI =  0; xD = 0; 
		yP = 0; yI =  0; yD = 0;
		zP = 0; zI =  0; zD = 0;
		Alpha = 0.4f;
		step = 0;
		xPosDes = 150;
		yPosDes = 120;
		t = SystemClock.uptimeMillis();
		PIDSet = true;
	}
	
	public void onPause() {
		this.Set();
	}

	//public void run() {
	//    this.Set();
		public boolean runPID (int rotating) {
//			//		ARDRONE MOVEMENT
			//x --> (+) right	(-) left
			//y --> (+) back	(-) front
			//z --> (+) up		(-) down
			//r --> (+) cw		(-) cc
			if (!PIDSet) this.Set ();	
			dt = (SystemClock.uptimeMillis() - t) / 1000;
			if (dt < 0.5) return false;
			t  =  SystemClock.uptimeMillis();
			/*----------------------------------------------------------------------*
			 * 									XPID								*
			 *----------------------------------------------------------------------*/
			synchronized (mX) {
				if (xPosAct > 0 && xPosDes > 0) {					// Parrot detected and set
					if (avXPos < 0) avXPos = xPosAct;				// Parrot was not detected or set
					if (stable) {									// Time is stable
						avXPos = ALPF (xPosAct,avXPos,Alpha);		// Filtering position 
						xError = xPosDes - avXPos;					// Error
						xP  = xError * xKP;							// Proportional
						xI += xKI * xError * dt;					// Integral
						xD  = xKD * ((xError - xPrevError) / dt);	// Derivative
					}
					xPID = round((xP + xI + xD) / xN, 100);
					//xPID = within (xPID,-0.1f,0.1f,-0.01f,0.01f);	//without strcuture
					xPID = within (xPID,-0.12f,0.12f,-0.03f,0.03f);
					if (Math.abs(xError) < 15) 	xPID = 0;		
					Log.i("xPID","|xPosAct = "+xPosAct+" |avXPos = "+avXPos+" |xPosDes = "+xPosDes+" |xP = "+xP+" |xI = "+xI+" |xD = "+xD+" |xPID = "+xPID);
					xPrevError = xError;
				} else {
					Log.i("xPID","|xPosAct = "+xPosAct+" |xPosdes = "+xPosDes);
					xPID	= 0;
					avXPos	= -1;									//Parrot not detected or set
				}
			}
			/*----------------------------------------------------------------------*
			 * 									YPID								*
			 *----------------------------------------------------------------------*/
			synchronized (mY) {
				if (yPosAct > 0 && yPosDes > 0) {					// Parrot detected and set
					if (avYPos < 0) avYPos = xPosAct;				// Parrot was not detected or set
					if (stable && Math.abs(yPosAct) < 300) {		// Time is stable
						avYPos = ALPF (yPosAct,avYPos,Alpha);		// Filtering position 
						yError = yPosDes - avYPos;					// Error
						yP  = yKP * yError;							// Proportional
						yI += yKI * yError * dt;					// Integral
						yD  = yKD * ((yError - yPrevError) / dt);	// Derivative
					}
					yPID = round((yP + yI + yD) / yN, 100);
					//yPID = -within (yPID,-0.1f,0.1f,-0.01f,0.01f);	// (-)!!! without structure
					yPID = -within (yPID,-0.1f,0.12f,-0.03f,0.03f);	// (-)!!!
					if (Math.abs(yError) < 20) yPID = 0;
					Log.i("yPID","|yPosAct = "+yPosAct+" |avYPos = "+avYPos+" |yPosDes = "+yPosDes+" |yP = "+yP+" |yI = "+yI+" |yD = "+yD+" |yPID = "+yPID);
					yPrevError = yError;
				} else {
					Log.i("yPID","|yPosAct = "+yPosAct+" |yPosdes = "+yPosDes);
					yPID	= 0;
					avYPos	= -1;									//Parrot not detected or set
				}
			}
			/*----------------------------------------------------------------------*
			 * 									ZPID								*
			 *----------------------------------------------------------------------*/
			synchronized (mZ) {
				if (zPosAct > 0 && zPosDes > 0) {					// Parrot detected and set
					if (avZPos < 0) avZPos = zPosAct;				// Parrot was not detected or set
					if (stable) {									// Time is stable
						avZPos = ALPF (zPosAct,avZPos,Alpha);		// Filtering position 
						zError = yPosDes - avYPos;					// Error
						zP  = zKP * zError;							// Proportional
						zI += zKI * zError * dt;					// Integral
						zD  = zKD * ((zError - zPrevError) / dt);	// Derivative
					}
					zPID = round((zP + zI + zD) / zN, 100);
					zPID = within (zPID,-0.1f,0.1f,-0.01f,0.01f);
					if (Math.abs(zError) < 20) zPID = 0;
					zPrevError = zError;
				} else {
					avZPos = -1;									//Parrot not detected or set
				}
			}
			Log.i("BeforeCommand", "|rotating: " + rotating+" | xPID: " + xPID + " | yPID: " +yPID);
			//Time has been stabilized and quadcopter is not executing a motion buffer
			if (enabledPID && stable) {
				switch (rotating){
				case -1:
					if (xPID != 0 || yPID != 0){
					//if (xPID > 0) xPID = 0.10f; else if (xPID < 0) xPID = -0.06f;
					//if (yPID > 0) yPID = 0.07f; else if (yPID < 0) yPID = -0.07f;
					mBuffers.onSet(xPID, yPID, 0, 0, 64, null);
					}
					break;
				case 0:
					if (xPID != 0 || yPID != 0){
					//if (xPID > 0) xPID = 0.06f; else if (xPID < 0) xPID = -0.06f;
					//if (yPID > 0) yPID = 0.07f; else if (yPID < 0) yPID = -0.07f;
					mBuffers.onSet(xPID, yPID, 0, 0, 64, null);
					}
					break;
				case 1:
					mBuffers.onSet(0, 0, 0, 0.4f, 64, null);
					break;
				case 2:
					mBuffers.onSet(0, 0, 0, -0.4f, 64, null);
					break;
				default:
					break;
				}
			}
			stable = true;
			return true;
	}
	
	public void setxPosAct (float x) {
		synchronized (mX) {
			xPosAct = x;
		}
	}
	public void setyPosAct (float y) {
		synchronized (mY) {
			yPosAct = y;
		}
	}
	public void setzPosAct (float z) {
		synchronized (mZ) {
			zPosAct = z;
		}
	}
	public float getxPosAct () {
		synchronized (mX) {
			return xPosAct;
		}
	}
	public float getyPosAct () {
		synchronized (mY) {
			return yPosAct;
		}
	}
	public float getzPosAct () {
		synchronized (mZ) {
			return zPosAct;
		}
	}
	public void setXPos (float x) {
		synchronized (mX) {
			xPosDes = x;
			xPrevError = 0;
			avXPos = -1;
		}
	}
	
	public void setYPos (float y) {
		synchronized (mY) {
			yPosDes = y;
			yPrevError = 0;
			avYPos = -1;
		}
	}
	
	public float getXPos () {
		synchronized (mX) {
			return xPosAct;
		}
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

	/*private float signum (float x) {
		if (x < 0) return -1; else return 1; 
	}*/
	
	/*public void circle (double t) {
		xPosDes = 130f + (float)(20*Math.cos((t*Math.PI)/180));
		zPosDes = 60f + (float)(20*Math.sin((t*Math.PI)/180));
	}*/
	
	
	public float LPF (float a,float b, float c) {
		//wo^2 / s^2 + 2*shi*wo*s + wo ^ 2
		
		float as,cs;
		
		if (a < 0) a *= -1;
		as = a * a;
		cs = c * c;
		return as / (as + 2*a*b*c +cs);
		
	}
	
	void convolve (float p_coeffs[], int p_coeffs_n, float p_in[], float p_out[], int n) {
		
		int i=0, j=0, k=0;
		float tmp=0;

		for (i = 0;i < n;i++) {					//  position in output
			tmp = 0;
			for (j = 0;j < p_coeffs_n; j++) {	//  position in coefficients array 
				k = i - j;						//  position in input
				if (k >= 0)						//  bounds check for input buffer
				tmp += p_coeffs [i] * p_in [k];
			}
		}
		p_out [j] = tmp;
	}

	
	public float ALPF (float a, float b, float alpha) {
		return b + (a - b) * alpha;
	}
	
	public float round(float d, int decimal) {
		return (float)Math.round(d*decimal) / decimal;
	}
	
}
