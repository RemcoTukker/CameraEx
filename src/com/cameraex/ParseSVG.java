package com.cameraex;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class ParseSVG {

	private ArrayList<Stroke> 	strokes = null;	
	private Stroke stroke	= null;
	
	public ArrayList<Stroke> ParseSVGStart (InputStream Archive) {
		try {
			XmlPullParserFactory pullParserFactory;
			pullParserFactory 		= XmlPullParserFactory.newInstance();
			XmlPullParser parser 	= pullParserFactory.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(Archive, null);
			ParseSVGAnalyze(parser);
			return strokes;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}		
	}
	
	public void ParseSVGDestroy () {
		strokes.clear();
		strokes = null;
		stroke = null;
	}
	
	private void ParseSVGAnalyze(XmlPullParser parser) throws XmlPullParserException,IOException{
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT){
			switch (eventType){
				case XmlPullParser.START_DOCUMENT:
					strokes = new ArrayList<Stroke>();
					break;
				case XmlPullParser.START_TAG:
					ParseSVGNameToClass (parser);
					break;
				case XmlPullParser.END_TAG:
					if (stroke != null) {
						if (parser.getName().equals(stroke.StrokeGetType())) {
							strokes.add(stroke);
						}
						stroke = null;
					}
				   	break;
				case XmlPullParser.TEXT:
					break;
				default:
					break;
			}
			eventType = parser.next();
		}

	}
	
	private Boolean ParseSVGNameToClass (XmlPullParser parser) {
		String Name = parser.getName();
		if (Name.equals ("ellipse")) {
			return ParseSVGNewEllipse (parser);
		}else if (Name.equals("line")) {
			return ParseSVGNewLine (parser);
		}else if (Name.equals("path")) {
			return ParseSVGNewPath (parser);
		}else if (Name.equals("polygon")) {
			return ParseSVGNewPolygon (parser);
		}else if (Name.equals("rect")) {
			return ParseSVGNewRectangle (parser);
		}
		return false;
	}
	
	private Boolean ParseSVGNewEllipse (XmlPullParser parser) {
		try{
			stroke = new Stroke ();
			float CX = Float.parseFloat(parser.getAttributeValue(3));
			float CY = Float.parseFloat(parser.getAttributeValue(4));
			float RX = Float.parseFloat(parser.getAttributeValue(5));
			float RY = Float.parseFloat(parser.getAttributeValue(6));
			stroke.StrokeSetEllipse(CX, CY, RX, RY, parser.getName());
			return true;
		} catch (Exception e) {return false;}
	}
	
	private Boolean ParseSVGNewLine (XmlPullParser parser) {
		try{
			stroke = new Stroke ();
			float X1 = Float.parseFloat(parser.getAttributeValue(3));
			float Y1 = Float.parseFloat(parser.getAttributeValue(4));
			float X2 = Float.parseFloat(parser.getAttributeValue(5));
			float Y2 = Float.parseFloat(parser.getAttributeValue(6));
			stroke.StrokeSetLine(X1, Y1, X2, Y2, parser.getName());
			return true;
		} catch (Exception e) {return false;} 
	}
	
	private Boolean ParseSVGNewPath (XmlPullParser parser) {

		try{
			String Path = parser.getAttributeValue(3);
			float 	initialPosX = 0;
			float 	initialPosY = 0;
			float 	mirrorConX	= 0;
			float 	mirrorConY	= 0;
			float[] points = new float[9];
			int 	State = 0;
			char	type = 0;
			Boolean newPoint = true;
			StringBuilder rawCoordinate = new StringBuilder ();
			for (int i = 0;i < Path.length();i++) {
				if (Path.charAt(i) > 64) {
					if (i > 0) {
						points[State] = Float.parseFloat(rawCoordinate.toString());
						rawCoordinate.delete(0, rawCoordinate.length());
						newPoint 	= true;
					}
				} else if (Path.charAt(i) != '-' && Path.charAt(i) != ',') {
					rawCoordinate.append(Path.charAt(i));
					if ( i < Path.length () - 1) {
						newPoint = false;	
					} else {
						points[State] = Float.parseFloat(rawCoordinate.toString());
						rawCoordinate.delete(0, rawCoordinate.length());
						newPoint 	= true;
					}
				} else if (newPoint) {
					rawCoordinate.append(Path.charAt(i));
					newPoint = false;
				} else {
					points[State] = Float.parseFloat(rawCoordinate.toString());
					rawCoordinate.delete(0, rawCoordinate.length());
					if (Path.charAt(i) == '-') {
						rawCoordinate.append(Path.charAt(i));
					}
					newPoint = true;
				}
				if (newPoint) {
					switch (type) {
						case 'a':
							//Arc
							//Lower case --> Reference previous point
							//Input: 	XR1,YR2,XAR,LAC,SWP,EX1,EY1
							//Output:	XR1,YR2,XAR,LAC,SWP,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5	6	7	8
							if (State == 6) {
								points[7] 	= initialPosX + points[5];		points[8] 	= initialPosY + points[6];		//End
								points[5] 	= initialPosX;					points[6] 	= initialPosY;					//Start
								initialPosX = points[7];					initialPosY = points[8];
								type = 1;
								Log.i("arc","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ " " + points[5]+ " " + points[6]+ " " + points[7] + points[8]);
							}
							break;
						case 'A':
							//Arc
							//Upper case --> Reference top left
							//Input: 	XR1,YR2,XAR,LAC,SWP,EX1,EY1
							//Output:	XR1,YR2,XAR,LAC,SWP,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5	6	7	8
							if (State == 6) {
								points[7] 	= points[5];					points[8] 	= points[6];		//End
								points[5] 	= initialPosX;					points[6] 	= initialPosY;		//Start
								initialPosX = points[7];					initialPosY = points[8];
								type = 1;
								Log.i("ARC","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ " " + points[5]+ " " + points[6]+ " " + points[7] + points[8]);
							}
							break;
						case 'c':
							//Cubic Bezier Curve
							//Lower case --> Reference previous point
							//Input: 	XC1,YC1,XC2,YC2,EX1,EY1
							//Output:	XC1,YC1,XC2,YC2,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5	6	7
							if (State == 5) {
								points[6] 	= initialPosX + points[4];		points[7] 	= initialPosY + points[5];	//End
								points[4] 	= initialPosX;					points[5] 	= initialPosY;				//Start
								points[2]	= initialPosX + points[2];		points[3]	= initialPosY + points[3];	//Control2
								points[0]	= initialPosX + points[0];		points[1]	= initialPosY + points[1];	//Control1
								initialPosX = points[6];					initialPosY = points[7];
								mirrorConX	= points[2];					mirrorConY	= points[3];
								type 		= 2;
								Log.i("cubicbezier","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ " " + points[5]+ " " + points[6]+ " " + points[7]);
							}
							break;
						case 'C':
							//Cubic Bezier Curve
							//Upper case --> Reference top left
							//Input: 	XC1,YC1,XC2,YC2,EX1,EY1
							//Output:	XC1,YC1,XC2,YC2,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5	6	7
							if (State == 5) {
								points[6] 	= points[4];					points[7] 	= points[5];	//End
								points[4] 	= initialPosX;					points[5] 	= initialPosY;	//Start
								initialPosX = points[6];					initialPosY = points[7];
								mirrorConX	= points[2];					mirrorConY	= points[3];
								type 		= 2;
								Log.i("CUBICBEZIER","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ "," + points[5]+ " " + points[6]+ "," + points[7]);
							}
							break;
						case 'h':
							//Horizontal Line
							//Lower case --> Reference previous point
							//Input: 	XE1,YE1
							//Output:	SX1,SY1,EX1,EY1
							//			0	1	2	3
							if (State == 1) {
								points[2] 	= initialPosX + points[0];		points[3] 	= initialPosY;	//End
								points[0] 	= initialPosX;					points[1] 	= initialPosY;	//Start
								initialPosX = points[2];
								type 		= 3;
								Log.i("lineh","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'H':
							//Horizontal Line
							//Upper case --> Reference top left
							//Input: 	XE1,YE1
							//Output:	SX1,SY1,EX1,EY1
							//			0	1	2	3
							if (State == 0) {
								points[2] 	= points[0];					points[3] 	= initialPosY;	//End
								points[0] 	= initialPosX; 					points[1] 	= initialPosY;	//Start
								initialPosX = points[2];
								type 		= 3;
								Log.i("LINEH","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'l':
							//Line
							//Lower case --> Reference previous point
							//Input: 	XE1,YE1
							//Output:	SX1,SY1,EX1,EY1
							//			0	1	2	3
							if (State == 1) {
								points[2] 	= initialPosX + points[0];		points[3] 	= initialPosY + points[1];	//End
								points[0] 	= initialPosX; 					points[1] 	= initialPosY;				//Start
								initialPosX = points[2];					initialPosY = points[3];
								type 		= 3;
								Log.i("line","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'L':
							//Line
							//Upper case --> Reference top left
							//Input: 	XE1,YE1
							//Output:	SX1,SY1,EX1,EY1
							//			0	1	2	3
							if (State == 0) {
								points[2] 	= points[0];					points[3] 	= points[1];	//End
								points[0] 	= initialPosX; 					points[1] 	= initialPosY;	//Start
								initialPosX = points[2];					initialPosY = points[3];
								type 		= 3;
								Log.i("LINE","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'm':
							if (State == 1) {
								initialPosX = points[0];					initialPosY = points[1];
								type 		= 0;
								Log.i("M","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'M':	
							//Start of the stroke
							//Upper case --> Reference top left document
							//----------- Example ----------//
							//Start position	 00x ,  00y	//
							//M info			-27x ,  25y	//
							//Start position	-27x ,  25y	//
							//------------------------------//
							if (State == 1) {
								initialPosX = points[0];					initialPosY = points[1];
								type 		= 0;
								Log.i("M","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'q':
							//Quadratic Bezier Curve
							//Lower case --> Reference previous point
							//Input: 	XC1,XC2,XE1,YE1
							//Output:	XC1,YC1,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5
							if (State == 3) {
								points[4]	= initialPosX + points[2];		points[5] 	= initialPosY + points[3];	//End
								points[2]	= initialPosX;					points[3] 	= initialPosY;				//Start
								points[0]	= initialPosX + points[0];		points[1] 	= initialPosY + points[1];	//Control
								initialPosX = points[4];					initialPosY = points[5];
								mirrorConX	= points[0];					mirrorConY	= points[1];
								type 		= 4;
								
								Log.i("quadraticbezier","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ "," + points[5]);
							}
							break;
						case 'Q':
							//Quadratic Bezier Curve
							//Upper case --> Reference top left document
							//Input: 	XC1,XC2,XE1,YE1
							//Output:	XC1,YC1,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5
							if (State == 3) {
								points[4] 	= points[2];					points[5] 	= points[3];	//End
								points[2] 	= initialPosX;					points[3] 	= initialPosY;	//Start
								initialPosX = points[4];					initialPosY = points[5];	
								mirrorConX	= points[0];					mirrorConY	= points[1];
								type 		= 4;
								Log.i("QUADRATICBEZIER","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ "," + points[5]);
							}
							break;
						case 's':
							//Cubic Bezier Curve Mirror
							//Lower case --> Reference previous point
							//Input: 	XC2,YC2,EX1,EY1
							//Output:	XC1,YC1,XC2,YC2,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5	6	7
							if (State == 3) {
								points[6] 	= initialPosX + points[2];		points[7] 	= initialPosY + points[3];		//End
								points[4] 	= initialPosX;					points[5] 	= initialPosY;					//Start
								points[2]	= initialPosX + points[0];		points[3]	= initialPosY + points[1];		//Control2
								points[0]	= initialPosX * 2 - mirrorConX;	points[1]	= initialPosY * 2 - mirrorConY;	//Control1
								initialPosX = points[6];					initialPosY = points[7];
								mirrorConX	= points[2];					mirrorConY	= points[3];
								type 		= 2;
								Log.i("cubicbezierMirror","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ "," + points[5]+ " " + points[6]+ "," + points[7]);
							}
							break;
						case 'S':
							//Cubic Bezier Curve Mirror
							//Upper case --> Reference top left document
							//Input: 	XC2,YC2,EX1,EY1
							//Output:	XC1,YC1,XC2,YC2,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5	6	7
							if (State == 3) {
								points[6] 	= points[2];					points[7] 	= points[3];					//End
								points[4] 	= initialPosX;					points[5] 	= initialPosY;					//Start
								points[2]	= points[0];					points[3]	= points[1];					//Control2
								points[0]	= initialPosX * 2 - mirrorConX;	points[1]	= initialPosY * 2 - mirrorConY;	//Control1
								initialPosX = points[6];					initialPosY = points[7];
								mirrorConX	= points[2];					mirrorConY	= points[3];
								type 		= 2;
								Log.i("BCUBICBEZIERMirror","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ "," + points[5]+ " " + points[6]+ "," + points[7]);
							}
							break;
						case 't':
							//Quadratic Bezier Curve Mirror
							//Lower case --> Reference previous point
							//Input: 	EX1,EY1
							//Output:	XC1,YC1,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5
							if (State == 1) {
								points[4] 	= initialPosX + points[0];			points[5] 	= initialPosY + points[1];		//End
								points[2] 	= initialPosX;						points[3] 	= initialPosY;					//Start
								points[0]	= initialPosX * 2 - mirrorConX;		points[1]	= initialPosY * 2 - mirrorConY;	//Control1						
								initialPosX = points[4];						initialPosY = points[5];
								mirrorConX	= points[0];						mirrorConY	= points[1];
								type 		= 4;
								Log.i("quabzrmirror","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ "," + points[5]);
							}
							break;
						case 'T':
							//Quadratic Bezier Curve Mirror
							//Upper case --> Reference top left document
							//Input: 	EX1,EY1
							//Output:	XC1,YC1,SX1,SY1,EX1,EY1
							//			0	1	2	3	4	5
							if (State == 1) {
								points[4] 	= points[0];						points[5] 	= points[1];					//End
								points[2] 	= initialPosX;						points[3] 	= initialPosY;					//Start
								points[0]	= initialPosX * 2 - mirrorConX;		points[1]	= initialPosY * 2 - mirrorConY;	//Control1
								initialPosX = points[4];						initialPosY = points[5];
								mirrorConX	= points[0];						mirrorConY	= points[1];
								type 		= 4;
								Log.i("QUABZMIRROR","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]+ " " + points[4]+ "," + points[5]);
							}
							break;
						case 'v':
							//Vertical Line
							//Lower case --> Reference previous point
							//Input: 	XE1,YE1
							//Output:	SX1,SY1,EX1,EY1
							//			0	1	2	3
							if (State == 1) {
								points[2] 	= initialPosX;					points[3] 	= initialPosY + points[0];	//End
								points[0] 	= initialPosX; 					points[1] 	= initialPosY;				//Start
																			initialPosY = points[3];
								type 		= 3;
								Log.i("linev","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'V':
							//Vertical Line
							//Upper case --> Reference top left
							//Input: 	XE1,YE1
							//Output:	SX1,SY1,EX1,EY1
							//			0	1	2	3
							if (State == 0) {
								points[2] 	= initialPosX;					points[3] 	= points[0];	//End
								points[0] 	= initialPosX; 					points[1] 	= initialPosY;	//Start
																			initialPosY = points[3];
								type 		= 3;
								Log.i("LINEV","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'z':
							//"Line"
							//Lower case --> Reference previous point
							//Input: 	XE1,YE1
							//Output:	SX1,SY1,EX1,EY1
							//			0	1	2	3
							if (State == 1) {
								points[2] 	= initialPosX + points[0];		points[3] 	= initialPosY + points[1];	//End
								points[0] 	= initialPosX; 					points[1] 	= initialPosY;				//Start
								initialPosX = points[2];					initialPosY = points[3];
								type 		= 3;
								Log.i("line","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						case 'Z':
							//"Line"
							//Upper case --> Reference top left
							//Input: 	XE1,YE1
							//Output:	SX1,SY1,EX1,EY1
							//			0	1	2	3
							if (State == 0) {
								points[2] 	= points[0];					points[3] 	= points[1];	//End
								points[0] 	= initialPosX; 					points[1] 	= initialPosY;	//Start
								initialPosX = points[2];					initialPosY = points[3];
								type 		= 3;
								Log.i("LINE","POINTS: " + points[0] + "," + points[1]+ " " + points[2]+ "," + points[3]);
							}
							break;
						default:
							break;
					}
					if (type != Path.charAt(i) && Path.charAt(i) != '-' && Path.charAt(i) != ',') {
						ParseSVGTypeToStroke (points,type);
						type 	= Path.charAt(i);
						State 	= 0;
					}else{
						State++;		//Points
					}
				}
			}
			return true;
		} catch (Exception e) {return false;}

	}
	/*Converting points and types in primitives coordinates*/
	private Boolean ParseSVGTypeToStroke (float[] Points, char Type) {
		switch (Type) {
			case 1:
				stroke = new Stroke ();
				stroke.StrokeSetArc(Points[0], Points[1], Points[2], Points[3], Points[4], Points[5], Points[6], Points[7], Points [8], "arc");
				strokes.add(stroke);
				break;
			case 2:
				stroke = new Stroke ();
				stroke.StrokeSet3Bezier(Points[0], Points[1], Points[2], Points[3], Points[4], Points[5], Points[6], Points[7], "cubicbezier");
				strokes.add(stroke);
				break;
			case 3:
				stroke = new Stroke ();
				stroke.StrokeSetLine(Points[0], Points[1], Points[2], Points[3], "line");
				strokes.add(stroke);
				break;
			case 4:
				stroke = new Stroke ();
				stroke.StrokeSet2Bezier(Points[0], Points[1], Points[2], Points[3], Points[4], Points[5], "quadraticbezier");
				strokes.add(stroke);
				break;
			default:
				break;
		}
		return true;
	}
	
	private Boolean ParseSVGNewPolygon (XmlPullParser parser) {
		try{
			stroke = new Stroke ();
			StringTokenizer  rawPoints = new StringTokenizer(parser.getAttributeValue(3)," ");
			float[] Points = new float[rawPoints.countTokens()*2];
			int i = 0; 
			while(rawPoints.hasMoreTokens()){
				StringTokenizer  rawCoordinate = new StringTokenizer(rawPoints.nextToken(),",");
				while(rawCoordinate.hasMoreTokens()){
					Points[i++] = Float.parseFloat(rawCoordinate.nextToken());
				}
			}
			stroke.StrokeSetPolygon(Points,parser.getName());
			return true;
		} catch (Exception e) {return false;}
	}
	
	private Boolean ParseSVGNewRectangle (XmlPullParser parser) {
		stroke = new Stroke ();
		try{
			float X = Float.parseFloat(parser.getAttributeValue(0));
			float Y = Float.parseFloat(parser.getAttributeValue(1));
			float Width = Float.parseFloat(parser.getAttributeValue(5));
			float Height = Float.parseFloat(parser.getAttributeValue(6));
			stroke.StrokeSetRectangle(X, Y, Width, Height, parser.getName());
			return true;
		} catch (Exception e) {return false;} 
	}
}

