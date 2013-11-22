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
	private volatile float	xPosDes = -1, yxPosDes = -1, yPosDes = -1, zPosDes = -1;
	volatile float signum,Alpha = 0.4f;
	volatile float avxPos,avyPos,avzPos;
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
		xPosDes = -1; yPosDes = -1; zPosDes = -1;
		avxPos = -1; avyPos = -1;; avzPos = -1;
		xKP = 0.8f; xKI =  0.0f; xKD = 0.15f;
		//yKP = 0.8f; yKI =  0.0f; yKD = 0.1f;//0.05f; without structure
		yKP = 0.9f; yKI =  0.0f; yKD = 0.1f;//0.05f;
		zKP = 0.9f; zKI =  0.0f; zKD = 0.22f;
		//xN = 1000f; yN = 650f; zN = 230f;  //without structure
		//xN = 1000f; yN = 435f; zN = 230f;  //No proportional
		xN = 875f; yN = 500f; zN = 230f; 
		xP = 0; xI =  0; xD = 0; 
		yP = 0; yI =  0; yD = 0;
		zP = 0; zI =  0; zD = 0;
		Alpha = 0.4f;
		step = 0;
		setNewxPosDes (60,100);
		yPosDes = 100;
		t = SystemClock.uptimeMillis();
		PIDSet = true;
	}
	//v1: 850 500 230 0.45 0.10(Buf) + / -
	//v1: 875 500 230 0.40 0.08(Buf)

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
			float timeoutCommand = 0.48f;
			
			if (!PIDSet) this.Set ();
			dt = (SystemClock.uptimeMillis() - t) / 1000;
			/*----------------------------------------------------------------------*
			 * 									XPID								*
			 *----------------------------------------------------------------------*/
			synchronized (mX) {
				if (avxPos > 0) {									// Parrot detected and set
					if (stable) {									// Time is stable
						xError = proportionalE (xPosDes,avxPos);
						//xError = xPosDes - avxPos;					// Error
						xP  = xError * xKP;							// Proportional
						xI += xKI * xError * dt;					// Integral
						xD  = xKD * ((xError - xPrevError) / dt);	// Derivative
					}
					xPID = round((xP + xI + xD) / xN, 1000);
					//xPID = within (xPID,-0.1f,0.1f,-0.01f,0.01f);	//without strcuture
					xPID = within (xPID,-0.03f,0.03f,-0.01f,0.01f);
					if (Math.abs(xError) < 15) 	xPID = 0;
					if (dt >= timeoutCommand)
					Log.i("xPID","|xError = "+xError+" |avXPos = "+avxPos+" |xPosDes = "+xPosDes+" |xP = "+xP+" |xI = "+xI+" |xD = "+xD+" |xPID = "+xPID);
					xPrevError = xError;
				} else {
					if (dt >= timeoutCommand)
					Log.i("xPID","|avXPos = "+avxPos+" |xPosdes = "+xPosDes);
					xPID	= 0;
				}
			}
			/*----------------------------------------------------------------------*
			 * 									YPID								*
			 *----------------------------------------------------------------------*/
			synchronized (mY) {
				if (avyPos > 0) {									// Parrot detected and set
					if (stable) {									// Time is stable
						yError = proportionalE (yPosDes,avyPos);
						yP  = yKP * yError;							// Proportional
						yI += yKI * yError * dt;					// Integral
						yD  = yKD * ((yError - yPrevError) / dt);	// Derivative
					}
					yPID = round((yP + yI + yD) / yN, 1000);
					//yPID = -within (yPID,-0.1f,0.1f,-0.01f,0.01f);	// (-)!!! without structure
					yPID = -within (yPID,-0.03f,0.03f,-0.01f,0.01f);	// (-)!!!
					if (Math.abs(yError) < 15) yPID = 0;
					if (dt >= timeoutCommand)
					Log.i("yPID","|yError = "+yError+" |avYPos = "+avyPos+" |yPosDes = "+yPosDes+" |yP = "+yP+" |yI = "+yI+" |yD = "+yD+" |yPID = "+yPID);
					yPrevError = yError;
				} else {
					if (dt >= timeoutCommand)
					Log.i("yPID","|avyPos = "+avyPos+" |yPosdes = "+yPosDes);
					yPID	= 0;
				}
			}
			/*----------------------------------------------------------------------*
			 * 									ZPID								*
			 *----------------------------------------------------------------------*/
			synchronized (mZ) {
				if (avzPos > 0) {									// Parrot detected and set
					if (stable) {									// Time is stable
						zError = proportionalE (zPosDes,avzPos);
						zP  = zKP * zError;							// Proportional
						zI += zKI * zError * dt;					// Integral
						zD  = zKD * ((zError - zPrevError) / dt);	// Derivative
					}
					zPID = round((zP + zI + zD) / zN, 100);
					zPID = within (zPID,-0.1f,0.1f,-0.01f,0.01f);
					if (Math.abs(zError) < 10) zPID = 0;
					zPrevError = zError;
				} else {
					zPID	= 0;
				}
			}
			stable = true;
			if (dt < timeoutCommand) return false;
			t  =  SystemClock.uptimeMillis();
			
			//Time has been stabilized and quadcopter is not executing a motion buffer
			if (enabledPID && stable) {
				switch (rotating){
				case -1:
					if (yPID != 0) {		mBuffers.onSet(0, yPID, 0, 0, 64, null);
					} else if (xPID != 0) {	mBuffers.onSet(xPID, 0, 0, 0, 64, null); }
					//mBuffers.onSet(xPID, yPID, 0, 0, 64, null);
					break;
				case 0:
					if (yPID != 0) {		mBuffers.onSet(0, yPID, 0, 0, 64, null);
					} else if (xPID != 0) {	mBuffers.onSet(xPID, 0, 0, 0, 64, null); }
					//mBuffers.onSet(xPID, yPID, 0, 0, 64, null);
					break;
				case 1:
					Log.i("ROTATING", "ROTATING");
					mBuffers.onSet(0, 0, 0, 0.4f, 64, null);
					break;
				case 2:
					Log.i("ROTATING", "ROTATING");
					mBuffers.onSet(0, 0, 0, -0.4f, 64, null);
					break;
				default:
					break;
				}
			}
			Log.i("-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-", "-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.- " + dt);

			return true;
	}
	
	private float proportionalE (float desirePos, float posAct) {
		return ((desirePos - posAct) * 100f ) / desirePos;
	}
	
	private float tolerance (float desirePos, float percentage) {
		return (desirePos / 100f) *  percentage; 
	}
		
	public void setxPos (float x) {
		synchronized (mX) {
			if (x > 0 && x < 300 && xPosDes > 0) {			//Parrot detected and set
				if (avxPos < 0) avxPos = x;					//Filter set
				avxPos = ALPF (x,avxPos,Alpha);				//Filtering Position
			} else {
				avxPos	= -1;
			}
		}
	}
	public void setyPos (float y) {
		synchronized (mY) {
			if (y > 0 && y < 300 && yPosDes > 0) {			//Parrot detected and set
				if (avyPos < 0) avyPos = y;					//Filter set
				avyPos = ALPF (y,avyPos,Alpha);				//Filtering Position
			} else {
				avyPos	= -1;
			}
		}
	}
	public void setzPos (float z) {
		synchronized (mZ) {	
			if (z > 0 && z < 300 && zPosDes > 0) {			//Parrot detected and set
				if (avzPos < 0) avzPos = z;					//Filter set
				avzPos = ALPF (z,avzPos,Alpha);				//Filtering Position
			} else {
				avzPos	= -1;
			}
		}
	}

	public float getxPos () {
		synchronized (mX) {
			return avxPos;
		}
	}
	public float getyPos () {
		synchronized (mY) {
			return avyPos;
		}
	}
	
	public float getzPos () {
		synchronized (mZ) {
			return avzPos;
		}
	}
	public void setNewxPosDes (float x,float y) {
		synchronized (mX) {
			setxPosDes (x, y);
			xPrevError = 0;
			avxPos = -1;
		}
	}
	public void setxPosDes (float x, float y) {
		synchronized (mX) {
			xPosDes		=  x;
			yxPosDes	=  y;
		}
	}
	public float[] getXPosDes () {
		synchronized (mX) {
			float[] pos = new float[2];
			pos[0] = xPosDes;
			pos[1] = yxPosDes;
			return pos;
		}
	}
	
	public void setYPos (float y) {
		synchronized (mY) {
			yPosDes = y;
			yPrevError = 0;
			avxPos = -1;
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

	public float signum (float x) {
		if (x >= 0) return 1f;
		else return -1f;
	}
	
	public float ALPF (float a, float b, float alpha) {
		return round(b + (a - b) * alpha,100);
	}
	
	public float round(float d, int decimal) {
		return (float)Math.round(d*decimal) / decimal;
	}
	
}
