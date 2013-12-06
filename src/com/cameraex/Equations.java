package com.cameraex;

import java.util.ArrayList;

import android.util.Log;


public class Equations {
	
	public ArrayList<float []> convertStrokes (ArrayList<Stroke> strokes) {
		Log.i("wut","wut");
		if (strokes == null) return null;
		Log.i("wut2","wut2");
		ArrayList<float []> equations = new ArrayList<float []>();
		Log.i("strokes","size: " + strokes.size());
		for (Stroke stroke : strokes) {
			String type = stroke.StrokeGetType();
			if (type.equals ("arc"))			equations.add(Line (5,stroke.StrokeGetArc()));
			if (type.equals ("cubicbezier"))	equations.add(BezierCubic(5,stroke.StrokeGet3Bezier()));
			if (type.equals ("line"))			equations.add(Line (5,stroke.StrokeGetLine()));
			if (type.equals ("quadraticbezier"))equations.add(BezierQuadratic (5,stroke.StrokeGetLine()));
		}
		return equations;
	}
	
	/*Returns a vector of n points*/
	private float[] Line (int n,float[] Points) {		
		float m			= (Points[3] - Points[1]) / (Points[2]  - Points[0]);	//Slope
		float dx		= (Points[2] - Points[0]) / (n - 1);					//Steps
		float [] Vec	= new float[n * 2];										//Points
		float x;
		for (int i = 0;i < n;i++) {
			x 			= (float)i * dx;
			Vec[i*2]	= x + Points[0];										//X
			Vec[i*2 + 1]= m * x + Points[1];									//Y
		}
		return Vec;
	}
	/*Returns a vector of n points*/
	private float[] BezierCubic (int n,float[] Points) {	
		//Bezier Curve equation
		//Posistions:	XC1,YC1,XC2,YC2,SX1,SY1,EX1,EY1
		//				0	1	2	3	4	5	6	7
		//Q(t)=(-P0 + 3*(P1-P2) + P3)*t^3 + 3*(P0-2*P1+P2)*t^2 + 3*(P1-P0)*t + Px0
		float Px0 = Points[4]; 					float Py0 = Points[5];
		float Px1 = Points[0]; 					float Py1 = Points[1];
		float Px2 = Points[2]; 					float Py2 = Points[3];
		float Px3 = Points[6]; 					float Py3 = Points[7];

		float cx3 = -Px0 + 3*(Px1-Px2) + Px3; 	float cy3 = -Py0 + 3*(Py1-Py2) + Py3;
		float cx2 = 3*(Px0-2*Px1+Px2);			float cy2 = 3*(Py0-2*Py1+Py2);
		float cx1 = 3*(Px1-Px0);				float cy1 = 3*(Py1-Py0);
		float cx0 = Px0; 						float cy0 = Py0;
		float dt	 = 1f / (n - 1);
		float [] Vec = new float[n * 2];
		float t;
		for (int i = 0;i < n;i++) {
			t = i * dt;
			Vec[i*2]	= ((cx3*t+cx2)*t+cx1)*t + cx0;
			Vec[i*2 + 1]= ((cy3*t+cy2)*t+cy1)*t + cy0;
		}
		return Vec;
	}
	/*Returns a vector of n points*/
	private float[] BezierQuadratic (int n,float[] Points) {	
		//Bezier Quadratic equation
		//Q(t)=(P0-2*P1+P2)*t^2 + 2*(P1-P0)*t + Px0
		float Px0 = Points[2]; 					float Py0 = Points[3];
		float Px1 = Points[0]; 					float Py1 = Points[1];
		float Px2 = Points[4]; 					float Py2 = Points[5];

		float cx2 = (Px0-2*Px1+Px2); 			float cy2 = (Py0-2*Py1+Py2);
		float cx1 = 2*(Px1-Px0);				float cy1 = 2*(Py1-Py0);
		float cx0 = Px0; 						float cy0 = Py0;
		float dt 	 = 1f / (n - 1);
		float [] Vec = new float[n * 2];
		float t;
		for (int i = 0;i < n;i++) {
			t = i * dt;
			Vec[i*2]	= (cx2*t+cx1)*t + cx0;
			Vec[i*2 + 1]= (cy2*t+cy1)*t + cy0;
		}
		return Vec;
	}
	/*Returns a vector of n points*/
	private float[] Circle (int n,float[] Points) {
		float  cx 	= Points[0];	
		float  cy 	= Points[1];
		float  r	= Points[2];
		float  R	= r*r;
		float  start	= (cx - r);
		float  dx 	= (r * 2) / (n - 1);
		float  a;
		float  x = 0;
		float [] Vec = new float[n*4];
		float sqr;
		for (int i = 0;i < n;i++) {
			x = start + i * dx;
			a = x - cx;			
			sqr = (float)Math.sqrt(Math.abs(R - a*a));
			Vec[i*2] 			 = x;
			Vec[i*2 + 1]		 = cy + sqr;
			Vec[(n*4 - 2) - i*2] = x;
			Vec[(n*4 - 1) - i*2] = cy - sqr;
		}
		return Vec;
	}
	/*Returns a vector of n points*/
	/*public float[] Ellipse (int n,float[] Points) {
		float  cx 	= Points[0];	
		float  cy 	= Points[1];
		float  rx	= Points[2];
		float  ry	= Points[3];
		float  start	= (cx - r);
		float  dx 	= (r * 2) / (n - 1);
		float  a;
		float  x = 0;
		float [] Vec = new float[n*4];
		float sqr;
		for (int i = 0;i < n;i++) {
			x = start + i * dx;
			a = x - cx;			
			sqr = (float)Math.sqrt(Math.abs(R - a*a));
			Vec[i*2] 			 = x;
			Vec[i*2 + 1]		 = cy + sqr;
			Vec[(n*4 - 2) - i*2] = x;
			Vec[(n*4 - 1) - i*2] = cy - sqr;
		}
		return Vec;
	}*/
}
