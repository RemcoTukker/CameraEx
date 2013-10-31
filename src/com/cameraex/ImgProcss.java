package com.cameraex;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
/*
* nexus 4
* focal length :  (4.6 mm)
* opening: f/2.4
* sensor: 1/3.2" --> d = 4.6mm w = 3.7mm h = 2.8mm
* vision angle = 2 * arctan (3.7 /2*4.6) = 52.53 degrees ///// 49
*/

public class ImgProcss extends Thread { 

	/*Flags*/
	private volatile  boolean running = true;
	

    private LinkedList<int[]> wGClusters;
    private LinkedList<int[]> wBClusters; 
    private LinkedList<int[]> wKClusters; 
    private DrawView mDrawView;
    private PID mPID;
    volatile private  byte[] mData;
    volatile private float pXcm = -1;
    volatile private Bitmap mBitmap;
    volatile private int mCamResW,mCamResH,N;
    final private float sensorH = 2.8f, fL2 = 9.2f, h1 = 24.49f/2;
	
	private Object mPauseLock = new Object();  
	private boolean mPaused = true;
    
	public ImgProcss (DrawView drawView, PID pid) {
		wGClusters 	= new LinkedList<int[]>();
		wBClusters 	= new LinkedList<int[]>();
		wKClusters 	= new LinkedList<int[]>();
		mDrawView   = drawView;
		mPID		= pid;
		mPaused 	= true;
	}
	
	public void Set (byte[] data,int resW,int resH,int n) {
		mData = data;
		wGClusters.clear();
		wBClusters.clear();
		wKClusters.clear();
		mCamResW = resW;
		mCamResH = resH;
		N = n;
		running = true;
	}
	
	public void Stop () {
		running = false;
	}
	
	public void onPause() {
		synchronized (mPauseLock) {
			mPaused = true;
		}
	}

	public void onResume () {
		synchronized (mPauseLock) {
			mPaused = false;
			mDrawView.invalidate();
			mPauseLock.notifyAll();
		}
	}
	public boolean paused () {
		return mPaused; 
	}
	public void run() {
		
		while (running) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			YuvImage yuv = new YuvImage(mData, ImageFormat.NV21, mCamResW, mCamResH, null);
			mData = null;
			yuv.compressToJpeg(new Rect(0, 0, mCamResW, mCamResH), 100, out);
			byte[] bytes = out.toByteArray();
			mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			mBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
			bytes = null;
			Sampling(2);
			this.onPause();
			synchronized (mPauseLock) {
				while (mPaused) {
					try {
						mPauseLock.wait();
					} catch (InterruptedException e) {}
				}
			}	
		}
		Log.i("END THREAD","IMGPROC");

		
	}
	
	private void Sampling (int wdwSize) {
		 int i, samples, samplesY, samplesX,x=0,y=0,nX,nY,nI,samplesY2, samplesX2;
		 int[] colors = new int[3];
		 double height,width;
		 
	
		height = mBitmap.getHeight(); width = mBitmap.getWidth();
	//	 mDrawView.mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		samples  = (int)Math.ceil((height * width) / N);
		samplesX = (int)Math.ceil(width / N); samplesX2 = samplesX -1;
		samplesY = (int)Math.ceil(height / N); samplesY2 = samplesY -1;
		y = 0;
		 
		for (i = 1;i < samples;i++) {
			getPixel(x,y,mBitmap,colors);			 
			if (isChecked (colors[0],colors[1],colors[2])){
			} else if (isGreen(colors[0],colors[1],colors[2])) {
				nX = x/N; nY = y/N;
				findAdyacentK (nX,nY,samplesX2, samplesY2,wdwSize,wGClusters,wKClusters);
			} else if (isBlue(colors[0],colors[1],colors[2])) {
				nX = x/N; nY = y/N;
				findAdyacent (nX,nY,samplesX2, samplesY2,wdwSize,wBClusters);
			}
			if (i%samplesX != 0) { x += N; } else { x = 0;y += N; i += (N - 1)*samplesX; }
		}
		selectClusters(wKClusters,wGClusters);
		mBitmap.recycle();

		synchronized(mDrawView.gRectangles) {
			if (mDrawView.gRectangles != null) {
				mDrawView.gRectangles.clear();
				for (i = 0;i < wGClusters.size();i++) {
					 mDrawView.gRectangles.add(new Rect(wGClusters.get(i)[0]*N*3,wGClusters.get(i)[1]*N*3,wGClusters.get(i)[2]*N*3,wGClusters.get(i)[3]*N*3));
				}
			}
		}
		synchronized(mDrawView.bRectangles) {
			if (mDrawView.bRectangles != null) {
				mDrawView.bRectangles.clear();
				for (i = 0;i < wBClusters.size();i++) {
					mDrawView.bRectangles.add(new Rect(wBClusters.get(i)[0]*N*3,wBClusters.get(i)[1]*N*3,wBClusters.get(i)[2]*N*3,wBClusters.get(i)[3]*N*3));
				}
			}
		}
		synchronized(mDrawView.kRectangles) {
			if (mDrawView.kRectangles != null) {
				mDrawView.kRectangles.clear();
				for (i = 0;i < wKClusters.size();i++) {
					mDrawView.kRectangles.add(new Rect(wKClusters.get(i)[0]*N*3,wKClusters.get(i)[1]*N*3,wKClusters.get(i)[2]*N*3,wKClusters.get(i)[3]*N*3));
				}
			}
		}
		pXcm = pixelsXcm();
		mPID.xPosAct = xPositionCm ();
		mPID.yPosAct = yPositionCm ();
		mPID.zPosAct = zPositionCm ();
		mPID.runPID(isRotating ());
		// Log.i("--------------------","--------------------");
    
    }
    private void getPixel (int x, int y,Bitmap bitmap,int[] colors) {
    	int pixel;
		pixel =  bitmap.getPixel(x, y);
		colors[0] = (pixel>>16)&0xFF; colors[1] = (pixel>>8)&0xFF; colors[2] = (pixel)&0xFF;
    }
    private void setPixel (int x, int y, Bitmap bitmap) {
		bitmap.setPixel(x,y,Color.WHITE);
    }
    
	private boolean isChecked (int R, int G, int B) {

		 if (R != 255 || G != 255 || B != 255) return false;
		 return true;
	}
    private boolean isRed (int R, int G, int B) {
		 if (R > G && R > B) {
			 if (R > 50) {
			     if (((float)G+(float)B) / ((float)R*2) < 0.75 ) {
			    	 return true;
			     }
			 }
		 }
		 return false;
    }
	private boolean isGreen (int R, int G, int B) {
		 if (G > R && G > B) {
			 if (G > 50) {
			     if (((float)R+ (float)B) / ((float)G*2) < 0.75 ) {
			    	 return true;
			     }
			 }
		 }
		 return false;
	}
	private boolean isBlue (int R, int G, int B) {

		if (B > R && B > G) {
			if (B > 100) {
				if (((float)R+(float)G) / ((float)B*2) < 0.75 ) {
					 return true;
				}
			}
		 }
		 return false;
	}
	private boolean isBlack (int R, int G, int B) {
		float r,g,b;
		
		r = 0.241f*R; g = 0.691f*G; b = 0.068f*B;	
		if (Math.sqrt(r*r + g*g + b*b ) < 30) return true;
		return false;
	}
	private boolean about (int R, int G, int B) {
		float a,b,c,max;
		
		if (R > G) {
			if (R > B) { max = R; } else { max = B; }
		} else if (G > B) { max = G; } else { max = B; }
		
		if (R > G) a = (float)G / (float)R; else a = (float)R / (float)G;
		if (a < 0.8) return false;
		if (R > B) b = (float)B / (float)R; else b = (float)R / (float)B;
		if (b < 0.8) return false;
		if (G > B) c = (float)B / (float)G; else c = (float)G / (float)B;
		if (c < 0.8) return false;
		return true;
	}
	private boolean findAdyacent (int x,int y, int samplesX, int samplesY,int wdwSize,LinkedList<int[]> wClusters) {
		int i = 0,j = 0, posX, posY;																//Working in samples
		int[] colors = new int[3];
		
		//	x	x	x
		//	x	o	x
		//	x	x	x
		if (y > 0 && y < samplesY) {
			if (x > 0 && x < samplesX) {
				for (i = 0;i < 3;i+=1) {
					for (j = 0;j < 3;j+=1) {
						posX = (x+(i-1))*N; posY = (y+(j-1))*N;
						getPixel(posX,posY,mBitmap,colors);
						if (isRed(colors[0],colors[1],colors[2])) {
							createCluster (x, y, samplesX, samplesY, wdwSize, wClusters);
							setPixel(posX,posY,mBitmap);
							return true;
						}
					}
				}
			}		
		}
		/*if (y < samplesY) {
			if (x < samplesX) {
				for (i = 0;i < 2;i++) {
					posX = (x+1)*N; posY = (y+i)*N;
					getPixel(posX,posY,mBitmap,colors);
					if (isBlack(colors[0],colors[1],colors[2])) {
						createCluster (x, y, samplesX, samplesY, wdwSize, wClusters);
						setPixel(posX,posY,mBitmap);
						return true;
					}
				}
			}	
		}*/
		return false;
	}
	
	private boolean findAdyacentK (int x,int y, int samplesX, int samplesY,int wdwSize,LinkedList<int[]> wClusters,LinkedList<int[]> rClusters) {
		int i = 0,j = 0, posX, posY;																//Working in samples
		int[] colors = new int[3];
		boolean gCreated=false,kCreated=false;
		//	-	-	- 	-	-
		//	-	o	x	o	x
		//	-	-	x	-	x

		
		if (y > 0 && y < samplesY) {
			if (x > 0 && x < samplesX) {
				for (i = 0;i < 3;i+=1) {
					for (j = 0;j < 3;j+=1) {
						posX = (x+(i-1))*N; posY = (y+(j-1))*N;
						getPixel(posX,posY,mBitmap,colors);
						if (isBlack(colors[0],colors[1],colors[2]) && !kCreated) {
							createCluster (x, y, samplesX, samplesY, wdwSize, rClusters);
							setPixel(posX,posY,mBitmap);
							kCreated = true;
						}
						if (isRed(colors[0],colors[1],colors[2]) && !gCreated) {
							createCluster (x, y, samplesX, samplesY, wdwSize, wClusters);
							setPixel(posX,posY,mBitmap);
							gCreated = true;
						}
					}
				}
			}	
		}
		return false;
	}
	private void createCluster (int x, int y, int samplesX, int samplesY, int wdwSize,LinkedList<int[]> wClusters) {

		int [] wdw = new int[4];

		wdw[0] = x - wdwSize;
		if (wdw[0] < 0) wdw[0] = 0;
		wdw[1] = y - wdwSize;
		if (wdw[1] < 0) wdw[1] = 0;
		wdw[2] = x + wdwSize;
		if (wdw[2] > samplesX) wdw[2] = samplesX;
		wdw[3] = y + wdwSize;
		if (wdw[3] > samplesY) wdw[3] = samplesY;
		wClusters.add(wdw);
		resizeClusters (wClusters);
	}
	private void createClusterK (int x, int y, int samplesX, int samplesY, int wdwSize,LinkedList<int[]> wClusters) {

		int [] wdw = new int[4];

		wdw[0] = x - wdwSize;
		if (wdw[0] < 0) wdw[0] = 0;
		wdw[1] = y - wdwSize;
		if (wdw[1] < 0) wdw[1] = 0;
		wdw[2] = x + wdwSize;
		if (wdw[2] > samplesX) wdw[2] = samplesX;
		wdw[3] = y + wdwSize;
		if (wdw[3] > samplesY) wdw[3] = samplesY;
		wClusters.add(wdw);
	}
	
	private void selectClusters (LinkedList<int[]> wClusters,LinkedList<int[]> rClusters) {
		int i = 0, j = 0,sX1,sY1,eX1,eY1,sX2,sY2,eX2,eY2,mSY,mEY;
		int [] wdw = new int[4];
		
		if (rClusters.size() != 2) {
			wClusters.clear(); //Error
		} else {
			sY1 = rClusters.get(0)[1];  eY1 = rClusters.get(0)[3];
			sY2 = rClusters.get(1)[1];  eY2 = rClusters.get(1)[3];
			if (sY1 < sY2) mSY = sY1; else mSY = sY2;
			if (eY1 > eY2) mEY = eY1; else mEY = eY2;
			j = wClusters.size(); i = 0;
			Log.i("-------------------","---------------------");
			Log.i("mSY: " + mSY,"mEY: " + mEY);
			while (i < j) { 
				sY1 = wClusters.get(i)[1];eY1 = wClusters.get(i)[3];
				
				Log.i("sY1: " + sY1,"eY1: " + eY1);	
				if (eY1 <= mSY || sY1 >= mEY) {
					wClusters.remove(i);
					j--;i--;
					Log.i("removed! " + i,"removed! " + j);
				} else {
					wdw[0] = wClusters.get(i)[0]; wdw[2] = wClusters.get(i)[2];
					wdw[1] = mSY; wdw[3] = mEY; 
					wClusters.set(i,wdw);
				}
				i++;
			}

			Log.i("..............................",".....................");
			resizeClusters (wClusters);
		}
	}
	private void resizeClusters (LinkedList<int[]> wClusters) {
		int i = 0, j = 0,sX1,sY1,eX1,eY1,sX2,sY2,eX2,eY2;
		int [] aux = new int[4];
		boolean resizing;

		do {
			resizing = false;
			mainLoop:
			for (i = 0;i < wClusters.size();i++) {
				sX1 = wClusters.get(i)[0];sY1 = wClusters.get(i)[1];eX1 = wClusters.get(i)[2];eY1 = wClusters.get(i)[3];
				for (j = 0;j < wClusters.size();j++) {
					if (j != i) {
						sX2 = wClusters.get(j)[0];sY2 = wClusters.get(j)[1];eX2 = wClusters.get(j)[2];eY2 = wClusters.get(j)[3];
						if (eX1 >= sX2 && eX1 <= eX2) {
							if (sY1 >= sY2 && sY1 <= eY2) {
								aux[0] = sX1; aux[1] = sY2; aux[2] = eX2; aux[3] = eY1;
								wClusters.set(i, aux); wClusters.remove(j); 
								resizing = true;
								break mainLoop;
							} else if (eY1 >= sY2 && eY1 <= eY2) {
								aux[0] = sX1; aux[1] = sY1; aux[2] = eX2; aux[3] = eY2;
								wClusters.set(i, aux); wClusters.remove(j); 
								resizing = true;
								break mainLoop;
							}
						} else if (sX1 >= sX2 && sX1 <= eX2) {
							if (sY1 >= sY2 && sY1 <= eY2) {
								aux[0] = sX2; aux[1] = sY2; aux[2] = eX1; aux[3] = eY1;
								wClusters.set(i, aux); wClusters.remove(j); 
								resizing = true;
								break mainLoop;
							} else if (eY1 >= sY2 && eY1 <= eY2) {
								aux[0] = sX2; aux[1] = sY1; aux[2] = eX1; aux[3] = eY2;
								wClusters.set(i, aux); wClusters.remove(j); 
								resizing = true;
								break mainLoop;
							}
						}
					}
				}
			}
		} while (resizing);
	}
	private float pixelsXcm () {
		int cX1,cX2,cY1,cY2,x,y;
		
		if (wBClusters.size() != 2) {
			if (pXcm < 0) {
				return -1;
			}
			return pXcm;
		} else {
			cX1 = (wBClusters.get(0)[0] + wBClusters.get(0)[2]) >> 1;
			cX2 = (wBClusters.get(1)[0] + wBClusters.get(1)[2]) >> 1;
	//		cY1 = (wBClusters.get(0)[1] + wBClusters.get(0)[3]) >> 1;
	//		cY2 = (wBClusters.get(1)[1] + wBClusters.get(1)[3]) >> 1;
	//		x = (cX1 - cX2); y = (cY1 - cY2);
	//		return round((float)(Math.sqrt(x*x + y*y) * N) / 150,1000);
			//Log.i("cX1"+ cX1,"cX2: "+ cX2);
			return round((float)(Math.abs(cX1 - cX2) * N * 3) / 125,1000); 	   
		}
	}
	private float xPositionCm () {
		int cX1,cX2;
		float x;//,cY1,cY2,x,y;
		
		if (wGClusters.size() != 2) {
			return -1;
		} else {
			cX1 = (wGClusters.get(0)[0] + wGClusters.get(0)[2]) >> 1;
			cX2 = (wGClusters.get(1)[0] + wGClusters.get(1)[2]) >> 1;
			x = (cX1 + cX2) * N * 1.5f;   

			return round(x / pXcm,1000);
		}
	}
	private float zPositionCm () {
		int cY1,cY2;
		float y;//,cY1,cY2,x,y;
		
		if (wGClusters.size() != 2) {
			return -1;
		} else {
			cY1 = (wGClusters.get(0)[1] + wGClusters.get(0)[3]) >> 1;
			cY2 = (wGClusters.get(1)[1] + wGClusters.get(1)[3]) >> 1;
			y = mCamResH * 3 - ((cY1 + cY2) * N * 1.5f);   

			return round(y / pXcm,1000);
		}
	}
	private float objectSize () {
		int cX1,cX2,cY1,cY2;
		float x,y,h;
		
		if (wGClusters.size() != 2) {
			return -1;
		} else {
			cX1 = (wGClusters.get(0)[0] + wGClusters.get(0)[2]) >> 1;
			cX2 = (wGClusters.get(1)[0] + wGClusters.get(1)[2]) >> 1;			
			cY1 = (wGClusters.get(0)[1] + wGClusters.get(0)[3]) >> 1;
			cY2 = (wGClusters.get(1)[1] + wGClusters.get(1)[3]) >> 1;
			x = cX1 - cX2;
			y = cY1 - cY2;
			h = (float)Math.sqrt(x*x + y*y) * N * 3;
			Log.i("ObjectSize: " + h/pXcm,"ObjectSize: " + h/pXcm);
			return round(h / pXcm,1000);
		}
	}
	public float pixelsTocm (float pixels) {
		return round(pixels / pXcm,1000);
	}
	private float imageWSize () {
		return  pixelsTocm (mCamResW*3);
	}
	public float yPositionCm () {
		//f = F (1 + h2/h1)
		//alpha = 2*arctan(d / 2*f)
		//s1 = (h1/2) / tan (alpha/2)
		//d = 3.7 || F= 4.6 || h1 = 20-30
		//M = h2(sensor) / h1
		//w = 2arctan(h2 (sensor) / 2*sensorH(M + 1))
		float h2Sensor;
		double w;
	//	Log.i("objct size"+ objectSize(),"objct size"+ objectSize());
		h2Sensor = (objectSize() * sensorH) / imageWSize ();
		w = 2 * Math.atan(h2Sensor / (fL2 * ((h2Sensor / h1) + 1)));
		return (float)(h1 / Math.tan(w));
	}
	public int isRotating () {
		// 0 not rotating
		// -1 error
		// 1 rotating CC
		// 2 rotating CW
		int cX1,cX2,cX3;
		
		if (wKClusters.isEmpty()) return 0;
		if (wKClusters.size() > 1) return -1;
		if (wGClusters.size() != 2) return -1;
		cX1 = (wGClusters.get(0)[0] + wGClusters.get(0)[2]) >> 1;
		cX2 = (wGClusters.get(1)[0] + wGClusters.get(1)[2]) >> 1;			
		cX3 = (wGClusters.get(0)[0] + wGClusters.get(0)[2]) >> 1;
		if (cX1 < cX2) {
			if (cX3 < cX1) return 1;	
			if (cX3 > cX2) return 2;
		} else {
			if (cX3 < cX2) return 1;	
			if (cX3 > cX1) return 2;
		}
		return 0;
	}
	private float round(float d, int decimal) {
		return (float)Math.round(d*decimal) / decimal;
	}
	
}