package com.example.snapsatpayload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class TxTelemetryService extends Service 
{

	private static final String TAG = "SnapSatPayload";
	public static final int InfoMsg        = 0;
	public static final int WiFi           = 1;
	public static final int SensorData     = 2;
	public static final int CameraFile     = 3;
	public static final int GPS            = 4;
	
	static int server_port  = MainActivity.UDPport;
	
	private static Thread UDPBroadcastThread;
	private static Thread LoggingThread;
	
	private static Queue<String> msgQueue = new ArrayBlockingQueue<String>(1000);
	private static Queue<String> logQueue = new ArrayBlockingQueue<String>(1000);
	
	void TxTelemetryServiceInit()
	{

    	Log.i(TAG, "TxTelemetryService - TxTelemetryServiceInit DatagramSocket");

    	StartsendUDPBroadcastThread();
    	StartLoggingThread();
		
	}

	class TxTelemetryServiceHandler extends Handler 
	{
			
	   @Override 
	   public void handleMessage(Message msg)
	   { 
	      super.handleMessage(msg);
		  String data   = null;
		  Bundle bundle = null;
		  switch (msg.what)
		  { 
		  
		     case InfoMsg:
			        bundle = msg.getData();
			        data   = bundle.getString("InfoMsgData");
			        
			        Log.i(TAG, "TxTelemetryService - handleMessage InfoMsg "+ data);
		        	    
			        break;
			        
		     case WiFi:
		        bundle = msg.getData();
		        data   = bundle.getString("WiFiData");
		        
		        Log.i(TAG, "TxTelemetryService - handleMessage WiFi "+ data);
		        	    
		        break;
		        
		     case SensorData:
			        bundle = msg.getData();
			        data   = bundle.getString("SensorData");
			        
			        Log.i(TAG, "TxTelemetryService - handleMessage SensorData "+ data);
			        	    
			        break;
			        
		     case CameraFile:
			        bundle = msg.getData();
			        data   = bundle.getString("CameraFile");
			        
			        Log.i(TAG, "TxTelemetryService - handleMessage CameraFile "+ data);
			        	    
			        break;
			        
		     case GPS:
			        bundle = msg.getData();
			        data   = bundle.getString("GPS");
			        
			        Log.i(TAG, "TxTelemetryService - handleMessage GPS "+ data);
			       			        	    
			        break;
		     	   
		     default: 
		        break; 
		  }
   
		  if(data != null)
		  {
			   try
		       {
				   msgQueue.add(data);
				   if(UDPBroadcastThread != null)
				   {
				       synchronized(UDPBroadcastThread)
				       {
				    	   UDPBroadcastThread.notify();
				       }
				   }
		       }
		       catch (IllegalStateException e)    
		       {
		    	   e.printStackTrace();
				   Log.e(TAG, "TxTelemetryService - IllegalStateException "+ e.getMessage()); 	
		       }
		  }
	   }
	}
	
	private void StartLoggingThread() 
	{  
		LoggingThread = new Logger(); 
		LoggingThread.setPriority(Thread.MIN_PRIORITY);
		LoggingThread.start();
	} 
	
	private static class Logger extends Thread
	{
		public void run() 
		{
      	  	Boolean ThreadAlive        = true;
      	  	while((!LoggingThread.isInterrupted()))
      	  	{
      	  		if(ThreadAlive)
      	  		{  			
    			    try 
    			    {
	      	  			synchronized(LoggingThread)
	      	  			{
	      	  				wait();
	      	  			}
         	  			while(!logQueue.isEmpty())
          	  			{
     	  					String data = logQueue.remove();
     	  					
    					    //Log to SD Card
    					    String FullPath = MainActivity.getLogDir() + MainActivity.getLogFileName();
    					    Log.i(TAG, "TxTelemetryService - Write To Log File "+ FullPath);
    					    
    					    // Open for Append
    					    FileOutputStream fOut = new FileOutputStream (new File(FullPath),true);  
    				        OutputStreamWriter osw = new OutputStreamWriter(fOut); 
    				        Log.i(TAG, "TxTelemetryService - osw.write "+ data);
    				        osw.write(data+"\r\n");
    				        Log.i(TAG, "TxTelemetryService - osw.flush() ");
    				        osw.flush();
    				        Log.i(TAG, "TxTelemetryService - osw.close()");
    				        osw.close();
    				        fOut.close();
     	  					
     	  					Log.i(TAG, "TxTelemetryService - Logger "+ data);
          	  			}
    			    }
    			    catch (InterruptedException e)
    			    {
    			    	Log.i(TAG, "TxTelemetryService - Logger ThreadAlive = false ");
    			    	ThreadAlive = false;
    					
    			    }
    			    catch (FileNotFoundException e) 
    			    {
    			    	Log.e(TAG, "TxTelemetryService - FileNotFoundException" + e.getMessage());
						e.printStackTrace();
					} 
    			    catch (IOException e)
					{
    			    	Log.e(TAG, "TxTelemetryService - IOException" + e.getMessage());
						e.printStackTrace();
					}
      	  			
      	  		}
      	  		else
      	  		{
      	  			break;
      	  		}
      	  	}
			
			Log.i(TAG, "TxTelemetryService - Logger - Logger Ended");
			LoggingThread = null;
		}
	}
	
	
	private void StartsendUDPBroadcastThread() 
	{  
		UDPBroadcastThread = new sendUDPBroadcast(); 
		UDPBroadcastThread.setPriority(Thread.NORM_PRIORITY);
		UDPBroadcastThread.start();
	} 
		
	private static class sendUDPBroadcast extends Thread
	{
		String _data;	
		public void run() 
		{
      	  	Boolean ThreadAlive        = true;
      	  	while((!UDPBroadcastThread.isInterrupted()))
      	  	{
			    try 
			    {	    	
     	  			if(ThreadAlive)
      	  			{
	      	  			synchronized(UDPBroadcastThread)
	      	  			{
	      	  				wait();
	      	  			}
      	  			}
     	  			else
     	  			{
     	  				break;
     	  			}
     	  			
	      	  		while(msgQueue.size() > 0)
	      	  		{
	      	  			_data = msgQueue.remove();
	      	  			logQueue.add(_data);
	      	  			synchronized(LoggingThread)
	      	  			{
	      	  				LoggingThread.notify();
	      	  			}
					   				        
					    InetAddress UDPAdress = InetAddress.getByName(MainActivity.UDPaddress);
					    int msg_length        = _data.length();
					    byte[] message        = _data.getBytes();
				        Log.i(TAG, "TxTelemetryService -new DatagramPacket ");
					    DatagramPacket p      = new DatagramPacket(message, msg_length,UDPAdress,server_port);
						if (SnapSatPayLoadManagerService.IsNetworkAvailable())
						{
						    DatagramSocket s = new DatagramSocket();
						    Log.i(TAG, "TxTelemetryService - s.send(p) "+ _data);
						    s.send(p);
						    s.close();
						    Log.i(TAG, "TxTelemetryService - s.close() ");
					    }
						else
						{
							Log.i(TAG, "TxTelemetryService - Network Not Avaiable "+ _data);
						}
	      	  		}//while(msgQueue.size() > 0)
		
				} //try 
			    catch (SocketException e) 
			    {
				   e.printStackTrace();
				   Log.e(TAG, "TxTelemetryService - SocketException "+ e.getMessage());
			    }
			    catch (UnknownHostException e) 
			    {
					e.printStackTrace();
					Log.e(TAG, "TxTelemetryService - UnknownHostException "+ e.getMessage());
				} 
			    catch (IOException e) 
			    {
					e.printStackTrace();
					Log.e(TAG, "TxTelemetryService - IOException "+ e.getMessage());
				}
			    catch (NoSuchElementException e)
			    {
					e.printStackTrace();
					Log.e(TAG, "TxTelemetryService - NoSuchElementException "+ e.getMessage());
			    }
			    catch (InterruptedException e)
			    {
			    	Log.i(TAG, "TxTelemetryService - sendUDPBroadcast ThreadAlive = false ");
			    	ThreadAlive = false;
					
			    }
      	  	}//while((!UDPBroadcastThread.isInterrupted()))
      	  	
    		Log.i(TAG, "TxTelemetryService - UDPBroadcastThread - UDPBroadcastThread Ended");
    		UDPBroadcastThread = null;
		}//run
	}

    private Messenger messenger = new Messenger(new TxTelemetryServiceHandler());
		
	@Override 
	public IBinder onBind(Intent intent)
	{
       Log.i(TAG, "TxTelemetryService - onBind");
    
       TxTelemetryServiceInit();
       
       return messenger.getBinder();
	}
	
	@Override
	public void onDestroy()
	{
		Log.i(TAG, "TxTelemetryService - onDestroy");
		super.onDestroy();
	   
		if(UDPBroadcastThread != null)   
		{   
			UDPBroadcastThread.interrupt();   
		}
		
		if(LoggingThread != null)
		{
			LoggingThread.interrupt();
		}
	}
}