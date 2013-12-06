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
	private boolean mPaused, mRunning ,bufCreated,threadRunning = false;
	//----------------		Control		----------------
	private int waitCycles;
	
	
	
	//----------------	Clockwise Defined Curves	----------------
	//						\							   /		
	//	   / \				 \			\		  /		  /			
	//	 /	   \			 /			  \		/		  \			
	// /		 \ curve1	/ curve 2	    \ / curve3	   \ curve4	
	//--------------------------------------------------------------
	private float[] curve1  = new float[] { 0.10f, 0.30f, 0f, 0.10f, 0.40f, 0f, 0.10f, 0.50f, 0f, 0.10f,-0.50f, 0f, 0.10f,-0.40f, 0f, 0.10f,-0.30f, 0f, 0.00f, 0.00f, 0f };
	private float[] curve2  = new float[] { 0.08f,-0.35f, 0f, 0.09f,-0.35f, 0f, 0.10f,-0.35f, 0f,-0.14f,-0.35f, 1f,-0.13f,-0.35f, 1f,-0.12f,-0.35f, 1f,-0.12f,-0.35f, 1f };
	private float[] curve3  = new float[] {-0.10f,-0.30f, 1f,-0.10f,-0.40f, 1f,-0.10f,-0.50f, 1f,-0.10f, 0.50f, 1f,-0.10f, 0.40f, 1f,-0.10f, 0.30f, 1f,-0.10f, 0.30f, 1f };
	private float[] curve4  = new float[] {-0.12f, 0.35f, 1f,-0.12f, 0.35f, 1f,-0.12f, 0.35f, 1f, 0.11f, 0.35f, 0f, 0.11f, 0.35f, 0f, 0.11f, 0.35f, 0f, 0.00f, 0.00f, 0f };
	private float[] curve5  = new float[] {-0.10f, 0.30f, 1f,-0.10f, 0.40f, 1f,-0.10f, 0.50f, 1f,-0.10f,-0.50f, 1f,-0.10f,-0.40f, 1f,-0.10f,-0.30f, 1f,-0.10f,-0.30f, 1f };
	private float[] curve6  = new float[] { 0.08f, 0.35f, 0f, 0.09f, 0.35f, 0f, 0.10f, 0.35f, 0f,-0.14f, 0.35f, 1f,-0.13f, 0.35f, 1f,-0.12f, 0.35f, 1f,-0.12f, 0.35f, 1f };
	private float[] curve7  = new float[] { 0.10f,-0.30f, 0f, 0.10f,-0.40f, 0f, 0.10f,-0.50f, 0f, 0.10f, 0.50f, 0f, 0.10f, 0.40f, 0f, 0.10f, 0.30f, 0f, 0.00f, 0.00f, 0f };
	private float[] curve8  = new float[] {-0.12f,-0.35f, 1f,-0.12f,-0.35f, 1f,-0.12f,-0.35f, 1f, 0.11f,-0.35f, 0f, 0.11f,-0.35f, 0f, 0.11f,-0.35f, 0f, 0.00f, 0.00f, 0f };
	//----------------------
	//		\    /			
	//		D1	D2			
	//		  \/			
	//		  /\			
	//		D3	D4			
	//		/	 \			
	//----------------------
	
	private float[] lineV  = new float[] { 0.00f, 0.45f, 0f, 0.00f, 0.45f, 0f, 0.00f, 0.45f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f };
	private float[] lineH  = new float[] { 0.12f, 0.00f, 0f, 0.12f, 0.00f, 0f, 0.12f, 0.00f, 0f, 0.12f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f };
	private float[] lineHn = new float[] {-0.12f, 0.00f, 1f,-0.11f, 0.00f, 1f,-0.11f, 0.00f, 1f,-0.11f, 0.00f, 1f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f };
	
	private float[] lineD1 = new float[] {-0.11f, 0.50f, 1f,-0.10f, 0.50f, 1f,-0.10f, 0.40f, 1f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f };
	private float[] lineD2 = new float[] { 0.11f, 0.50f, 0f, 0.10f, 0.50f, 0f, 0.10f, 0.40f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f };
	//private float[] lineD2 = new float[] {-0.10f,-0.30f, 1f,-0.10f,-0.40f, 1f,-0.10f,-0.50f, 1f,-0.10f, 0.50f, 1f,-0.10f, 0.40f, 1f,-0.10f, 0.30f, 1f,-0.10f, 0.30f, 1f };
	private float[] lineD3 = new float[] {-0.11f,-0.50f, 1f,-0.10f,-0.50f, 1f,-0.10f,-0.40f, 1f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f };
	private float[] lineD4 = new float[] { 0.11f,-0.50f, 0f, 0.10f,-0.50f, 0f, 0.10f,-0.40f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f, 0.00f, 0.00f, 0f };
	
	/*
	private float[] lineV  = new float[] { 0.00f, 0.60f, 0.00f, 0.60f, 0.00f, 0.60f, 0.00f, 0.60f, 0.00f, 0.60f, 0.00f,-0.25f};
	private float[] lineH  = new float[] { 0.12f, 0.00f, 0.12f, 0.00f, 0.12f, 0.00f, 0.12f, 0.00f, 0.11f, 0.00f,-0.10f, 0.00f};
	private float[] lineD1 = new float[] {-0.13f, 0.55f,-0.12f, 0.55f,-0.11f, 0.55f,-0.11f, 0.55f,-0.11f, 0.55f, 0.10f,-0.25f};
	private float[] lineD2 = new float[] { 0.13f, 0.60f, 0.12f, 0.60f, 0.12f, 0.60f, 0.11f, 0.60f, 0.11f, 0.60f,-0.10f,-0.25f};
	private float[] lineD3 = new float[] {-0.11f,-0.40f,-0.11f,-0.40f,-0.11f,-0.40f,-0.11f,-0.40f,-0.11f,-0.45f, 0.10f, 0.25f};
	private float[] lineD4 = new float[] { 0.13f,-0.50f, 0.12f,-0.50f, 0.12f,-0.50f, 0.11f,-0.50f, 0.11f,-0.50f,-0.10f, 0.25f};
	*/
	private ArrayList<Integer> bufferStrokes = new ArrayList<Integer>();
	
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
	public void onSet (float a, float b, float c, float d, int command, ArrayList<Integer> aS) {
		synchronized (mSet) {	
			synchronized (mPauseLock) {
				if (mPaused) {
					p1 = a; p2 = b; p3 = c; p4 = d;	//Movement description
					p5 = command;					//Command to execute
					bufferStrokes = aS;				//Array storing primitives describing the figure to draw
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
			if (dt > 240) {
				t = SystemClock.uptimeMillis(); 
				if (p5 > 0)	command = (p5 & 448) >> 6;
				else 		command = (p5 & 448) >> 6 ^ 7;
				switch (command) {
					case 1: //Position control, two commands burst is sent.
						if (movement (p1,p2,p3,p4,2)) this.onPause();
						if (t > 140) t -= 140;	// To speed up the process.
						break;
					case 5: //Drawing the desired figures using primitive buffered lines
						mPID.clearPIDEn();
						if (lines (bufferStrokes)){
							float xPos = mPID.getxPos();
							float yPos = mPID.getyPos();
							if (xPos > 0 && yPos > 0) {
								mPID.setNewxPosDes(xPos,yPos);
							}
							mPID.setPIDEn();
							this.onPause();
						}
						break;
					case 6: //Drawing the desired figures using primitive buffered curves
						float val = strokes (bufferStrokes);
						if (val == 1f){
							if (t > 50) t -= 50;
						} else if (val == 2f) {
							float xPos = mPID.getxPos();
							float yPos = mPID.getyPos();
							if (xPos > 0 && yPos > 0) {
								mPID.setNewxPosDes(xPos,yPos);
							}
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
	
	private float strokes (ArrayList<Integer> bS) {
		if (!bufCreated) {											//Creating Buffer 
			if (bS.size() != 0) {
				bufStroke (bS.remove(0));
				bufCreated = true;
				waitCycles = 0;
				mPID.clearPIDEn();
				if (mParrot != null) mMainActivity.writeServoMotor(250);
				return 0f;
			}
		}
		if (!bufParams.isEmpty()) {									//Executing Buffer
			params param = bufParams.removeFirst();
			if (mParrot != null) {
				mParrot.executeMoveCompose(param.vX,0,param.vZ,0);
			}
			Log.i("Params","X: " + param.vX + " Y: " + param.vY + " Z: "  + param.vZ + " n: " + param.n);
			return param.n;
		} else {													//Buffer Execution Completed
			if (bS.size() != 0) {
				if (++waitCycles > 4)	bufCreated = false;			//Stabilization Temporal Gap
				else					mPID.setPIDEn();
				if (waitCycles > 2){ if (mParrot != null) mMainActivity.writeServoMotor(0);}
			} else { return 2f; }
		}
		return 0f;
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
			params param = new params(vX,vY,vZ,vW,0);
			bufParams.add(param);
		}
	}
	
	private void bufLine (int info) {

		float vX=0,vZ=0,n=0;
		int direction;
		
		bufParams.clear();
		direction = info & 7;
	
		if (info < 0) {
			direction = ((direction - 1) ^ 7) + 2;
		}
		for (int i = 0;i < 7;i ++) {
			switch (direction) {
			case 1:
				vX = lineD1[i*3];
				vZ = lineD1[i*3+1];
				n  = lineD1[i*3+2];
				break;
			case 2:
				vX = lineD2[i*3];
				vZ = lineD2[i*3+1];
				n  = lineD2[i*3+2];
				break;
			case 3:
				vX = lineD3[i*3];
				vZ = lineD3[i*3+1];
				n  = lineD3[i*3+2];
				break;
			case 4:
				vX = lineD4[i*3];
				vZ = lineD4[i*3+1];
				n  = lineD4[i*3+2];
				break;
			case 5:
				vX = -lineV[i*3];
				vZ = -lineV[i*3+1];
				n  = -lineV[i*3+2];
				break;
			case 6:
				vX = lineH[i*3];
				vZ = lineH[i*3+1];
				n  = lineH[i*3+2];
				break;
			case 7:
				vX = lineV[i*3];
				vZ = lineV[i*3+1];
				n  = lineV[i*3+2];
				break;
			case 8:
				vX = lineHn[i*3];
				vZ = lineHn[i*3+1];
				n  = lineHn[i*3+2];
			default:
				vX = 0f;
				vZ = 0f;
				n  = 0f;
				break;
			}
			params param = new params(vX,0,vZ,0,n);
			bufParams.add(param);
		}
	}
	
	private void bufStroke (int info) {

		float vX=0,vZ=0,n = 0;
		int direction,arc;
		
		bufParams.clear();
		direction	= (info & 120) >> 3;	
		arc 		= (info & 7);	

		Log.i("strokes","info: "+ info + " direction: " + direction + " arc: " + arc);
		for (int i = 0;i < 7;i ++) {
			switch (direction) {
			//--------	Curves	----------
			case 0:
				switch (arc) {
				case 1:Log.i("strokes","curve 4");			vX = curve4[i*3]; vZ = curve4[i*3+1]; n = curve4[i*3+2]; break;
				case 2:Log.i("strokes","curve 4 (- +)");	vX = curve4[i*3]; vZ = curve4[i*3+1]; n = curve4[i*3+2]; break; //Not Covered	
				case 3:Log.i("strokes","curve 6 (+ -)");	vX = curve6[i*3]; vZ = curve6[i*3+1]; n = curve6[i*3+2]; break; //Not Covered
				case 4:Log.i("strokes","curve 6");			vX = curve6[i*3]; vZ = curve6[i*3+1]; n = curve6[i*3+2]; break;
				default: break;
				}
				break;
			case 1:
				switch (arc) {
				case 1:Log.i("strokes","curve 6");			vX = curve6[i*3]; vZ = curve6[i*3+1]; n = curve6[i*3+2]; break;
				case 2:Log.i("strokes","curve 6 (- +)");	vX = curve6[i*3]; vZ = curve6[i*3+1]; n = curve6[i*3+2]; break; //Not Covered
				case 3:Log.i("strokes","curve 4 (+ -)");	vX = curve4[i*3]; vZ = curve4[i*3+1]; n = curve4[i*3+2]; break; //Not Covered
				case 4:Log.i("strokes","curve 4");			vX = curve4[i*3]; vZ = curve4[i*3+1]; n = curve4[i*3+2]; break;
				default: break;
				}
				break;
			case 2:
				switch (arc) {
				case 1:Log.i("strokes","curve 2");			vX = curve2[i*3]; vZ = curve2[i*3+1]; n = curve2[i*3+2]; break;
				case 2:Log.i("strokes","curve 2 (- +)");	vX = curve2[i*3]; vZ = curve2[i*3+1]; n = curve2[i*3+2]; break; //Not Covered
				case 3:Log.i("strokes","curve 8 (+ -)");	vX = curve8[i*3]; vZ = curve8[i*3+1]; n = curve8[i*3+2]; break; //Not Covered
				case 4:Log.i("strokes","curve 8");			vX = curve8[i*3]; vZ = curve8[i*3+1]; n = curve8[i*3+2]; break;
				default: break;
				}
				break;
			case 3:
				switch (arc) {
				case 1:Log.i("strokes","curve 8");			vX = curve8[i*3]; vZ = curve8[i*3+1]; n = curve8[i*3+2]; break;
				case 2:Log.i("strokes","curve 8 (- +)");	vX = curve8[i*3]; vZ = curve8[i*3+1]; n = curve8[i*3+2]; break; //Not Covered
				case 3:Log.i("strokes","curve 2 (+ -)");	vX = curve2[i*3]; vZ = curve2[i*3+1]; n = curve2[i*3+2]; break; //Not Covered
				case 4:Log.i("strokes","curve 2");			vX = curve2[i*3]; vZ = curve2[i*3+1]; n = curve2[i*3+2]; break;
				default: break;
				}
				break;
			case 4:
				switch (arc) {
				case 1:Log.i("strokes","curve 3");			vX = curve3[i*3]; vZ = curve3[i*3+1]; n = curve3[i*3+2]; break;
				case 2:Log.i("strokes","curve 3 (- +)");	vX = curve3[i*3]; vZ = curve3[i*3+1]; n = curve3[i*3+2]; break; //Not Covered
				case 3:Log.i("strokes","curve 5 (+ -)");	vX = curve5[i*3]; vZ = curve5[i*3+1]; n = curve5[i*3+2]; break; //Not Covered
				case 4:Log.i("strokes","curve 5");			vX = curve5[i*3]; vZ = curve5[i*3+1]; n = curve5[i*3+2]; break;
				default: break;
				}
				break;
			case 5:
				switch (arc) {
				case 1:Log.i("strokes","curve 4");			vX = curve4[i*3]; vZ = curve4[i*3+1]; n = curve4[i*3+2]; break;
				case 2:Log.i("strokes","curve 4 (- +)");	vX = curve4[i*3]; vZ = curve4[i*3+1]; n = curve4[i*3+2]; break; //Not Covered
				case 3:Log.i("strokes","curve 6 (+ -)");	vX = curve6[i*3]; vZ = curve6[i*3+1]; n = curve6[i*3+2]; break; //Not Covered
				case 4:Log.i("strokes","curve 6");			vX = curve6[i*3]; vZ = curve6[i*3+1]; n = curve6[i*3+2]; break;
				default: break;
				}
				break;
			case 6:
				switch (arc) {
				case 1:Log.i("strokes","curve 7");			vX = curve7[i*3]; vZ = curve7[i*3+1]; n = curve7[i*3+2]; break;
				case 2:Log.i("strokes","curve 7 (- +)");	vX = curve7[i*3]; vZ = curve7[i*3+1]; n = curve7[i*3+2]; break; //Not Covered
				case 3:Log.i("strokes","curve 1 (+ -)");	vX = curve1[i*3]; vZ = curve1[i*3+1]; n = curve1[i*3+2]; break; //Not Covered
				case 4:Log.i("strokes","curve 1");			vX = curve1[i*3]; vZ = curve1[i*3+1]; n = curve1[i*3+2]; break;
				default: break;
				}
				break;
			case 7:
				switch (arc) {
				case 1:Log.i("strokes","curve 8");			vX = curve8[i*3]; vZ = curve8[i*3+1]; n = curve8[i*3+2]; break;
				case 2:Log.i("strokes","curve 8 (- +)");	vX = curve8[i*3]; vZ = curve8[i*3+1]; n = curve8[i*3+2]; break; //Not Covered
				case 3:Log.i("strokes","curve 2 (+ -)");	vX = curve2[i*3]; vZ = curve2[i*3+1]; n = curve2[i*3+2]; break; //Not Covered
				case 4:Log.i("strokes","curve 2");			vX = curve2[i*3]; vZ = curve2[i*3+1]; n = curve2[i*3+2]; break;
				default: break;
				}
				break;
			//--------	Lines	----------
			case 8:
				Log.i("strokes","D1");vX = lineD1[i*3];	vZ = lineD1[i*3+1];	n = lineD1[i*3+2]; break;
			case 9:
				Log.i("strokes","D2");vX = lineD2[i*3];	vZ = lineD2[i*3+1];	n = lineD2[i*3+2]; break;
			case 10:
				Log.i("strokes","D3");vX = lineD3[i*3];	vZ = lineD3[i*3+1];	n = lineD3[i*3+2]; break;
			case 11:
				Log.i("strokes","D4");vX = lineD4[i*3];	vZ = lineD4[i*3+1];	n = lineD4[i*3+2]; break;
			case 12:
				Log.i("strokes","-V");vX =-lineV[i*3];	vZ = -lineV[i*3+1];	n = lineV[i*3+2]; break;
			case 13:
				Log.i("strokes","V"); vX = lineV[i*3];	vZ =  lineV[i*3+1];	n = lineV[i*3+2]; break;
			case 14:
				Log.i("strokes","H"); vX = lineH[i*3];	vZ =  lineH[i*3+1];	n = lineH[i*3+2]; break;
			case 15:
				Log.i("strokes","Hn");vX = lineHn[i*3];	vZ = lineHn[i*3+1];	n = lineHn[i*3+2]; break;
			default:
				vX = 0;	vZ = 0;	n = 0;
				break;
			}
			params param = new params(vX,0,vZ,0,n);
			bufParams.add(param);
		}
	}
	/*
	Class storing the parrot commands 
	*/
	public class params {
		public float 	vX = 0;
		public float 	vY = 0;
		public float 	vZ = 0;
		public float 	vW = 0;
		public float	n  = 0;
	
		public params (float a, float b, float c, float d, float e) {
			vX = a;
			vY = b;
			vZ = c;
			vW = d;
			n  = e;
		}
	}
}
