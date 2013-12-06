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
				if (xAcum > 300 || yAcum > 300) {
					bufferSX.add(slopeX);bufferSY.add(slopeY);
					bufferAX.add(xAcum);bufferAY.add(yAcum);
					bufferLines.add(toLines(inX,inY,x,y,n));
					update = true;
				}
			}
			if (signum(bufferSY.get(bufferSY.size() - 1)) != signum(slopeY)) {
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
	
	public ArrayList<Integer> strokesToPrimitives (ArrayList<Stroke> strokes) {
		if (strokes == null) return null;
		ArrayList<Integer> primitives = new ArrayList<Integer>();
		for (Stroke stroke : strokes) {
			String type = stroke.StrokeGetType();
			if (type.equals ("arc"))			primitives.add(Line (stroke.StrokeGetArc()));
			if (type.equals ("cubicbezier"))	primitives.add(BezierCubic(stroke.StrokeGet3Bezier()));
			if (type.equals ("line"))			primitives.add(Line (stroke.StrokeGetLine()));
			if (type.equals ("quadraticbezier"))primitives.add(BezierQuadratic(stroke.StrokeGet2Bezier()));
		}
		return primitives;
	}
	
	private int Line (float[] Points) {	
		return toLinesSVG (Points[0],Points[1],Points[2],Points[3]);
	}
	private int Line2 (float[] Points) {	
		return toLinesSVG (Points[2],Points[3],Points[4],Points[5]);
	}
	private int Line3 (float[] Points) {	
		return toLinesSVG (Points[4],Points[5],Points[6],Points[7]);
	}
	private int BezierCubic (float[] Points) {	
		return toCurve3SVG (Points[0],Points[1],Points[2],Points[3],Points[4],Points[5],Points[6],Points[7]);
	}
	private int BezierQuadratic (float[] Points) {	
		return toCurve2SVG (Points[0],Points[1],Points[2],Points[3],Points[4],Points[5]);
	}
	private int toLines (float xi, float yi, float xo, float yo,int N) {
		float sX,sY,m;
		int edges = 1;
		
		sX = (xo - xi ) / N;
		sY = (yo - yi ) / N;
		if (sX == 0) return (int)signum(sY) * ((edges << 6) + 5);
		if (sY == 0) return (int)signum(sX) * ((edges << 6) + 6);
		m = Math.abs(sY / sX);

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
	
	private int toLinesSVG (float xi, float yi, float xo, float yo) {
		float sX,sY;
		
		sX = (xo - xi );
		sY = (yo - yi );
		if (sX == 0) {
			if (sY > 0) return 96;			//12 v
			if (sY < 0) return 104;			//13 ^
		}
		if (sY == 0) {
			if (sX > 0) return 112;			//14 >
			if (sX < 0) return 120;			//15 <
		}
		if (sX < 0 && sY < 0) return 64;	//8
		if (sX > 0 && sY < 0) return 72;	//9
		if (sX < 0 && sY > 0) return 80;	//10
		if (sX > 0 && sY > 0) return 88;	//11
		
		return 0;
	}
	private int toCurve3SVG (float xc1, float yc1, float xc2, float yc2,float xi, float yi, float xo, float yo) {
		
		float y =0, x = 0, c1 = 0, c2 = 0;
		int direction = 0;
		//-!-!-!-!-!-!-!-!-!-!-!-!-!-!-!
		//----	Top Left Reference	----
		//-!-!-!-!-!-!-!-!-!-!-!-!-!-!-!
		
		//Main Line
		float X = (xo - xi);			
		float Y = (yo - yi);
		float K = (X * yi) - (Y * xi);
		if (X == 0 && Y != 0) {
			if (xc1 > xi) c1 = 1;		//right
			else if (xc1 < xi) c1 = -1;	//left
			if (xc2 > xi) c2 = 1;		//right
			else if (xc2 < xi) c2 = -1;	//left
		} else if (X != 0 && Y == 0) {
			if (yc1 > yi) c1 = -1;		//up
			else if (yc1 < yi) c1 = 1;	//down
			if (yc2 > yi) c2 = -1;		//up
			else if (yc2 < yi) c2 = 1;	//down
		} else if (X != 0 && Y != 0) {
			//Control Point One
			y = (Y * xc1 + K) / X;
			Log.i("tocurve3","y: " + y);
			if (yc1 > y) c1 = -1;		//up
			else if (yc1 < y) c1 = 1;	//down
			//Control Point Two
			y = (Y * xc2 + K) / X;
			Log.i("tocurve3","y: " + y);
			if (yc2 > y) c2 = -1;		//up
			else if (yc2 < y) c2 = 1;	//down
		} else {
			return -1;
		}
		Log.i("tocurve3","xi: " + xi + " xo: " + xo + " yi: " + yi + " yo: " + yo);
		Log.i("tocurve3","xc1: " + xc1 + " yc1: " + yc1 + " xc2: " + xc2 + " yc2: " + yc2);
		Log.i("tocurve3","c1: " + c1 + " c2: " + c2 + " X: " + X + " Y: " + Y + " K: " + K);
		//----- Direction ------
		//   - \ +   |   + / -  
		//     0   - 5 +  1     
		//     + \   |  / +     
		//_____4_____ ____6_____
		//     -          -     
		//       /   |  \       
		//     2   - 7 +  3     
		//   + / -   |  - \ +   
		//----------------------
		if (X < 0 && Y < 0) 		direction = 0;	//0 << 3
		else if (X < 0 && Y > 0)	direction = 16;	//2 << 3
		else if (X > 0 && Y < 0)	direction = 8;	//1 << 3  
		else if (X > 0 && Y > 0)	direction = 24;	//3 << 3
		else if (X < 0 && Y == 0)	direction = 32;	//4 << 3
		else if (X > 0 && Y == 0)	direction = 48;	//6 << 3
		else if (X == 0 && Y < 0)	direction = 40;	//6 << 3
		else if (X == 0 && Y > 0)	direction = 56;	//7 << 3
		Log.i("tocurve3","direction: " + direction + " dir>>: " + (direction>>3));
		if (c1 < 0 && c2 < 0)	return direction + 1; //down
		if (c1 < 0 && c2 > 0)	return direction + 2; //down up
		if (c1 > 0 && c2 < 0)	return direction + 3; //up down
		if (c1 > 0 && c2 > 0)	return direction + 4; //up
		if (c1 < 0 && c2 == 0)	return direction + 1; //down
		if (c1 > 0 && c2 == 0)	return direction + 4; //up
		if (c1 == 0 && c2 < 0)	return direction + 1; //down
		if (c1 == 0 && c2 > 0)	return direction + 4; //up
		
		return -1;
	}
	
	private int toCurve2SVG (float xc1, float yc1,float xi, float yi, float xo, float yo) {
		
		
		float y =0, c1 = 0;
		int direction = 0;
		//-!-!-!-!-!-!-!-!-!-!-!-!-!-!-!
		//----	Top Left Reference	----
		//-!-!-!-!-!-!-!-!-!-!-!-!-!-!-!
		
		//Main Line
		float X = (xo - xi);			
		float Y = (yo - yi);
		float K = (X * yi) - (Y * xi);
		if (X == 0 && Y != 0) {
			if (xc1 > xi) c1 = 1;		//right
			else if (xc1 < xi) c1 = -1;	//left
		} else if (X != 0 && Y == 0) {
			if (yc1 > yi) c1 = -1;		//up
			else if (yc1 < yi) c1 = 1;	//down
		} else if (X != 0 && Y != 0) {
			//Control Point One
			y = (Y * xc1 + K) / X;
			Log.i("tocurve2","y: " + y);
			if (yc1 > y) c1 = -1;		//up
			else if (yc1 < y) c1 = 1;	//down
		} else {
			return -1;
		}
		Log.i("tocurve2","xi: " + xi + " xo: " + xo + " yi: " + yi + " yo: " + yo);
		Log.i("tocurve2","xc1: " + xc1 + " yc1: " + yc1);
		Log.i("tocurve2","c1: " + c1 +" X: " + X + " Y: " + Y + " K: " + K);
		//----- Direction ------
		//   - \ +   |   + / -  
		//     0   - 5 +  1     
		//     + \   |  / +     
		//_____4_____ ____6_____
		//     -          -     
		//       /   |  \       
		//     2   - 7 +  3     
		//   + / -   |  - \ +   
		//----------------------
		if (X < 0 && Y < 0) 		direction = 0;	//0 << 3
		else if (X < 0 && Y > 0)	direction = 16;	//2 << 3
		else if (X > 0 && Y < 0)	direction = 8;	//1 << 3  
		else if (X > 0 && Y > 0)	direction = 24;	//3 << 3
		else if (X < 0 && Y == 0)	direction = 32;	//4 << 3
		else if (X > 0 && Y == 0)	direction = 48;	//6 << 3
		else if (X == 0 && Y < 0)	direction = 40;	//6 << 3
		else if (X == 0 && Y > 0)	direction = 56;	//7 << 3
		Log.i("tocurve3","direction: " + direction);
		if (c1 < 0) return direction + 1; //down
		if (c1 > 0) return direction + 4; //up		
		return -1;
	}
	private float signum (float x) {
		if (x < 0) return -1; 
		else return 1;
	}
}
