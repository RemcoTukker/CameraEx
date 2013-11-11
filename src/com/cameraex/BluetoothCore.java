package com.cameraex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class BluetoothCore {

	BluetoothAdapter							bluetoothAdapter 	= null;
	/*Hash Tables*/
	Hashtable<BluetoothDevice, BluetoothSocket>	btSockets 			= null;
	static 	Hashtable<String, Queue<String>>	btInfoDevices		= null;
	/*Array Lists*/
	ArrayList<BluetoothDevice>					btListDetected		= null;			//Array List of all stored bluetooth detected devices.
	ArrayList<BluetoothDevice>					btListPaired		= null;			//Array List of all stored bluetooth paired devices.
	/*Queues*/
	static 	Queue<String>						btQueueDev			= null;			//Queue storing device information.
	/*Handler*/
	//  static BTCoreHandler 						btCoreHandler		= null;
	/*Broadcast Sender Constants*/
	public final String 						ACTION_BTCORE 		  = "bluetoothCore.broadcastSender";
	final int									BTERROR 			  = 0;
	final int									BTDISCOVER_FINISHED   = 1;
	final int									BTENABLED			  = 2;
	final int									BTDISABLED			  = 3;
	final int									BTDEVICE_FOUND 		  = 4;
	final int 									BTDEVICE_BOND		  = 5;
	final int 									BTDEVICE_UNBOND		  = 6;
	final int 									BTDEVICE_CONNECTED	  = 7;
	final int 									BTDEVICE_DISCONNECTED = 8;

	
	/*Broadcast Receiver*/
	private BroadcastReceiver 					btBroadcastReceiver = null;
	/*Broadcast Receiver Constants*/
	final int									INFO_READ 			= 1;
	/*Threads*/
	private ConnectThread						connectThread 		= null;
	public Intent								btCoreIntent		= null;
	Context										Gcontext			= null;
	
	/****************************************************
	-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-
						PUBLIC METHODS
	-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-
	****************************************************/

	/*Method to initialize the btCoreBluetooth class*/
	public void btCoreCreate(Context context) {

		bluetoothAdapter 			= BluetoothAdapter.getDefaultAdapter();
		btListPaired				= new ArrayList<BluetoothDevice>();
		btListDetected				= new ArrayList<BluetoothDevice>();
		btSockets					= new Hashtable<BluetoothDevice, BluetoothSocket>();
		btInfoDevices				= new Hashtable<String, Queue<String>>();
	//     btCoreHandler				= new BTCoreHandler ();
		btCoreIntent 				= new Intent();
		Gcontext 					= context;
	}

	/*Handler in charge of received data from "read threads"*/
	static class BTCoreHandler extends Handler {																				 
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 1:
					btCoreQueueWrite ((String) msg.obj);																		//Storing the information in the appropiate FIFO.
					Log.i("inside",(String) msg.obj);
					break;
				default:
					break;
			}
		}
	}

	/*Method to release bluetoothCore Resources*/
	public void btCoreStop () {
		if (btBroadcastReceiver != null) {
			Gcontext.unregisterReceiver (btBroadcastReceiver);																//Unregistering brodadcast receiver.
			btBroadcastReceiver = null;
		}
	}

	/*Method to ask for the needed bluetoothCore Resources*/
	public void btCoreResume () {																								
		if (btBroadcastReceiver == null) {
			btBroadcastReceiver = new btCoreActionChanged();																//Registering the broadcast receiver and initializing filters.
			Gcontext.registerReceiver(btBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
			Gcontext.registerReceiver(btBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
			Gcontext.registerReceiver(btBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
			Gcontext.registerReceiver(btBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
			Gcontext.registerReceiver(btBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
		}
	}

	/*Method to realease all bluetoothCore Resources*/
	public void btCoreDestroy () {
		CloseAllConnections ();
		btCoreStop ();
		if (bluetoothAdapter != null){
			if (bluetoothAdapter.isDiscovering())
			bluetoothAdapter.cancelDiscovery();
			if (bluetoothAdapter.isEnabled())
			bluetoothAdapter.disable();
		}
	}

	/*Method to extract an element of certain bluetooth reception queue*/
	public String btCoreGetQueueElement (BluetoothDevice btDevice) {
		String macAddress = btDevice.getAddress ();
		if (!btInfoDevices.isEmpty()) {																							//Queues Hash table is not empty.
			if (btInfoDevices.containsKey (macAddress)) {																		//Queues Hash table contains this Mac Address.
				if (!btInfoDevices.get (macAddress).isEmpty ()) {																//Queue is not empty.
					return btInfoDevices.get(macAddress).remove ();																//Returning element. 
				}
			}
		}
		return null;
	}

	/*Method to add an element to certain bluetooth reception queue*/
	static void btCoreQueueWrite (String data) {																				//String structure: [information][$][mac_address]
		int addStart = data.indexOf ('$');																						//Searching the [$] index.
		if (addStart != -1) {																									//[$] exists in the string.
			String macAddress = data.substring (addStart + 1);																	//Extracting Mac Address.
			String information = data.substring (0,addStart);																	//Extracting Information.
			if (!btInfoDevices.isEmpty ()) {																					//Queues Hash table is not empty.
				if (btInfoDevices.containsKey(macAddress)) {																	//Queues Hash table contains this Mac Address.
					btInfoDevices.get(macAddress).add(information);																//Writing the information in the queue.
				}
			}
		}
	}

	/*Method to List all paired bluetooth devices*/
	public ArrayList<BluetoothDevice> btCoreGetPairedDevices () {
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()){															//Bluetooth Adapter exists and it's enabled.
			Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices ();											//Getting all paired bluetooth devices.     
			if (pairedDevices.size() > 0) {																						//The list is not empty.
				btListPaired.clear ();																							//Cleaning the paired bluetooth devices list.
				for (BluetoothDevice device : pairedDevices) {
					btListPaired.add (device);                																	//Storing the devices in the paired bluetooth devices list.
				}
				return btListPaired;																							//returning the List.
			}
		}
		return null;
	}

	/*Method to List all detected bluetooth devices*/
	public ArrayList<BluetoothDevice> btCoreGetDetectedDevices () {												
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled () ){														//Bluetooth Adapter exists and it's enabled.
			if (btListDetected.size() > 0)																					//There are elements in the detected bluetooth devices list.
			return btListDetected;																							//returning the List.
		}
		return null;
	}

	/*Method to List all connected bluetooth devices*/
	public ArrayList<BluetoothDevice> btCoreGetConnectedDevices () {															
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (!btSockets.isEmpty ()) {																						//Sockets Hash table is not empty.
				ArrayList<BluetoothDevice> 	btListConnected = new ArrayList<BluetoothDevice> ();								//Creating a new array to store all the Connected devices.
				Enumeration<BluetoothDevice> btDevices = btSockets.keys ();														//Extracting all connected devices. 	
				while (btDevices.hasMoreElements ()) {																			//Storing connected devices in the array.
					btListConnected.add(btDevices.nextElement ());
				}
				return btListConnected;
			}
		}
		return null;
	}

	/*Method to extract a certain Detected bluetooth device from the detected list*/
	public BluetoothDevice btCoreGetDetectedDevice (int position) {																
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (btListDetected.size () > 0) {																					//There are elements in the Detected list.
				if (position <= btListDetected.size ())																			//Position exists in the Detected list.
				return btListDetected.get (position);																			//Extracting the detected device.
			}
		}
		return null;
	}

	/*Method to extract a certain Paired bluetooth device from the paired list*/
	public BluetoothDevice btCoreGetPairedDevice (int position) {																
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (btListPaired.size () > 0) {																						//There are elements in the Paired list.
				if (position <= btListPaired.size ())																			//Position exists in the Paired list.
				return btListPaired.get (position);																				//Extracting the paired device.
			}
		}
		return null;
	}

	/*Method to disconnect all connected devices*/
	public void btCoreCloseAllConnections () {
		CloseAllConnections ();    
	}

	/*Method to send a message to the connected bluetooth device*/
	public void btCoreSendMessage (byte[] Message, BluetoothDevice btDevice) {
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (bluetoothAdapter.isDiscovering ())																				//Cancelling the bluetooth devices discovering.
			bluetoothAdapter.cancelDiscovery ();
			if (!btSockets.isEmpty ()) {																						//Socket Hash table is not empty.
				if (btSockets.containsKey (btDevice)) {																			//Socket Hash table contains that device, so it's connected.
					try {
						WriteThread writeThread = new WriteThread (btSockets.get(btDevice),Message);							//Creating a new Writing thread to send a message to the connected device.
						writeThread.start ();																					//Starting the Writing thread.
					}catch (Exception e) {e.printStackTrace ();}
				}
			}
		}
	}

	/*Method to Enable Bluetooth*/
	public Boolean btCoreEnableBluetooth () {																						
		/* Bluetooth Adapter Exists*/
		if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's not enabled.
			Intent enableBtIntent = new Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE);										//Requesting bluetooth activation.
			Gcontext.startActivity (enableBtIntent);
			return true;
		}
		return false;
	}

	/*Method to Disable Bluetooth*/
	public Boolean btCoreDisableBluetooth () {
		/* Bluetooth Adapter Exists*/
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (bluetoothAdapter.isDiscovering ()) {																			//Cancelling the bluetooth devices discovering.
				bluetoothAdapter.cancelDiscovery ();
			}
			CloseAllConnections ();																								//Disconnecting all devices. 
			bluetoothAdapter.disable ();																						//Disabling bluetooth.
			return true;
		}
		return false;
	}

	/*Method to make the device discoverable*/
	public void btCoreMakeDicoverable () {
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (bluetoothAdapter.isDiscovering ())																				//Cancelling the bluetooth devices discovering.
			bluetoothAdapter.cancelDiscovery ();
			Intent discoverableIntent = new Intent (BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra (BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 360);									//Time being discoverable 6 minuts.
			Gcontext.startActivity (discoverableIntent);
		}
	}

	/*Method to discover bluetooth devices*/
	public void btCoreDiscover () {
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (bluetoothAdapter.isDiscovering ()) {																			//Cancelling the bluetooth devices discovering.
				bluetoothAdapter.cancelDiscovery ();																			
			}
			btListDetected.clear ();																							//Cleaning the bluetooth devices detected list. 
			bluetoothAdapter.startDiscovery ();																					//Starting the bluetooth device discovering, 12 secs.
		}
	}

	/*Method to create a bond with the selected device*/
	public void btCoreBond (BluetoothDevice btDevice) {
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (bluetoothAdapter.isDiscovering ())																				//Cancelling the bluetooth devices discovering.
			bluetoothAdapter.cancelDiscovery ();
			PairingThread pairingThread = new PairingThread (btDevice);															//Creating a new pairing thread to bond the device.
			pairingThread.start ();																								//Starting the pairing thread.
		}
	}

	/*Method to unbond the selected device*/
	public Boolean btCoreUnBond (BluetoothDevice btDevice) {
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (bluetoothAdapter.isDiscovering ())																				//Cancelling the bluetooth devices discovering.
			bluetoothAdapter.cancelDiscovery ();
			try {
				if (!btListPaired.isEmpty ()) {																					//List of paired bluetooth devices is not empty.
					if (btListPaired.contains(btDevice)) {																		//List of paired bluetooth devices contains the device to unbond.
						if (!btSockets.isEmpty ()) {																			//Socket Hash table is not empty.
							if (btSockets.containsKey (btDevice)) {																//Socket Hash table contains that device, so the device is connected.
								if (!CloseSocket (btSockets.get (btDevice),btDevice)) {											//Closing the connection.  					
									return false;
								}
							}
						}
						removeBond (btDevice);																					//Unbonding device.				
					}
				}
			} catch (Exception e) {e.printStackTrace (); return false;}															
		}
		return true;
	}

	/*Method to create a connection between bonded device*/
	public Boolean btCoreConnect (BluetoothDevice btDevice) {
		if (bluetoothAdapter != null && bluetoothAdapter.isEnabled ()) {														//Bluetooth Adapter exists and it's enabled.
			if (bluetoothAdapter.isDiscovering ()) {																			//Cancelling the bluetooth devices discovering.
				bluetoothAdapter.cancelDiscovery ();
			}
			if (!btSockets.isEmpty ()) {																						//Socket Hash table is not empty.																					
				if (btSockets.containsKey (btDevice)) {																			//Socket Hash table contains that device, so the device is already connected.
					if (btSockets.get (btDevice) != null) {																		//The socket exists, the connection process is aborted..
						return false;																							
					}else{
						btSockets.remove (btDevice);																			//The socket doesn't exists, removing object from Hash table.
					}
				}
			}
			connectThread = new ConnectThread (btDevice);																		//Creating a new connection thread to connect the device.
			connectThread.start ();																								//Starting the connection thread.												
			return true;
		}
		return false;
	}

	/****************************************************
	-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-
							THREADS
	-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-
	****************************************************/

	private class PairingThread extends Thread {    	
		private BluetoothDevice btDevice;
		
		public PairingThread (BluetoothDevice a) {
			btDevice = a;
		}
		public void run () {
			if (bluetoothAdapter != null) {
				if (bluetoothAdapter.isDiscovering ()) {
					bluetoothAdapter.cancelDiscovery ();
				}
				try {
					createBond (btDevice);
				} catch (Exception e) {e.printStackTrace ();}
			}
		}
	}

	public synchronized void finish() {
		/* Cancel any thread attempting to make a connection */
		if (connectThread != null) {
			connectThread.Cancel();
			connectThread = null;
		}
	}
	
	/* Creates a socket with a paired device */
	private class ConnectThread extends Thread {

		private final BluetoothSocket threadSocket;
		private final BluetoothDevice threadDevice;

		public ConnectThread (BluetoothDevice device) {
			BluetoothSocket Sock 	= null;
			threadDevice			= device;
			try{
				Method m 	= threadDevice.getClass ().getMethod("createRfcommSocket", new Class[] { int.class });
				Sock 		= (BluetoothSocket) m.invoke(threadDevice, Integer.valueOf(1));
			} catch (Exception e) {e.printStackTrace(); }
			threadSocket = Sock;
		}

		public void run() {

			if (bluetoothAdapter != null) {
				if (bluetoothAdapter.isDiscovering ()) {
					bluetoothAdapter.cancelDiscovery ();
				}
				Thread.currentThread().setName("ConnectThread");
				try {
					threadSocket.connect ();
					synchronized (BluetoothCore.this) {
					btSockets.put(threadDevice, threadSocket);
					}
				} catch (IOException e) {Log.e(Thread.currentThread().getName(),"Connection Error",e);
					try {
						threadSocket.close();
					} catch (IOException e2) {Log.e(Thread.currentThread().getName(),"closing socket Error",e2);}
					BluetoothCore.this.finish ();
					return;
				}
				synchronized (BluetoothCore.this) {
					connectThread = null;
					Log.i("connectThreadNull","connectThreadNull");
				}
				Connected(threadSocket,threadDevice);
			}			
		}

		public void Cancel() {
			try {
				threadSocket.close();
				Log.i("cancel Connected Thread","close");
			} catch (IOException e) {
				Log.e("Cancel Connected Thread","close() of connect socket failed", e);
			}
		}
	}

	public synchronized  void Connected (BluetoothSocket btSocket,BluetoothDevice btDevice) {
		ReadThread readThread = new ReadThread (btSocket,btDevice);
		readThread.start ();
		SendBroadcast(BTDEVICE_CONNECTED);
	}

	private class ReadThread extends Thread {
		private InputStream		threadIS;
		private BluetoothDevice	threadbtDevice;
		public ReadThread (BluetoothSocket btSocket, BluetoothDevice btDevice) {
			InputStream IS 	= null;			
			try {
				IS = btSocket.getInputStream();
				btQueueDev = new LinkedList<String>();
				btInfoDevices.put(btDevice.getAddress(), btQueueDev);
				threadbtDevice = btDevice;
			} catch (IOException e) {
				Log.e("ReadWriteThread", "temp sockets not created", e);
			}
			threadIS = IS;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;
			StringBuilder stringIncom = new StringBuilder ();
			Log.i("ReadWriteThread", "Starting read");
			this.setName("read");
			Log.i ("threadread",this.getName());
			
			while (true) {
				try {
					bytes = threadIS.read(buffer);
					byte[] readBuffer = (byte[]) buffer;
					String strIncom = new String(readBuffer, 0, bytes);					// create string from bytes array
					stringIncom.append(strIncom);										// append string
					for (int i = 0;i < stringIncom.length();i++){
						if (stringIncom.charAt(i) == '$'){
							StringBuilder toSend = new StringBuilder ();
							String strToSend = stringIncom.substring(0, i+1);
							toSend.append(strToSend);
							toSend.append(threadbtDevice.getAddress());
							btCoreQueueWrite (toSend.toString());																		//Storing the information in the appropiate FIFO.
							//btCoreHandler.obtainMessage(INFO_READ,toSend.toString()).sendToTarget();
							stringIncom.delete(0, i+1);
							i = 0;
						} 
					}
				} catch (IOException e) {Log.e("ReadWriteThread", "disconnected", e);break;}
			
			}
			Log.i ("endthread",this.getName());
		}
	}

	private class WriteThread extends Thread {    	
		private BluetoothSocket Sock;
		private OutputStream	threadOS;
		private byte[] 			Message;
		
		public WriteThread(BluetoothSocket socket, byte[] message) {
			Sock 		= socket;
			threadOS 	= null;
			Message		= message;
			try {
				threadOS = Sock.getOutputStream();
			} catch (IOException e) {}
		}
		
		public void run() {
			this.setName("write");
			try {
				threadOS.write(Message,0,Message.length);
				threadOS.flush();
			} catch (IOException e) {}
		}
	}
	
	/****************************************************
	-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-
						PRIVATE METHODS
		-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-
	****************************************************/

	private boolean createBond(BluetoothDevice btDevice) throws Exception { 

		Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
		Method createBondMethod = class1.getMethod("createBond");  
		Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);

		return returnValue;  
	}

	private boolean removeBond(BluetoothDevice btDevice) throws Exception {  
		Class btClass = Class.forName("android.bluetooth.BluetoothDevice");
		Method removeBondMethod = btClass.getMethod("removeBond");  
		Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);

		return returnValue.booleanValue();  
	}

	/*Closses all opened InputStreams/OutputStreams and sockets, resets handlerIdentifier"*/
	private Boolean CloseAllConnections () {
		Enumeration<BluetoothDevice> Devices = btSockets.keys(); 
		while (Devices.hasMoreElements()) {
			BluetoothDevice auxDev = Devices.nextElement();
			BluetoothSocket auxSock = btSockets.get(auxDev);
			if (!CloseSocket (auxSock,auxDev))
			return false;
		}
		return true;
	}

	private Boolean CloseSocket (BluetoothSocket btSocket, BluetoothDevice btDevice) {
		if (btSocket != null){
			try{
				InputStream iS = btSocket.getInputStream();
				OutputStream oS= btSocket.getOutputStream();
				if (iS != null) {
					try {iS.close();} catch (Exception e) {Log.e("Closing sockets","iS Error",e);return false;}
					iS = null;
				}
				if (oS != null) {
					try {oS.close();} catch (Exception e) {Log.e("Closing sockets","oS Error",e);return false;}
					oS = null;
				}
				if (btSocket != null) {
					try {btSocket.close();} catch (Exception e) {Log.e("Closing sockets","Socket Error",e);return false;}
					btSocket = null;
					btSockets.remove(btDevice);
					btInfoDevices.remove(btDevice.getAddress());
					SendBroadcast(BTDEVICE_DISCONNECTED);

					return true;
				}
			}catch (IOException e){Log.e("Closing sockets","iS & oS Error",e);return false;}
		}
		return false;
	}

	private void SendBroadcast(int action){
		btCoreIntent.setAction(ACTION_BTCORE);
		btCoreIntent.putExtra("btCoreAction", action);
		Gcontext.sendBroadcast(btCoreIntent);
	}

	private class btCoreActionChanged extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();

			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				int State = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch (State) {
					case BluetoothAdapter.STATE_OFF:
						btListDetected.clear ();
						btListPaired.clear();
						SendBroadcast(BTDISABLED);
						break;
					case BluetoothAdapter.STATE_TURNING_OFF:
						break;
					case BluetoothAdapter.STATE_ON:
						btCoreGetPairedDevices();
						SendBroadcast(BTENABLED);
						break;
					case BluetoothAdapter.STATE_TURNING_ON:
						break;
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
						break;
					default:
						break;
				}
			}else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
				Log.i("ACTION_DISCOVERY_STARTED","ACTION_DISCOVERY_STARTED");
			}else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				Log.i("ACTION_DISCOVERY_FINISHED","ACTION_DISCOVERY_FINISHED");
			}else if (action.equals(BluetoothDevice.ACTION_FOUND)){
				BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (!btListDetected.contains(newDevice) && !btListPaired.contains(newDevice)) {
					btListDetected.add(newDevice);
					SendBroadcast(BTDEVICE_FOUND);
				}
			}else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
				BluetoothDevice	btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int extraState = intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE);

				if (extraState == BluetoothDevice.BOND_NONE) {
					if (btListPaired.contains (btDevice)) 
					btListPaired.remove (btDevice);
					SendBroadcast (BTDEVICE_UNBOND);
				}
				if (extraState == BluetoothDevice.BOND_BONDED) {
					btCoreGetPairedDevices ();
					if (btListPaired.contains(btDevice) && btListDetected.contains(btDevice))
					btListDetected.remove(btDevice);
					SendBroadcast(BTDEVICE_BOND);
				}
			}
		}
	};	
}

