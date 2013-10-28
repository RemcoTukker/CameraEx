package com.cameraex;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import robots.parrot.ctrl.Parrot;

import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.SystemClock;
import android.util.Log;

public class Buffers extends Thread {
	
	
	static Parrot aRDrone = null;

	private float t,dt,p1,p2;
	private int p3;
	private int figure;
	private LinkedList<params> bufParams = new LinkedList<params>();
	private Object mPauseLock = new Object();  
	private boolean mPaused,  mRunning ,bufCreated;
	private float[] squareCC = new float[] { 	0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f,
										   	   -0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f, 0.25f,-0.40f,
										   		0.00f,-0.40f, 0.00f,-0.40f, 0.00f,-0.40f, 0.00f,-0.40f, 0.00f,-0.40f, 0.00f,-0.40f,
										   		0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f,-0.25f, 0.00f };
	private float[] squareCW = new float[] { 	0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f,
												0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f,-0.25f, 0.00f,
		   	   								   	0.00f,-0.40f, 0.00f,-0.40f, 0.00f,-0.40f, 0.00f,-0.40f, 0.00f,-0.40f, 0.00f,-0.40f,
		   	   								   -0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f, 0.25f,-0.40f };
	
	public Buffers (Parrot parrot) {
        aRDrone = parrot;
		mRunning = true;
		mPaused  = false;
		bufCreated = false;
	}

	public void Set (int fig, float a, float b, int c) {
		figure = fig;
		p1 = a;
		p2 = b;
		p3 = c; 
		
		mRunning = true;
		mPaused  = false;
		bufCreated = false;
	}
	
	public void onStop () {
		mRunning = false;
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
	
	public void run() {
		
		while (mRunning) {
			dt = SystemClock.uptimeMillis() - t;
			if (dt > 150) {
				t = SystemClock.uptimeMillis(); 
				figure = (p3 & 448) >> 6;
				Log.i("figure: "+figure,"p3: "+p3);
				switch (figure) {
					case 1:
						if (line (p1,p2)) this.onPause();
					//	if (circle (p1)) this.onPause();
						break;
					case 3:
						
						break;
					case 4:
						if (square (p3)) this.onPause();
						break;
					default:
						break;						
				}
			}
			synchronized (mPauseLock) {
			    while (mPaused) {
			        try {
			            mPauseLock.wait();
			        } catch (InterruptedException e) {}
			    }
			}
		}
		Log.i("END THREAD","BUFFERS");

		
	}
	
	private boolean circle (float R) {
		if (!bufCreated) { 
			bufCircle (R);
			bufCreated = true;
		}
		if (!bufParams.isEmpty()) {
			params param = bufParams.removeFirst();
//			aRDrone.executeMoveCompose(param.vX,0,param.vZ,0);
			return false;
		} else {
			return true;
		}
	}
	
	private boolean square (int info) {
		if (!bufCreated) { 
			bufSquare (info);
			bufCreated = true;
		}
		if (!bufParams.isEmpty()) {
			params param = bufParams.removeFirst();
			aRDrone.executeMoveCompose(param.vX,0,param.vZ,0);
			return false;
		} else {
			return true;
		}
	}
	
	private boolean line (float direction, float length) {
		if (!bufCreated) { 
			bufLine (direction, length);
			bufCreated = true;
		}
		if (!bufParams.isEmpty()) {
			params param = bufParams.removeFirst();
//			aRDrone.executeMoveCompose(param.vX,0,param.vZ,0);
			return false;
		} else {
			return true;
		}
	}
	
/*	private boolean triangle (float direction, float length) {
		if (!bufCreated) { 
			bufTriangle (direction, length);
			bufCreated = true;
		}
		if (!bufParams.isEmpty()) {
			params param = bufParams.removeFirst();
//			aRDrone.executeMoveCompose(param.vX,0,param.vZ,0);
			return false;
		} else {
			return true;
		}
	}*/
	
	private void bufCircle (float R) {
		float vX,vZ, xPos, zPos,prevXPos = R, prevZPos = 0;
		bufParams.clear();
	/*	for (int i = 0;i < 360;i += 15) {
			xPos = (float)(R*Math.cos((i*Math.PI)/180));
			vX = within ((xPos - prevXPos) / 10, -1, 1); // centimiters --> (30 ms time step)
			prevXPos = xPos;
			zPos = (float)(R*Math.sin((i*Math.PI)/180));
			vZ = within ((zPos - prevZPos) / 10, -1, 1);
			prevZPos = zPos;
			params param = new params(vX,0,vZ);
			bufParams.add(param);
		}*/
		for (int i = 0;i < 360;i += 18) {
			if (i < 90) {
				vX = -0.1f; vZ = 0.3f;
			} else if (i == 90) {
				vX = -0.1f; vZ = -0.5f;
			} else if (i < 180) {
				vX = -0.1f; vZ = -0.3f;
			} else if (i == 180) {
				vX = 0.25f; vZ = -0.3f;
			} else if (i < 270) {
				vX = 0.1f; vZ = -0.3f;
			} else if (i == 270) {
				vX = 0.1f; vZ = 0.5f;
			} else {
				vX = 0.1f; vZ = 0.3f;
			}
			params param = new params(vX,0,vZ);
			bufParams.add(param);
		}
	}
	
	private void bufSquare (int info) {
		float vX,vZ;
		int direction,p,s=0;
		
		bufParams.clear();
		
		direction = info & 63;
		//--------------
		//   1 \ / 2
		//   3 / \ 4
		//--------------
		switch (direction) {
		case 10://(1 --> 2) CW
			s = 36;
			direction = 1;
			break;
		case 11://(1 --> 3) CC
			s = 0;
			direction = 0;
			break;
		case 17://(2 --> 1) CC
			s = 12;
			direction = 0;
			break;
		case 28://(3 --> 4) CC
			s = 24;
			direction = 0;
			break;
		case 34://(4 --> 2) CC
			s = 36;
			direction = 0;
			break;
		case 35://(4 --> 3) CW
			s = 12;
			direction = 1;
			break;
		case 36://(2 --> 4) CW
			s = 0;
			direction = 1;
			break;
		case 49://(3 --> 1) CW
			s = 24;
			direction = 1;
			break;
		default:
			break;
		}
		for (int i = 0;i < 23;i ++) {
			p = (i + s) % 23;
			Log.i("p: "+p,"direction: "+ direction);
			if (direction != 1) {
				vX = squareCC[p*2];
				vZ = squareCC[p*2+1];
			} else {
				vX = squareCW[p*2];
				vZ = squareCW[p*2+1];
			}
			params param = new params(vX,0,vZ);
			bufParams.add(param);
		}
	}

	private void bufLine (float direction, float length) {
		//24 commands Length --> cm to m
		float v,vX,vZ;
		
		length /= 100;
		v = length / 4f;
		bufParams.clear();
		for (int i = 0;i < 360;i += 30) {
			switch ((int)direction) {
				case 1: //Vertical
					vX = 0; 
					vZ = v + 0.35f;
					break;
				case 2: //Horizontal 1
					if ( v > 0.15) vX = v - 0.1f; else vX = 0.1f;
					vZ = 0;
					break;
				case 3: //Horizontal 2
					if ( v < -0.15) vX = -v + 0.1f; else vX = -0.08f;
					vZ = 0;
					break;
				case 4: //Diagonal 1
					if ( v < -0.15) vX = -v + 0.1f; else vX = -0.08f;
					vZ = -v - 0.35f;
					break;
				case 5: //Diagonal 2
					if ( v < -0.15) vX = -v + 0.1f; else vX = -0.08f;
					vZ = v + 0.35f;
					break;
				case 6: //Diagonal 3
					if ( v > 0.15) vX = v - 0.1f; else vX = 0.08f;
					vZ = -v - 0.35f;
					break;
				case 7: //Diagonal 4
					if ( v > 0.15) vX = v - 0.1f; else vX = 0.08f;
					vZ = v + 0.35f;
					break;
				default:
					vX = 0;
					vZ = 0;
					break;
			}
			Log.i("vX: "+vX,"vZ: " +vZ);
			params param = new params(vX,0,vZ);
			bufParams.add(param);
		}
	}
	
	
	public class params {
	    public float vX = 0;
	    public float vY = 0;
	    public float vZ = 0;
	    
	    public params (float a, float b, float c) {
	        vX = a;
	        vY = b;
	        vZ = c;
	    }
	}
	
	private float within(float x, float min, float max) {
		if (x < min) return min;
		if (x > max) return max;
		return x;
	}
	    
	
}
