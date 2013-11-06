package com.cameraex;
import java.util.ArrayList;

import android.util.Log;


public class Slope {
	
	private ArrayList<Float> bufferX = new ArrayList<Float>();
	private ArrayList<Float> bufferY = new ArrayList<Float>();
	private ArrayList<Float> bufferSX = new ArrayList<Float>();
	private ArrayList<Float> bufferSY = new ArrayList<Float>();
	private ArrayList<Float> bufferAX = new ArrayList<Float>();
	private ArrayList<Float> bufferAY = new ArrayList<Float>();
	private ArrayList<Integer> bufferLines = new ArrayList<Integer>();
	
	private float prevX = 0, prevY = 0, xAcum, yAcum,inX,inY;
	private boolean stable = false, stable2 = false;
	private final int  sizeMax = 13;
	private int n;
	
	public  ArrayList<Integer> getBufferLines () {
		return bufferLines;
	}
	public void onSet (float x, float y){
		inX = x; inY = y;
		stable = false; stable2 = false;
		bufferX.clear(); bufferY.clear();
		bufferSX.clear(); bufferSY.clear();
		bufferAX.clear(); bufferAY.clear();
		bufferLines.clear();
		prevX = 0; prevY = 0;
	}

	public void update (float x, float y) {
		float actSlopeX,actSlopeY;
		
		if (x > prevX) actSlopeX = 10; else actSlopeX = -10;
		if (y > prevY) actSlopeY = 10; else actSlopeY = -10;
		//actSlopeX = x - prevX;
		//actSlopeY = y - prevY;
		xAcum += Math.abs(x - inX);
		yAcum += Math.abs(y - inY);
		n += 1;
		prevX = x; prevY = y;
		if (stable) {
			bufferX.add(actSlopeX); bufferY.add(actSlopeY);
			if (bufferX.size() >= sizeMax) {
				avSlope (x,y);
				bufferX.remove(0); bufferY.remove(0);
			}
		} else {
			stable = true;
		}
	}

	private float avSlope (float x, float y) {
		
		float slopeX = 0, slopeY = 0;
		float sSlopeX = 0, sSlopeY = 0;
		boolean update = false;
		
		for (float bX : bufferX) {
			sSlopeX += signum (bX);
			slopeX += Math.abs (bX);
		}
		for (float bY : bufferY) {
			sSlopeY += signum (bY);
			slopeY += Math.abs (bY);
		}
		slopeX = signum(sSlopeX) * (slopeX / bufferX.size());
		slopeY = signum(sSlopeY) * (slopeY / bufferY.size());
		if (stable2) {
			if (signum(bufferSX.get(bufferSX.size() - 1)) != signum(slopeX)) { 
				
				//for (Float xx : bufferSX) {
				//	Log.i("slope_x: " + xx,"fdihwpsfiow");
				//}
				//Log.i("xAcum: "+xAcum,"yAcum: "+yAcum);
				//Log.i("slopeeeeeeeeeeeeeeee","slopeeeeeeeeeeeeeeeeeeeeee");
				if (xAcum > 300 || yAcum > 300) {
					bufferSX.add(slopeX);bufferSY.add(slopeY);
					bufferAX.add(xAcum);bufferAY.add(yAcum);
					bufferLines.add(toLines(inX,inY,x,y,n));
					update = true;
				}
			}
			if (signum(bufferSY.get(bufferSY.size() - 1)) != signum(slopeY)) {
			//	for (Float xx : bufferSY) {
			//		Log.i("slope_y: " + xx,"fdihwpsfiow");
			//	}
			//	Log.i("xAcum: "+xAcum,"yAcum: "+yAcum);

			//	Log.i("aaaaaaaaaaaaaaaaaaaaaaa","aaaaaaaaaaaaaaaaaaaaaaaaaa");
				if (xAcum > 300 || yAcum > 300) {
					bufferSX.add(slopeX);bufferSY.add(slopeY);
					bufferAX.add(xAcum);bufferAY.add(yAcum);
					if (!update) {
						bufferLines.add(toLines(inX,inY,x,y,n));
						update = true;
					}
				}
			}
		} else {
			bufferSX.add(slopeX);bufferSY.add(slopeY);
			bufferAX.add(xAcum);bufferAY.add(yAcum);
			bufferLines.add(toLines(inX,inY,x,y,n));
			update = true;
			stable2 = true;
		}
		if (update) {
			xAcum = 0; yAcum = 0;n = 0;inX = x; inY = y;
		//	Log.i("บบบบบบบบบบบบบบบบบบบบบบบ","บบบบบบบบบบบบบบบบบบบบบบบ");
		//	Log.i("-----------------------","-----------------------");
		//	Log.i("บบบบบบบบบบบบบบบบบบบบบบบ","บบบบบบบบบบบบบบบบบบบบบบบ");
		}
		return 1;
	}

	public int getSlopes (float x,float y) {
		
/*		for (int i = 0;i < bufferSX.size();i++) {
			if (signum(bufferSX.get(i)) > 0) {
			Log.i ("x slope: ++","x slope: ++");
			} else {
				Log.i ("x slope: --","x slope: --");
			}
			if (signum(bufferSY.get(i)) > 0) {
				Log.i ("y slope: ++","y slope: ++");
			} else {
				Log.i ("y slope: --","y slope: --");
			}
			Log.i ("x acum: " + bufferAX.get(i),"y acum: " + bufferAY.get(i));
		}
		for (Integer c : bufferLines) {
			Log.i("object: " + c,"object: " + c);
		}
		Log.i("-----------------------","-----------------------");*/
		avSlope (x,y);
		return edges (x,y);
	}

	private int edges (float x, float y) {
		int id = 0, prevId = 0, prevId2 = 0, edges = 1, info = 0;
		float toleranceX,toleranceY;
		
	/*	for (int i = 0;i < bufferSX.size();i++) {
			if (signum(bufferSX.get(i)) < 0 && signum(bufferSY.get(i)) < 0) {
				id = 1;
			} else if (signum(bufferSX.get(i)) < 0 && signum(bufferSY.get(i)) > 0) {
				id = 2;
			} else if (signum(bufferSX.get(i)) > 0 && signum(bufferSY.get(i)) < 0) {
				id = 3;
			} else if (signum(bufferSX.get(i)) > 0 && signum(bufferSY.get(i)) > 0) {
				id = 4;
			}
			if (prevId != 0) {
				if (id != prevId) {
					toleranceX = Math.abs(bufferAX.get(i) - bufferAX.get(i-1)) / (bufferAX.get(i) + bufferAX.get(i-1));
					toleranceY = Math.abs(bufferAY.get(i) - bufferAY.get(i-1)) / (bufferAY.get(i) + bufferAY.get(i-1));
					if (prevId2 != 0) {
						if (id != prevId2) {	
						} else {
							//ERROR!
						} 
					} else {
						info |= (id << 3);
					}
					Log.i("toleranceX: "+toleranceX,"toleranceY: "+toleranceY);
					if (bufferAX.get(i) > 200 || bufferAY.get(i) > 200) {
						edges++;
					} else {
						//bufferAX.set(i,bufferAX.get(i-1));
						//bufferAY.set(i,bufferAY.get(i-1));
					}
				} else {
					//ERROR!!
				}
			} else{
				info = id;
			}
			prevId2 = prevId;
			prevId = id;
		}
		*/
		if (bufferLines.size() > 0) {
			info = 320;
		} else {
			info = 0;
		}
		/*if (edges == 1) {
			toLines(inX,inY,x,y,n);
		}
		info |= (edges << 6);
		*/
		return info;
	}
	
	private int toLines (float xi, float yi, float xo, float yo,int N) {
		float sX,sY,m;
		int edges = 1;
		
		sX = (xo - xi ) / N;
		sY = (yo - yi ) / N;
		if (sX == 0) return (int)signum(sY) * ((edges << 6) + 5);
		if (sY == 0) return (int)signum(sX) * ((edges << 6) + 6);
		m = Math.abs(sY / sX);

	/*	Log.i("--------","--------");
		Log.i("m: " + m,"m: " + m);
		Log.i("--------","--------");*/
		if (m > 0.4 && m < 5) {
			if (sX < 0 && sY < 0) return (edges << 6) + 1;
			if (sX < 0 && sY > 0) return (edges << 6) + 3;
			if (sX > 0 && sY < 0) return (edges << 6) + 2;
			if (sX > 0 && sY > 0) return (edges << 6) + 4;
		} else {
			if (m >= 5.0) return (int)signum(sY) *((edges << 6) + 5);
			if (m <= 0.4) return (int)signum(sX) *((edges << 6) + 6);
		}
		return 0;
	}
	
	private float signum (float x) {
		if (x < 0) return -1; 
		else return 1;
	}
}
