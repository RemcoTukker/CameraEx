package com.cameraex;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
	static PID	mPID = null;
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
	private float[] triangleCC = new float[] { -0.10f, 0.40f,-0.10f, 0.40f,-0.10f, 0.40f,-0.10f, 0.40f,-0.10f, 0.40f, 0.25f,-0.30f,
											   -0.10f,-0.40f,-0.10f,-0.40f,-0.10f,-0.40f,-0.10f,-0.40f,-0.10f,-0.40f, 0.25f, 0.30f,
												0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f, 0.15f, 0.00f,-0.25f, 0.00f };
	private float[] triangleCW = new float[] {  0.10f, 0.40f, 0.10f, 0.40f, 0.10f, 0.40f, 0.10f, 0.40f, 0.10f, 0.40f,-0.25f,-0.30f,
												0.10f,-0.40f, 0.10f,-0.40f, 0.10f,-0.40f, 0.10f,-0.40f, 0.10f,-0.40f,-0.25f, 0.30f,
											   -0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f,-0.15f, 0.00f, 0.25f, 0.00f };
	private float[] lineV  = new float[] {  0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f, 0.40f, 0.00f,-0.30f };
	private float[] lineH  = new float[] {  0.10f, 0.00f, 0.10f, 0.00f, 0.10f, 0.00f, 0.10f, 0.00f, 0.10f, 0.00f,-0.25f, 0.00f };
	private float[] lineD1 = new float[] { -0.10f, 0.40f,-0.10f, 0.40f,-0.10f, 0.40f,-0.10f, 0.40f,-0.10f, 0.40f, 0.25f,-0.30f };
	private float[] lineD2 = new float[] {  0.10f, 0.40f, 0.10f, 0.40f, 0.10f, 0.40f, 0.10f, 0.40f, 0.10f, 0.40f,-0.25f,-0.30f };
	private float[] lineD3 = new float[] { -0.10f,-0.40f,-0.10f,-0.40f,-0.10f,-0.40f,-0.10f,-0.40f,-0.10f,-0.40f, 0.25f, 0.30f };
	private float[] lineD4 = new float[] {  0.10f,-0.40f, 0.10f,-0.40f, 0.10f,-0.40f, 0.10f,-0.40f, 0.10f,-0.40f,-0.25f, 0.30f };

	private ArrayList<Integer> bufferLines = new ArrayList<Integer>();
	
	
	public Buffers (Parrot parrot, PID pid) {
        aRDrone = parrot;
        mPID	= pid;
		mRunning = true;
		mPaused  = false;
		bufCreated = false;
	}

	public void Set (int fig, float a, float b, int c, ArrayList<Integer> aL) {
		mPID.clearPIDEn();
		figure = fig;
		p1 = a;
		p2 = b;
		p3 = c; 
		bufferLines = aL; 
		mRunning = true;
		mPaused  = false;
		bufCreated = false;
	}
	
	public void onStop () {
		mRunning = false;
	}
	
	public void onPause() {
	    synchronized (mPauseLock) {
	    	mPID.setPIDEn();
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
			if (dt > 140) {
				t = SystemClock.uptimeMillis(); 
				if (p3 > 0) figure = (p3 & 448) >> 6;
				else figure = (p3 & 448) >> 6 ^ 7;
				Log.i("figure: "+figure,"p3: "+p3);
				switch (figure) {
					case 1:
						if (line (p3)) this.onPause();
						break;
					case 3:
						if (triangle (p3)) this.onPause();
						break;
					case 4:
						if (square (p3)) this.onPause();
						break;
					case 5:
						if (lines (bufferLines)) this.onPause();
						break;
					default:
						this.onPause();
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
			if (aRDrone != null)
			aRDrone.executeMoveCompose(param.vX,mPID.getYPID(),param.vZ,0);
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
			if (aRDrone != null)
			aRDrone.executeMoveCompose(param.vX,mPID.getYPID(),param.vZ,0);
			return false;
		} else {
			return true;
		}
	}
	
	private boolean line (int info) {
		if (!bufCreated) { 
			bufLine (info);
			bufCreated = true;
		}
		if (!bufParams.isEmpty()) {
			params param = bufParams.removeFirst();
			if (aRDrone != null)
			aRDrone.executeMoveCompose(param.vX,mPID.getYPID(),param.vZ,0);
			return false;
		} else {
			return true;
		}
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
			if (aRDrone != null)
			aRDrone.executeMoveCompose(param.vX,mPID.getYPID(),param.vZ,0);
		} else {
			if (bL.size() != 0) bufCreated = false;
			else return true;
		}
		return false;
	}
	
	private boolean triangle (int info) {
		if (!bufCreated) { 
			bufTriangle (info);
			bufCreated = true;
		}
		if (!bufParams.isEmpty()) {
			params param = bufParams.removeFirst();
			if (aRDrone != null)
			aRDrone.executeMoveCompose(param.vX,mPID.getYPID(),param.vZ,0);
			return false;
		} else {
			return true;
		}
	}
	
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

		s = 0;direction =0;
		for (int i = 0;i < 24;i ++) {
			p = (i + s) % 24;
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
	
	private void bufTriangle (int info) {
		float vX,vZ;
		int direction,p,s=0,inv = 0;
		
		bufParams.clear();
		
		direction = info & 63;
		Log.i("direction: "+direction,"esoqueso");
		//--------------
		//   1 \ / 2
		//   3 / \ 4
		//--------------
		switch (direction) {
		case 10://(1 --> 2) CW
			s = 0;
			direction = 1;
			break;
		case 11://(1 --> 3) CC
			s = 0;
			direction = 0;
			break;
		case 17://(2 --> 1) CC
			s = 24;
			direction = 0;
			break;
		case 28://(3 --> 4) CC
			s = 12;
			direction = 0;
			break;
		case 34://(4 --> 2) CC
			s = 0;
			inv = 1;
			direction = 0;
			break;
		case 35://(4 --> 3) CW
			s = 0;
			inv = 1;
			direction = 1;
			break;
		case 36://(2 --> 4) CW
			s = 0;
			direction = 1;
			break;
		case 49://(3 --> 1) CW
			s = 0;
			inv = 1;
			direction = 1;
			break;
		default:
			break;
		}
		s = 0;inv = 0;direction =0;
		for (int i = 0;i < 18;i ++) {
			p = (i + s) % 18;
			Log.i("p: "+p,"direction: "+ direction);
			if (direction != 1) {
				vX = triangleCC[p*2];
				vZ = triangleCC[p*2+1];
			} else {
				vX = triangleCW[p*2];
				vZ = triangleCW[p*2+1];
			}
			if (inv != 0) {
				vX = -vX;
				vZ = -vZ;
			}
			params param = new params(vX,0,vZ);
			bufParams.add(param);
		}
	}
	private void bufLine (int info) {

		float vX=0,vZ=0;
		int direction;
		
		bufParams.clear();
		direction = info & 7;

		Log.i("direction: " + direction,"ifhpaifgwhepif");
		if (info < 0) {
			direction = ((direction - 1) ^ 7) + 2;

			Log.i("direction: " + direction,"egwoapEADJG");
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
