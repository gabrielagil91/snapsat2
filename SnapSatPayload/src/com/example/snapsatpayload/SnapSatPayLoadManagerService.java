package com.example.snapsatpayload;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class SnapSatPayLoadManagerService extends Service
{
	private static final String TAG = "SnapSatPayload";
	
	/** Called when the activity is first created. */ 
	private TxTelemetryServiceConnection TxTelemetryService_conn = new TxTelemetryServiceConnection();
	private static Messenger TxTelemetryServiceConnectionMessenger = null;
	
	private GPSServiceConnection GPSService_conn = new GPSServiceConnection();
	private Messenger GPSServiceConnectionMessenger = new Messenger(new SnapSatPayLoadManagerServiceHandler());
	
	static private WifiLock wifiLock;
    private static ConnectivityManager myConnManager = null;
    private static WifiManager myWifiManager         = null;
	
	Timer SensorTimer = new Timer();
	SensorTimerTask sensorTimer= new SensorTimerTask();
	
	static boolean TxTelemetryServiceConnectionIsBound = false;
	boolean GPSServiceConnectionIsBound         = false;
	
	static Thread SensorThread = null;
	
	void SnapSatPayLoadManagerServiceInit()
	{
		TxTelemetryServiceConnectionMessenger = new Messenger(new SnapSatPayLoadManagerServiceHandler());
		
	    myConnManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		myWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
	    wifiLock = myWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF , TAG);
	    wifiLock.acquire();

		GPSServiceConnectionIsBound = bindService(new Intent(this, GPSUSBService.class), GPSService_conn, Context.BIND_AUTO_CREATE);		
		if(GPSServiceConnectionIsBound)
		{
			Log.i(TAG, "SnapSatPayLoadManagerService - bindService to GPSUSBService - SUCCESS");  
		}
		else
		{
			Log.e(TAG, "SnapSatPayLoadManagerService - bindService to GPSUSBService - FAIL"); 
		}
				
		TxTelemetryServiceConnectionIsBound = bindService(new Intent(this, TxTelemetryService.class), TxTelemetryService_conn, Context.BIND_AUTO_CREATE);		
		if(TxTelemetryServiceConnectionIsBound)
		{
			Log.i(TAG, "SnapSatPayLoadManagerService - bindService to TxTelemetryService - SUCCESS");  
		}
		else
		{
			Log.e(TAG, "SnapSatPayLoadManagerService - bindService to TxTelemetryService - FAIL"); 
		}
		    
        SendVersion();
        
        StartSensorThrread();
        		 			 
        // 1.0 second timer
        SensorTimer.schedule(sensorTimer, 1000, 1000);
		Log.i(TAG, "SnapSatPayLoadManagerService - Sensor Timer started");
		
	}
	
	public static void StartSensorThrread() 
	{ 
	    SensorThread = new SendSensorDataThread();
	    SensorThread.setPriority(Thread.MAX_PRIORITY);
	    SensorThread.start();  
	} 
	

	private static class SendSensorDataThread extends Thread
	{	
  		
		public void run() 
		{
      	  	Boolean ThreadAlive  = true;
      	  	
      	  	while((!SensorThread.isInterrupted()))
      	  	{
      	  		try
      	  		{
      	  			if(ThreadAlive)
      	  			{
	      	  			synchronized(SensorThread)
	      	  			{
	      	  				wait();
	      	  				SendSensorData();
	      		 	        SendWifiState();
	      					if(!myWifiManager.isWifiEnabled())
	      					{
	      						 ReConnectWiFi();
	      					}
	      	  			}
      	  			}
      	  			else
      	  			{
      	  				break;
      	  			}
      	  		}
			    catch (InterruptedException e2) 
			    {
			    	Log.i(TAG, "SnapSatPayLoadManagerService - SensorThread - ThreadAlive = false ");
			    	ThreadAlive = false;
			    }
      	  	}
      	  	
    		Log.i(TAG, "SnapSatPayLoadManagerService - SendSensorDataThread - SensorThread Ended");
    		SensorThread = null;
		}
	}
	

	static public boolean IsNetworkAvailable()
	{
		NetworkInfo myNetworkInfo  = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		boolean status = false;
		if(myNetworkInfo.isConnected())
		{
			if(myNetworkInfo.isAvailable())
			{
				if(myWifiManager.getConnectionInfo().getRssi() > -82)
				{
					return true;
				}
			}
		}
		
		return status;
	}
	
	static public void ReConnectWiFi()
	{	

		if(myWifiManager.reconnect())
		{
			Log.i(TAG, "SnapSatPayLoadManagerService - Reconnect WiFI - Sucess");	
		}
		else
		{
			Log.e(TAG, "SnapSatPayLoadManagerService - Reconnect WiFI - Fail");	
		}	
	}
	
	
    public void SendVersion()
    {
    	SimpleDateFormat s = new SimpleDateFormat("yyyyMMddhhmmss");
	    String ts = s.format(new Date());
   	    String msgID = Integer.toString(TxTelemetryService.InfoMsg);
    	   
	    Message Msg = new Message();
	    Bundle bundle = Msg.getData();
	    Msg.what = TxTelemetryService.InfoMsg;
	        		        
	    bundle.putString("InfoMsgData",ts + "," + msgID + "," + MainActivity.VERSION);
	    Msg.setData(bundle);
	    
	    try 
	    {
	    	TxTelemetryServiceConnectionMessenger.send(Msg);
		} 
	    catch (RemoteException e) 
	    {
	    	Log.e(TAG, "TxTelemetryService - SendVersion "+ e.getMessage());
			e.printStackTrace();
		}
    }
		
    private static void SendWifiState()
	{
		 SimpleDateFormat s = new SimpleDateFormat("yyyyMMddhhmmss");
		 String ts = s.format(new Date());
		 String msgID = Integer.toString(TxTelemetryService.WiFi);
		
		 String ConnectionStatus = "";
		 String RSSIDdata        = "";
		 
		 if (IsNetworkAvailable())
		 {
			 ConnectionStatus = "Connected";
			 RSSIDdata        = Integer.toString(myWifiManager.getConnectionInfo().getRssi());
		 }
		 else
		 {
			 ConnectionStatus = "Disconnected";
			 RSSIDdata        = "N/A";
			 Log.e(TAG, "SnapSatPayLoadManagerService - Disconnected");
		}
		 
		Message Msg = new Message();
		Bundle bundle = Msg.getData();
		Msg.what = TxTelemetryService.WiFi;
		bundle.putString("WiFiData", ts + "," + msgID + "," + ConnectionStatus + "," + RSSIDdata);
		Msg.setData(bundle);
		if(TxTelemetryServiceConnectionIsBound)
		{
		   try 
		   {
		   	TxTelemetryServiceConnectionMessenger.send(Msg);
		   } 
		   catch (RemoteException e)
		   {
			   Log.e(TAG, "SnapSatPayLoadManagerService - SendWifiState " + e.getMessage());
			   e.printStackTrace();
		   }
		}	
	 }
    
    private static void SendSensorData()
    {
    	SimpleDateFormat s = new SimpleDateFormat("yyyyMMddhhmmss");
    	String ts = s.format(new Date());
    	String msgID = Integer.toString(TxTelemetryService.SensorData);
    	
	    Log.i(TAG, "SnapSatPayLoadManagerService - SensorTimerTask");
	    Message Msg = new Message();
	    Bundle bundle = Msg.getData();
	    Msg.what = TxTelemetryService.SensorData;
	    		    
	    //Read sensor data
	    bundle.putString("SensorData", ts + "," + msgID + ",V=13.2,PE=29.92");
	    Msg.setData(bundle);
	    if(TxTelemetryServiceConnectionIsBound)
	    {
	        try 
	        {
	            TxTelemetryServiceConnectionMessenger.send(Msg);
	        } 
	    	catch (RemoteException e)
	    	{
	    		Log.e(TAG, "SnapSatPayLoadManagerService - TxTelemetryServiceConnectionMessenger.send " + e.getMessage());
	    		e.printStackTrace();
	    	}
	    }
	}

	  
	 private class SensorTimerTask extends TimerTask
	 {
        @Override
		public void run()
		{
  			synchronized(SensorThread)
  			{
  				SensorThread.notify();
  			}  	
		}
	 }
	  
	 class TxTelemetryServiceConnection implements ServiceConnection
	 { 
		   @Override 
		   public void onServiceConnected(ComponentName arg0, IBinder binder)
		   {
			   TxTelemetryServiceConnectionMessenger = new Messenger(binder);
		        if(TxTelemetryServiceConnectionMessenger != null)
		        {
		        	Log.i(TAG, "SnapSatPayLoadManagerService - TxTelemetryServiceConnection - SUCCESS");
		        	TxTelemetryServiceConnectionIsBound = true;
		        }
		        else
		        {
		        	Log.e(TAG, "SnapSatPayLoadManagerService - TxTelemetryServiceConnection - FAIL");
		        	TxTelemetryServiceConnectionIsBound = false;
		        }
		   }
		     
		   @Override 
		   public void onServiceDisconnected(ComponentName arg0)
		   { 
			   TxTelemetryServiceConnectionMessenger = null; 
		   } 
	}
	 

	class GPSServiceConnection implements ServiceConnection
	{ 
		   @Override 
		   public void onServiceConnected(ComponentName arg0, IBinder binder)
		   {
			   GPSServiceConnectionMessenger = new Messenger(binder);
		        if(GPSServiceConnectionMessenger != null)
		        {
		        	Log.i(TAG, "SnapSatPayLoadManagerService - GPSServiceConnection - SUCCESS");
		        	GPSServiceConnectionIsBound = true;
		        }
		        else
		        {
		        	Log.e(TAG, "SnapSatPayLoadManagerService - GPSServiceConnection - FAIL");
		        	GPSServiceConnectionIsBound = false;
		        }
		   }
		     
		   @Override 
		   public void onServiceDisconnected(ComponentName arg0)
		   { 
			   GPSServiceConnectionMessenger = null; 
		   } 
	}
	
	 
	class SnapSatPayLoadManagerServiceHandler extends Handler
	{
										
        @Override 
		public void handleMessage(Message msg)
		{ 
		}
	}
	
	
	class GPSServiceHandler extends Handler
	{
										
        @Override 
		public void handleMessage(Message msg)
		{ 
		}
	}
	
	
    @Override 
	public IBinder onBind(Intent intent)
	{
	    Log.i(TAG, "SnapSatPayLoadManagerService - onBind");
	    return TxTelemetryServiceConnectionMessenger.getBinder();
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
		Log.i(TAG, "SnapSatPayLoadManagerService - onStartCommand Start");
		SnapSatPayLoadManagerServiceInit();
		Log.i(TAG, "SnapSatPayLoadManagerService - onStartCommand Complete");
        return Service.START_STICKY;
    }
    
	@Override
	public void onDestroy() 
	{
		
		super.onDestroy();
		
		Log.i(TAG, "SnapSatPayLoadManagerService - onDestroy");
	
		sensorTimer.cancel();
		SensorThread.interrupt();	

		stopService(new Intent(this,GPSUSBService.class));
		unbindService(GPSService_conn);	
		
		stopService(new Intent(this,TxTelemetryService.class));
		unbindService(TxTelemetryService_conn);
        wifiLock.release();
		
	}

}
