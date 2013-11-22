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
* opening: f/2.
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
	volatile private int mCamResW,mCamResH,N,IS,NIS;
	volatile private int stX,stY,edX,edY,startX, startY, endX, endY;
	
	private Object mPauseLock = new Object();  
	private boolean mPaused = true, upDate = true,threadRunning = false;
	private Object mU = new Object();
	
	public ImgProcss (DrawView drawView, PID pid) {
		wGClusters	= new LinkedList<int[]>();
		wBClusters	= new LinkedList<int[]>();
		wKClusters	= new LinkedList<int[]>();
		mDrawView   = drawView;
		mPID		= pid;
		threadRunning = false;
		mPaused		= true;
		upDate		= true;
	}
	
	public void Set (byte[] data,int resW,int resH,int n,int is) {
		synchronized (mPauseLock) {
			if (mPaused) {
				mData = data;
				wGClusters.clear();
				wBClusters.clear();
				wKClusters.clear();
				mCamResW = resW;					//Camera width resolution
				mCamResH = resH;					//Camera height resolution
				N = n;								//Subsampling, Nº samples --> (width * height) / N  
				IS = is;							//Image Scale
				NIS = N * IS;
				startTread ();
			}
		}
	}
	
	public void startTread () {
		running = true;
		if (!threadRunning) this.start();
		this.onResume();
		threadRunning = true;
	}
	
	public void onStop () {
		running 		= false;
		threadRunning 	= false;
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
			yuv.compressToJpeg(new Rect(0, 0, mCamResW, mCamResH), 100, out);
			byte[] bytes = out.toByteArray();
			mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			mBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
			bytes = null;
			Sampling();
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
	
	private void Sampling () {
		int i, samples, samplesY, samplesX,x=0,y=0,samplesY2, samplesX2,startXPixels;
		int[] colors = new int[3];
		double height,width;

		height = mBitmap.getHeight(); width = mBitmap.getWidth();
	//	mDrawView.mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		samplesX = (int)Math.ceil(width / N); samplesX2 = samplesX - 1;
		samplesY = (int)Math.ceil(height / N); samplesY2 = samplesY - 1;
		
		windowSampling (9, samplesX, samplesY);											//Resizing window sampling size (in relation to the quadcopter position)
		samplesX = endX - startX;
		samplesY = endY - startY;
		samples = samplesX * samplesY;
		startXPixels = startX * N; 
		x = startXPixels;
		y = startY * N;
		for (i = 1;i < samples;i++) {
			getPixel(x,y,mBitmap,colors);			 
			if (isChecked (colors[0],colors[1],colors[2])){
			} else if (isGreen(colors[0],colors[1],colors[2])) {
				findAdyacentK (x/N,y/N,samplesX2, samplesY2, 2, wGClusters, wKClusters);
			} else if (upDate) {															//Updating pixels/Cm
				if (isBlue(colors[0],colors[1],colors[2])) {
					findAdyacent (x/N,y/N,samplesX2, samplesY2,2,wBClusters);
				}
			}
			if (i%samplesX != 0) { x += N; } else { x = startXPixels; y += N;}
		}

		selectClusters (wKClusters,wGClusters);
		mBitmap.recycle();
		/* Draw rectangles on the camera preview view*/
		drawGreenRectangles ();
		drawBlueRectangles ();
		drawBlackRectangles ();
		pXcm = pixelsXcm();

		int isRotating = isRotating ();
		if (isRotating == 0) {
			float yPos = yPositionCm ();
			float xPos = xPositionCm (yPos);
			mPID.setxPos(xPos);
			mPID.setyPos(yPos);
			mPID.setzPos(zPositionCm ());
		}
		mPID.runPID(isRotating);
	}
	/*Resize the sampling window to optimize the image processing*/
	/*If quadcopter has not been detected full screen is calculated*/
	/*If references points are not found or are being updated full screen is calculated*/
	private boolean windowSampling (int wdwSize, int samplesX, int samplesY) {
		startX = 0; startY = 0;
		endX = samplesX; endY = samplesY;

		if (upDate) return false;
		if (stX < 0 || stY < 0 || edY < 0 || edY < 0) return false;
		
		startX = stX - wdwSize;
		if (startX < 0) startX = 0;
		endX = edX + wdwSize;
		if (endX > samplesX) endX = samplesX;
		startY = stY - wdwSize;
		if (startY < 0) startY = 0;
		endY = edY + wdwSize;
		if (endY > samplesY) endY = samplesY;
		
		return true;
	}
	
	private void drawGreenRectangles () {
		synchronized(mDrawView.gRectangles) {
			if (mDrawView.gRectangles != null) {
				mDrawView.gRectangles.clear();
				for (int i = 0;i < wGClusters.size();i++) {
					 mDrawView.gRectangles.add(new Rect(wGClusters.get(i)[0]*NIS,wGClusters.get(i)[1]*NIS,wGClusters.get(i)[2]*NIS,wGClusters.get(i)[3]*NIS));
				}
			}
		}
	}
	private void drawBlueRectangles () {
		synchronized(mDrawView.bRectangles) {
			if (upDate) {
				if (mDrawView.bRectangles != null) {
					mDrawView.bRectangles.clear();
					for (int i = 0;i < wBClusters.size();i++) {
						mDrawView.bRectangles.add(new Rect(wBClusters.get(i)[0]*NIS,wBClusters.get(i)[1]*NIS,wBClusters.get(i)[2]*NIS,wBClusters.get(i)[3]*NIS));
					}
				}
			}
		}
	}
	private void drawBlackRectangles () {
		synchronized(mDrawView.kRectangles) {
			if (mDrawView.kRectangles != null) {
				mDrawView.kRectangles.clear();
				for (int i = 0;i < wKClusters.size();i++) {
					mDrawView.kRectangles.add(new Rect(wKClusters.get(i)[0]*NIS,wKClusters.get(i)[1]*NIS,wKClusters.get(i)[2]*NIS,wKClusters.get(i)[3]*NIS));
				}
			}
		}
	}
	private void getPixel (int x, int y,Bitmap bitmap,int[] colors) {
		int pixel =  bitmap.getPixel(x, y);
		colors[0] = (pixel>>16)&0xFF; colors[1] = (pixel>>8)&0xFF; colors[2] = (pixel)&0xFF;
	}
	private void setPixel (int x, int y, Bitmap bitmap) {
		bitmap.setPixel(x,y,Color.WHITE);
	}
	/*Checking if the pixels have been already calculated to save processing capacity*/ 
	private boolean isChecked (int R, int G, int B) {
		if (R != 255) return false;
		if (G != 255) return false;
		if (B != 255) return false;
		return true;
	}
	/*Appliying HUE with tolerance to know if it's red*/ 
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
	/*Appliying HUE with tolerance to know if it's green*/
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
	/*Appliying HUE with tolerance to know if it's blue*/
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
	/*Appliying one of luminance formula to determine if the pixels are black*/
	private boolean isBlack (int R, int G, int B) {
		float r,g,b;
		
		r = 0.241f*R; g = 0.691f*G; b = 0.068f*B;	
		if (Math.sqrt(r*r + g*g + b*b ) < 30) return true;
		return false;
	}
//	private boolean about (int R, int G, int B) {
//		float a,b,c,max;
//		
//		if (R > G) {
//			if (R > B) { max = R; } else { max = B; }
//		} else if (G > B) { max = G; } else { max = B; }
//		
//		if (R > G) a = (float)G / (float)R; else a = (float)R / (float)G;
//		if (a < 0.8) return false;
//		if (R > B) b = (float)B / (float)R; else b = (float)R / (float)B;
//		if (b < 0.8) return false;
//		if (G > B) c = (float)B / (float)G; else c = (float)G / (float)B;
//		if (c < 0.8) return false;
//		return true;
//	}
	/*Sampling the adjacent pixels to determine if there is some color change*/
	/*green red --> quadcopter*/
	/*blue red --> beacons*/
	/*black green --> rotation*/
	private boolean findAdyacent (int x,int y, int samplesX, int samplesY,int wdwSize,LinkedList<int[]> wClusters) {
		int i = 0,j = 0, posX, posY;																//Working in samples
		int[] colors = new int[3];
		
		//	x	-	x
		//	-	o	-
		//	x	-	x
		if (y > 0 && y < samplesY) {
			if (x > 0 && x < samplesX) {
				for (i = 0;i < 3;i+=2) {
					for (j = 0;j < 3;j+=2) {
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
		int i = 0,j = 0, posX, posY;		//Working in samples
		int[] colors = new int[3];
		//	-	-	- 	-	-
		//	-	o	x	o	x
		//	-	-	x	-	x

		if (y > 0 && y < samplesY) {
			if (x > 0 && x < samplesX) {
				for (i = 0;i < 3;i+=2) {
					for (j = 0;j < 3;j+=2) {
						posX = (x+(i-1))*N; posY = (y+(j-1))*N;
						getPixel(posX,posY,mBitmap,colors);
						if (isRed(colors[0],colors[1],colors[2])) {
							createCluster (x, y, samplesX, samplesY, wdwSize, wClusters);
							setPixel(posX,posY,mBitmap);
							return true;
						}
						if (isBlack(colors[0],colors[1],colors[2])) {
							createCluster (x, y, samplesX, samplesY, wdwSize, rClusters);
							setPixel(posX,posY,mBitmap);
							return true;
						}
					}
				}
			}	
		}
		return false;
	}
	/*If red/blue/green/back are adjacent we create a cluster of pixels storing this information*/
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
	/*Selecting only the clusters created at the same level of the quadcopter clusters*/ 
	private void selectClusters (LinkedList<int[]> wClusters,LinkedList<int[]> rClusters) {
		int i = 0, j = 0,sY1,eY1,sY2,eY2,mSY,mEY;
		int [] wdw = new int[4];
		
		if (rClusters.size() != 2) {
			wClusters.clear(); //Error
		} else {
			sY1 = rClusters.get(0)[1]; eY1 = rClusters.get(0)[3];
			sY2 = rClusters.get(1)[1]; eY2 = rClusters.get(1)[3];
			if (sY1 < sY2) mSY = sY1; else mSY = sY2;
			if (eY1 > eY2) mEY = eY1; else mEY = eY2;
			j = wClusters.size(); i = 0;
			while (i < j) { 
				sY1 = wClusters.get(i)[1];eY1 = wClusters.get(i)[3];
				
				if (eY1 <= mSY || sY1 >= mEY) {
					wClusters.remove(i);
					j--;i--;
				} else {
					wdw[0] = wClusters.get(i)[0]; wdw[2] = wClusters.get(i)[2];
					wdw[1] = mSY; wdw[3] = mEY; 
					wClusters.set(i,wdw);
				}
				i++;
			}
			resizeClusters (wClusters);
		}
	}

	/*After creating the new cluster, possible intersections of clusters are calculated and the clusters are possibly resized*/
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
	/*Calculating how many pixels in the image correspond to 1 cm in the reaity*/
	private float pixelsXcm () {
		int cX1,cX2;
		float value;
		if (wBClusters.size() != 2) {
			if (pXcm < 0) {
				return -1;
			}
			return pXcm;
		} else {
			cX1 = (wBClusters.get(0)[0] + wBClusters.get(0)[2]) >> 1;
			cX2 = (wBClusters.get(1)[0] + wBClusters.get(1)[2]) >> 1;
			synchronized (mU) { upDate = false; }
			
			value =	round((float)(Math.abs(cX1 - cX2) * NIS) / 125,1000);
			if (value > 3 && value < 5) 
			return value;
			else
			return 4;
		}
	}
	/*Calculating the position of the quadcopter in the x axis in cm*/
	private float xPositionCm (float yPos) {
		int cX1,cX2;
		float x;
		
		if (wGClusters.size() != 2) {
			return -1;
		} else {
			cX1 = (wGClusters.get(0)[0] + wGClusters.get(0)[2]) >> 1;
			cX2 = (wGClusters.get(1)[0] + wGClusters.get(1)[2]) >> 1;
			x = ((cX1 + cX2) * NIS) >> 1;
			
			float newX = (yPos * x) / 220;
			
			float[]	pos = mPID.getXPosDes();
		//	Log.i("xPosition","newX: " + newX + " pXcm: " + pXcm + " x: " + x + " yPos: " + yPos);
			mPID.setxPosDes ((yPos * pos[0]) / pos[1], yPos);
			return round(newX / pXcm,1000);
		}
	}
	/*Calculating the position of the quadcopter in the z axis in cm*/
	private float zPositionCm () {
		int cY1,cY2;
		float y;//,cY1,cY2,x,y;
		
		if (wGClusters.size() != 2) {
			return -1;
		} else {
			cY1 = (wGClusters.get(0)[1] + wGClusters.get(0)[3]) >> 1;
			cY2 = (wGClusters.get(1)[1] + wGClusters.get(1)[3]) >> 1;
			y = IS * (mCamResH - (((cY1 + cY2) * N) >> 1));   

			return round(y / pXcm,1000);
		}
	}
	/*Calculating the relative size of the quadcopter in centimiters*/
	/*Using the information calculated in this funciton we will determine afterwards the size of the sampling window*/
	private float objectSize () {
		int cX1,cX2,cY1,cY2;
		float h,x,y;
		
		if (wGClusters.size() != 2) {
			stX = -1; edX = -1; stY = -1; edY = -1;
			return - 1;
		} else {
			cX1 = (wGClusters.get(0)[0] + wGClusters.get(0)[2]) >> 1;
			cX2 = (wGClusters.get(1)[0] + wGClusters.get(1)[2]) >> 1;
			cY1 = (wGClusters.get(0)[1] + wGClusters.get(0)[3]) >> 1;
			cY2 = (wGClusters.get(1)[1] + wGClusters.get(1)[3]) >> 1;
			if (wGClusters.get(0)[0] > wGClusters.get(1)[0]) {
				stX = wGClusters.get(1)[0]; edX = wGClusters.get(0)[2];
			} else {
				stX = wGClusters.get(0)[0]; edX = wGClusters.get(1)[2];
			}
			if (wGClusters.get(0)[1] > wGClusters.get(1)[1]) {
				stY = wGClusters.get(1)[1]; edY = wGClusters.get(0)[3];
			} else {
				stY = wGClusters.get(0)[1]; edY = wGClusters.get(1)[3];
			}
			x = cX1 - cX2;
			y = cY1 - cY2;
			h = (float)Math.sqrt(x*x + y*y) * NIS;
			return round(h / pXcm,1000);
		}
	}
	/*Function to convert pixels to cm using the stored information obtained using the beacons reference*/
	public float pixelsTocm (float pixels) {
		return round(pixels / pXcm,1000);
	}
	private float imageWSize () {
		return  pixelsTocm (mCamResW * IS);
	}
	/*Caculating the position of the quadcopter in the y axis in relation with the camera, we use the projected image size in the sensor 
	lenses formula and triangles relations to obtain it*/
	public float yPositionCm () {
		//SensorH = 2.8
		//fl = 9.2
		//h1 = 24.49/2
		//f = F (1 + h2/h1)							Effective Focal Length
		//s1 = (h1/2) / tan (alpha/2)
		//d = 3.7 || F= 4.6 || h1 = 20-30
		//M = hSensor / hReal 						Magnification
		//alpha = 2*arctan(hSensor / 2*f*(M + 1))	Angle Of View
		float hSensor;
		double alpha;
	//	Log.i("objectsize",": "+objectSize ());
	//	Log.i("imageWSize",": "+imageWSize ());

		hSensor = (objectSize () * 2.8f) / imageWSize ();							//Image size projected on the sensor
		alpha = 2 * Math.atan(hSensor / (9.2f * ((hSensor / 12.445f) + 1)));		//Alpha (degrees) sen/cos
		return (float)(12.445f / Math.tan(alpha));									//Finding the distance knowing the angle and the object size
	}
	/*This function determines if the quadcopter is spinning and in which direction*/
	/*The quadcopter must be recognized properly*/
	public int isRotating () {
		// -1 error
		//  0 not rotating
		//  1 rotating CC
		//  2 rotating CW
		int cX1,cX2,cX3;
		
		if (wKClusters.isEmpty()) return 0;
		if (wKClusters.size() > 1) return -1;
		if (wGClusters.size() != 2) return -1;
		cX1 = (wGClusters.get(0)[0] + wGClusters.get(0)[2]) >> 1;		//Center green square 1
		cX2 = (wGClusters.get(1)[0] + wGClusters.get(1)[2]) >> 1;		//Center green square 2
		cX3 = (wKClusters.get(0)[0] + wKClusters.get(0)[2]) >> 1;		//Center black square 1
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
	public void setUpDate () {
		synchronized (mU) { upDate = true; }
	}
	
}