package com.cameraex;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	private boolean onBluetooth = true;
	
	private Buffers mBuffers;
	
	private BluetoothCore mBluetoothCore;
	private PID mPID;
	private Slope mSlope;
	private ImgProcss mImgProcss;
	private CameraPreview mCameraPreview;
	private DrawView mDrawView;
	private ParseSVG mParseSVG;
	private Equations mEquations;
	private ArrayList<Stroke> mStrokes = null;		
	private ArrayList<Integer> primitives = new ArrayList<Integer>();


	//1280x768 --> menu = 200 --> 1080
	private int camResW=320,camResH=240,scale=3,mSH=742-camResH*scale,mSW=1080-camResW*scale;
	FrameLayout mFrameLayout;
	volatile float touched_x, touched_y;
	private boolean loaded = false;
	private TextView	angleSeekBar = null;
	private OnSeekBar	onSeekBar = null;
    private SeekBar		angleViewControl  = null;
	/* ---------------Bluetooth Constants---------------*/
	private final int	BTERROR					= 0;
//W	private final int	BTDISCOVER_FINISHED		= 1;
	private final int	BTENABLED				= 2;
	private final int	BTDISABLED				= 3;
	private final int	BTDEVICE_FOUND 			= 4;
	private final int	BTDEVICE_BOND			= 5;
	private final int	BTDEVICE_UNBOND			= 6;
	private final int	BTDEVICE_CONNECTED		= 7;
	private final int	BTDEVICE_DISCONNECTED	= 8;

	private ArrayAdapter<String>		btArrayDetected;						//Array Adapter storing Bluetooth detected devices
	private ArrayAdapter<String>		btArrayPaired;							//Array Adapter storing Bluetooth paired devices
	private ListView					listViewDetected = null;
	private ListView					listViewPaired = null;
	private ListItemOnClickPaired		listItemOnClickPaired;
	private ListItemOnLongClickPaired	listItemOnLongClickPaired;
	private ListItemOnClickDetected		listItemOnClickDetected;
	private BroadcastReceiver 			btBroadcastReceiver = null;

	private boolean bReceiverOn = false;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//size-values=3264x2448,3264x1836,3264x2176,2592x1944,2048x1536,1920x1080,1600x1200,1280x960,1280x720,720x480,800x480,640x480,352x288,320x240,176x144
		getWindow().setLayout(camResW*scale, camResH*scale);//.setLayoutParams(new FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER)
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);   

		setContentView(R.layout.activity_main);
		View view = findViewById(R.id.background);
		view.setBackgroundResource(R.drawable.bt);
		loaded = false;
		bReceiverOn = false;

	}
	

	/*------------------------------------------------------------------------*/
	/*ooooooooooooooooooo		Initializing Resources		oooooooooooooooooo*/
	/*------------------------------------------------------------------------*/
		
	protected void onResume(){
		super.onResume();
		Load();
	}
	
	public void Load(){
		if (!loaded) {
			mFrameLayout	= (FrameLayout)findViewById(R.id.frameLayout);
			mBluetoothCore	= new BluetoothCore ();
			mBluetoothCore.btCoreCreate(this);
			mBuffers		= new Buffers (this);
			mPID			= new PID (mBuffers);
			mBuffers.setPID (mPID);
			mDrawView		= new DrawView(this,camResW,camResH,scale);
			mImgProcss		= new ImgProcss (mDrawView, mPID);
			mCameraPreview	= new CameraPreview(this, mImgProcss);
			mEquations = new Equations ();			
			mSlope			= new Slope ();
			mParseSVG 		= new ParseSVG ();
			try {
				mStrokes = mParseSVG.ParseSVGStart(getAssets().open("test10.svg"));
				//mEquations.convertStrokes (mStrokes);
				primitives = mSlope.strokesToPrimitives(mStrokes);
			} catch (IOException e) {e.printStackTrace();}
			
			mFrameLayout.addView(mCameraPreview);
			mFrameLayout.addView(mDrawView);
			loaded			= true;
			
			if (mBluetoothCore != null) {
				listViewPaired 				= (ListView) findViewById(R.id.ListViewPaired);												//Associating the xml list view
				listViewDetected 			= (ListView) findViewById(R.id.ListViewDetected);											//Associating the xml list view
				btArrayPaired	 			= new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
				btArrayDetected 			= new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
				listViewPaired.setAdapter(btArrayPaired);																				//List View of all paired devices
				listViewDetected.setAdapter(btArrayDetected);																			//List View of all detected devices
				listItemOnClickDetected		= new ListItemOnClickDetected ();
				listItemOnClickPaired 		= new ListItemOnClickPaired ();
				listItemOnLongClickPaired	= new ListItemOnLongClickPaired ();
				onSeekBar					= new OnSeekBar ();
				angleSeekBar				= (TextView) findViewById(R.id.Angle);
				UpdateDeviceListView(mBluetoothCore.btCoreGetPairedDevices(),btArrayPaired);
				listViewDetected.setOnItemClickListener(listItemOnClickDetected);														//Setting listeners for the different lists
				listViewPaired.setOnItemClickListener(listItemOnClickPaired);
				listViewPaired.setOnItemLongClickListener(listItemOnLongClickPaired);
				angleViewControl = (SeekBar) findViewById(R.id.AngleControl);
				angleViewControl.setOnSeekBarChangeListener(onSeekBar);
				if (!bReceiverOn) {
					mBluetoothCore.btCoreResume();
					btBroadcastReceiver = new ActionChanged();
					registerReceiver(btBroadcastReceiver, new IntentFilter(mBluetoothCore.ACTION_BTCORE));
					bReceiverOn = true;
				}
			}
		}
		hideCameraPreview ();
	}

	/*------------------------------------------------------------------------*/
	/*ooooooooooooooooooooo		Cleaning Resources		oooooooooooooooooooooo*/
	/*------------------------------------------------------------------------*/
	
	protected void onPause() {
		super.onPause();
		clearCameraPreview ();
		clearPID ();
		clearBuffers ();
		clearImageProcessing ();
		clearBluetooth ();
		clearSVG ();
		loaded = false;
	}
	
	public void clearSVG () {
		mParseSVG.ParseSVGDestroy();
		mParseSVG = null;
		mStrokes.clear();
		mStrokes = null;
		mEquations = null;
	}
	
	public void clearImageProcessing () {
		mImgProcss = null;
	}
	
	public void clearBuffers () {
		if (mBuffers != null) {
			mBuffers.onDestroy();
			mBuffers = null;
		}
	}
	
	public void clearCameraPreview () {
		if (mCameraPreview != null){
			mCameraPreview.onPause();
			mCameraPreview = null;
		}
	}
	
	public void clearPID () {
		if (mPID != null) mPID = null;
	}
	
	public void clearBluetooth () {		
		if (mBluetoothCore != null) {
			if (bReceiverOn) {
				this.unregisterReceiver (btBroadcastReceiver);
				if (mBluetoothCore != null)
				mBluetoothCore.btCoreCloseAllConnections ();
				btArrayPaired.clear();
				btArrayPaired= null;
				btArrayDetected.clear();
				btArrayDetected = null;
				listItemOnClickDetected = null;
				listItemOnClickPaired = null;
				listItemOnLongClickPaired = null;
				onSeekBar = null;
				mBluetoothCore.btCoreStop();
				bReceiverOn = false;
			}
		}
	}
	/*------------------------------------------------------------------------*/
	/*oooooooooooooooooooooo			Buttons			oooooooooooooooooooooo*/
	/*------------------------------------------------------------------------*/
	
	/*Take Off / Land*/
	public void takeOff (View view) {
		View v = findViewById(R.id.takeOff);
		boolean on = ((ToggleButton) v).isChecked();
		if (on) {
			mBuffers.parrotTakeOff ();
		}else{
			mBuffers.parrotLand();
		}
	}
	
	public void hideCameraPreview () {
		mImgProcss = null;
		mFrameLayout.setVisibility(4); //Invisible
		mCameraPreview.onPause();
		listViewPaired.setVisibility(0);
		listViewDetected.setVisibility(0);
		
		View v = findViewById(R.id.linearLayoutBT);
		v.setVisibility(0);
		View v1 = findViewById(R.id.linearLayoutCP);
		v1.setVisibility(4);
		View v2 = findViewById(R.id.Angle);
		v2.setVisibility(0);
		View v3 = findViewById(R.id.AngleControl);
		v3.setVisibility(0);
		onBluetooth = true;
	}
	
	public void showCameraPreview () {
		mImgProcss		= new ImgProcss (mDrawView, mPID);
		mCameraPreview.onResume (mImgProcss);
		mFrameLayout.setVisibility(0); // 0 Visible || 4 Invisible
		listViewPaired.setVisibility(4);
		listViewDetected.setVisibility(4);
		View v = findViewById(R.id.linearLayoutBT);
		v.setVisibility(4);
		View v1 = findViewById(R.id.linearLayoutCP);
		v1.setVisibility(0);
		View v2 = findViewById(R.id.Angle);
		v2.setVisibility(4);
		View v3 = findViewById(R.id.AngleControl);
		v3.setVisibility(4);
		onBluetooth = false;
	}
	
	public void connectP (View view) {
		mBuffers.parrotConnect();
	}
	
	/*Changes to Bluetooth View*/
	public void toBlueTooth (View view) {
		hideCameraPreview ();
	}
	
	/*Changes to Camera Preview View*/
	public void toCameraPreview (View view) {
		showCameraPreview ();
	}
	
	/*Button to discover bluetooth devices*/
	public void DeviceSearch (View view) {
		if (mBluetoothCore != null) {
			btArrayDetected.clear();
			mBluetoothCore.btCoreDiscover ();
		}
	}
	
	/*Calls CloseSockets method*/
	public void CloseConnections (View view) {
		if (mBluetoothCore != null)
		mBluetoothCore.btCoreCloseAllConnections ();    
	}
	
	/*Updates references*/
	public void UpdateReferences (View view) {
		if (mImgProcss != null)
		mImgProcss.setUpDate();
	}
	
	/*PaintsFigure*/
	public void PaintFigure (View view) {
		mBuffers.onPause();
		if (primitives != null) {
			Log.i("primtives","sdspdk");
			ArrayList<Integer> aux = new ArrayList<Integer>();
			for(Integer item : primitives) aux.add(item);
			mBuffers.onSet(0, 0, 0, 0,384,aux);
		}
	}
	
	/*Enable Bluetooth Button*/
	public void BluetoothOnOff (View view) {
		if (mBluetoothCore != null) {
			View v = findViewById(R.id.BluetoothOnOff);
			boolean on = ((ToggleButton) v).isChecked();
			if (on) {
				mBluetoothCore.btCoreEnableBluetooth();
			} else {
				mBluetoothCore.btCoreDisableBluetooth();
				clearBluetooth ();
			}
		}
	}
	

	/*------------------------------------------------------------------------*/
	/*ooooooooooooooooooooo		Touch Screen Events		oooooooooooooooooooooo*/
	/*------------------------------------------------------------------------*/
	
	public boolean onTouchEvent(MotionEvent event) {
		if (onBluetooth) return false;					//Bluetooth View
		int eventAction = event.getAction();
		touched_x = event.getRawX();
		touched_y = event.getRawY();
		if (touched_x > 1080) touched_x = 1080;
		touched_x -= mSW;
		if (touched_x < 0) touched_x = 0;
		if (touched_y > 742) touched_y = 742;
		touched_y -= mSH;
		if (touched_y < 0) touched_y = 0;	

		switch (eventAction) {
			case MotionEvent.ACTION_DOWN:
				// finger touches the screen
				if (mSlope != null)
				mSlope.onSet(touched_x, touched_y);
				break;
			case MotionEvent.ACTION_MOVE:
				// finger moves on the screen
				if (mSlope != null)
				mSlope.update(touched_x, touched_y);
				break;
			case MotionEvent.ACTION_UP:   
				// finger leaves the screen
				if (mSlope != null && mBuffers != null)
				startBuffers (mSlope.getSlopes(touched_x, touched_y));
				break;
			default:
				break;
		}	
		return true;
	}

	public void startBuffers(int buf) {
		mBuffers.onPause();
		mBuffers.onSet(0, 0, 0, 0,buf,mSlope.getBufferLines());
	}
	
	class OnSeekBar implements OnSeekBarChangeListener {

		private int angle = 0, prevAngle = 0;

		public void onProgressChanged (SeekBar seekBar, int progress,boolean fromUser) {
			angleSeekBar.setText("Angle: " + progress + "º");
			angle = progress;
			angleSeekBar.setText("Angle: " + angle + "º");
		}
		public void onStartTrackingTouch (SeekBar seekBar) {}
		public void onStopTrackingTouch(SeekBar seekBar) {
			if (angle != prevAngle)
			writeServoMotor (angle);
			prevAngle = angle;
		}
	}
	
	/*Tries to create a bond with the selected device*/
	class ListItemOnClickDetected implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			BluetoothDevice btDevice = mBluetoothCore.btCoreGetDetectedDevice(position);
			mBluetoothCore.btCoreBond (btDevice);
		}
	}
	/*Removes the bluetooth bond with a certain device*/
	class ListItemOnClickPaired implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
			BluetoothDevice btDevice = mBluetoothCore.btCoreGetPairedDevice(position);
			if (!mBluetoothCore.btCoreConnect(btDevice)) {}
		}
	}

	/*Removes the bluetooth bond with a certain device*/
	class ListItemOnLongClickPaired implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position,long id) {
			BluetoothDevice btDevice = mBluetoothCore.btCoreGetPairedDevice(position);
			mBluetoothCore.btCoreUnBond (btDevice);
			return true;
		}
	}
	
	public void writeServoMotor (int mAngle) {
		if (mBluetoothCore != null) {
			byte[] buffer = new byte[3];
			buffer[0] = 115; //s
			buffer[1] = (byte)mAngle;
			buffer[2] = 37; //%
			ArrayList<BluetoothDevice> btDevices = mBluetoothCore.btCoreGetConnectedDevices ();
			if (btDevices != null) {
				for (int i = 0;i < btDevices.size();i++) {
					BluetoothDevice btDevice = btDevices.get (i);
					mBluetoothCore.btCoreSendMessage(buffer, btDevice);
				}
			}
		}
	}
	
	private class ActionChanged extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String Action = intent.getAction();
			if (Action.equals(mBluetoothCore.ACTION_BTCORE)) {
				int State = intent.getIntExtra("btCoreAction", 0);
				switch (State) {
				case BTERROR:
					break;
				case BTENABLED:
					UpdateDeviceListView(mBluetoothCore.btCoreGetPairedDevices(),btArrayPaired);
					break;
				case BTDISABLED:
					btArrayPaired.clear();
					btArrayDetected.clear();
					btArrayPaired.notifyDataSetChanged ();
					btArrayDetected.notifyDataSetChanged ();
					break;
				case BTDEVICE_FOUND:
					UpdateDeviceListView(mBluetoothCore.btCoreGetDetectedDevices(),btArrayDetected);
					break;
				case BTDEVICE_BOND:
					UpdateDeviceListView(mBluetoothCore.btCoreGetDetectedDevices(),btArrayDetected);
					UpdateDeviceListView(mBluetoothCore.btCoreGetPairedDevices(),btArrayPaired);
					break;
				case BTDEVICE_UNBOND:
					UpdateDeviceListView(mBluetoothCore.btCoreGetPairedDevices(),btArrayPaired);
					break;
				case BTDEVICE_CONNECTED:
					showCameraPreview ();
					break;
				case BTDEVICE_DISCONNECTED:
					break;
				default:
					break;
				}	
			}
		}
	};

	/*Updates a list view associated with x arraylist*/ 
	private void UpdateDeviceListView (ArrayList<BluetoothDevice> btCoreDevArray,ArrayAdapter<String> list){
		list.clear();
		if (btCoreDevArray != null) {
			for (int i = 0;i < btCoreDevArray.size();i++) {
				list.add(btCoreDevArray.get(i).getName() + "\n" + btCoreDevArray.get(i).getAddress());
			}
			list.notifyDataSetChanged (); 
		}
	}
}

