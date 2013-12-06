package com.cameraex;

public class Stroke {
	private String 	type;
	//Ellipse/Line/Rectangle/Bézier Curves
	private float 	x1 = 0;
	private float	y1 = 0;
	private float 	x2 = 0;
	private float	y2 = 0;
	private float	x3 = 0;
	private float	y3 = 0;
	private float	x4 = 0;
	private float	y4 = 0;
	private float	x5 = 0;
	//Polygon
	private float[] points;
	

	public String StrokeGetType () {
		return type;
	}
	
	public void StrokeSetArc (float XR1, float YR1, float XAR, float LAC, float SWP, float SX1, float SY1, float EX1, float EY1, String Type) {
		type = Type;
		x1 = XR1; y1 = YR1; x2 = XAR; y2 = LAC; x3 = SWP; y3 = SX1; x4 = SY1; y4 = EX1; x5 = EY1;
	}
	
	public float[] StrokeGetArc () {
		float[] points = new float[9];
		points[0] = x1; points[1] = y1; points[2] = x2; points[3] = y2; points[4] = x3; points[5] = y3; points[6] = x4; points[7] = y4; points[8] = x5;
		return points;
	}
	
	public void StrokeSet2Bezier (float XC1, float YC1, float X1, float Y1, float X2, float Y2, String Type) {
		type = Type;
		x1 = XC1; y1 = YC1; x2 = X1; y2 = Y1; x3 = X2; y3 = Y2;
	}
	
	public float[] StrokeGet2Bezier () {
		float[] points = new float[6];
		points[0] = x1; points[1] = y1; points[2] = x2; points[3] = y2; points[4] = x3; points[5] = y3;
		return points;
	}
	
	public void StrokeSet3Bezier (float XC1, float YC1, float XC2, float YC2, float X1, float Y1, float X2, float Y2, String Type) {
		type = Type;		
		x1 = XC1; y1 = YC1; x2 = XC2; y2 = YC2; x3 = X1; y3 = Y1; x4 = X2; y4 = Y2;
	}
	
	public float[] StrokeGet3Bezier () {
		float[] points = new float[8];
		points[0] = x1; points[1] = y1; points[2] = x2; points[3] = y2; points[4] = x3; points[5] = y3; points[6] = x4; points[7] = y4;
		return points;
	}
	
	public void StrokeSetCircle (float CX, float CY, float R, String Type) {
		type = Type;
		x1 = CX; y1 = CY; x2 = R;
	}
	
	public float[] StrokeGetCircle () {
		float[] points = new float[3];
		points[0] = x1; points[1] = y1; points[2] = x2;
		return points;
	}
	
	public void StrokeSetEllipse (float CX, float CY, float RX, float RY, String Type) {
		type = Type;
		x1 = CX; y1 = CY; x2 = RX; y2 = RY;
	}
	
	public float[] StrokeGetEllipse () {
		float[] points = new float[4];
		points[0] = x1; points[1] = y1; points[2] = x2; points[3] = y2;
		return points;
	}
	
	public void StrokeSetLine (float X1, float Y1, float X2, float Y2, String Type) {
		type = Type;
		x1 = X1; y1 = Y1; x2 = X2; y2 = Y2;
	}
	
	public float[] StrokeGetLine () {
		float[] points = new float[4];
		points[0] = x1; points[1] = y1; points[2] = x2; points[3] = y2;
		return points;
	}
	
	public void StrokeSetRectangle (float X, float Y, float Width, float Height, String Type) {
		type = Type;		
		x1 = X; y1 = Y; x2 = Width; y2 = Height;
	}
	
	public float[] StrokeGetRectangle () {
		float[] points = new float[4];
		points[0] = x1; points[1] = y1; points[2] = x2; points[3] = y2;
		return points;
	}
	
	public void StrokeSetPolygon (float[] Points, String Type) {
		type = Type;    		
		points = Points;
	}
	
	public float[] StrokeGetPolygon () {
		return points;
	}

}
