package com.example.snapsatpayload;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class GPSUSBService extends Service 
{
	private static final String TAG = "SnapSatPayload";
	
	private static final int MAX_BUF = 2048;
	
	private static boolean TxTelemetryServiceConnectionIsBound     = false;
	private TxTelemetryServiceConnection TxTelemetryService_conn   = new TxTelemetryServiceConnection();
	private static Messenger TxTelemetryServiceConnectionMessenger = null;
	
	private static Thread GPSThread         = null;
	
    
	private void GPSUSBServiceInit()
	{
	    Log.i(TAG, "GPSUSBServiceInit - Start");
		TxTelemetryServiceConnectionIsBound = bindService(new Intent(this, TxTelemetryService.class), TxTelemetryService_conn, Context.BIND_AUTO_CREATE);
		
		if(TxTelemetryServiceConnectionIsBound)
		{
			Log.i(TAG, "GPSUSBService - bindService to TxTelemetryService - SUCCESS");  
		}
		else
		{
			Log.e(TAG, "GPSUSBService - bindService to TxTelemetryService - FAIL"); 
		}
					 		    
	    TxTelemetryServiceConnectionMessenger = new Messenger(new TxTelemetryServiceHandler());
	    
	    StartManageGPSDevice();
	          
	    Log.i(TAG, "GPSUSBServiceInit - Complete");
	}
	
	private void StartManageGPSDevice() 
	{  
		GPSThread = new ManageGPSDeviceThread();
		GPSThread.setPriority(Thread.MIN_PRIORITY);
		GPSThread.start();  
	} 
	
	
	private static class ManageGPSDeviceThread extends Thread
	{
		
		public void run() 
		{
			Log.i(TAG, "GPSUSBService - ManageGPSDevice - Run");
	
      	  	byte buffer[]                = new byte[MAX_BUF];    	  		
      	  	byte GPSbuffer[]             = new byte[MAX_BUF];
      	    int GPSbufferIndx			 = 0;
      	    int numBytesRead             = 0;
      	    File fIn                     = new File("/dev/ttyUSB0");
      	    SerialPort mSerialPort       = null;
      	    int baudrate                 = 9600;
      		InputStream mInputStream     = null;
      	  	      	    
      	  	while((!GPSThread.isInterrupted()))
      	  	{	
      	  		if(fIn.exists())
      	  		{
      	  			if(mSerialPort == null)
      	  			{
      	  				
      	  				try 
      	  				{
   	  					
							mSerialPort   = new SerialPort(fIn, baudrate, 0);
						} 
      	  				catch (SecurityException e) 
      	  				{
      	  					Log.e(TAG, "GPSUSBService - SerialPort SecurityException" + e.getMessage());
							e.printStackTrace();
							mSerialPort = null;
						} 
      	  				catch (IOException e) 
      	  				{
      	  					Log.e(TAG, "GPSUSBService - SerialPort IOException" + e.getMessage());
							e.printStackTrace();
							mSerialPort = null;
						}
      	  				
  	  					GPSbufferIndx = 0;
      	  			}
      	  			else
      	  			{
      	  				mInputStream = mSerialPort.getInputStream();
      	  				if(mInputStream == null)
      	  				{
      	  					mSerialPort   = null;
      	  					GPSbufferIndx = 0;
      	  				}
      	  				else
      	  				{
      	  					try 
      	  					{
      	  					    if(mInputStream.available() > 0)
      	  					    {
	      	  						numBytesRead = mInputStream.read(buffer);
	      	  						if(numBytesRead > 0)
	      	  						{
	      	  							for(int i =0; i < numBytesRead; i++)
	      	  							{
	      	  								if(GPSbufferIndx < MAX_BUF)
	      	  								{
	      	  									GPSbuffer[GPSbufferIndx] = buffer[i];
	      	  									if( GPSbuffer[GPSbufferIndx] == '\n')
	      	  									{
	      	  										if(GPSbuffer[0] == '$')
	      	  										{
		      	  										String GPSSentence = new String(GPSbuffer,0,GPSbufferIndx+1);				      	       
		      	  										SendGPSSentence(GPSSentence);
		      	        	  	      	  				Thread.sleep(100);
	      	  										}
	      	  										
	      	  										GPSbufferIndx = 0;
	      	  									}
	      	  									else
	      	  									{
	      	  										GPSbufferIndx++;
	      	  									}
	      	  								}
	      	  								else
	      	  								{
	      	  									GPSbufferIndx = 0;
	      	  									Log.e(TAG, "GPSUSBService -ManageGPSDevice Buffer Full");
	      	  								}
	      	  							}
			      	        		}
      	  					    }
      	  					    else
      	  					    {
      	  	      	  				Thread.sleep(100);
      	  					    }
      	  					} 
      	  					catch (IOException e) 
	      	  				{
	      	  					Log.e(TAG, "GPSUSBService - IOException " + e.getMessage());
	      	  					e.printStackTrace();
	      	  					mSerialPort.close();
	      	  					mSerialPort = null;	
							}
      	        	  		catch (InterruptedException e2) 
      	        	  		{
      	        	  			// TODO Auto-generated catch block
      	        	  			e2.printStackTrace();
      	        	  		} 	
		      	        }			      	        		    	
		      	    }
      	  		}
      	  	} 
      	  	
    		Log.i(TAG, "GPSUSBService - ManageGPSDevice - GPSThread Ended");
    		GPSThread = null;
		}
	}
	
	
	static void SendGPSSentence(String GPSSentence)
	{
   	    SimpleDateFormat s = new SimpleDateFormat("yyyyMMddhhmmss");
   	    String ts = s.format(new Date());
   	    String msgID = Integer.toString(TxTelemetryService.GPS);
		
	    Message Msg = new Message();
	    Bundle bundle = Msg.getData();
	    Msg.what = TxTelemetryService.GPS;
	        		        
	    bundle.putString("GPS",ts + "," + msgID + "," + GPSSentence);
	    Msg.setData(bundle);
	    if(TxTelemetryServiceConnectionIsBound)
	    {
	        try 
	        {
	        	TxTelemetryServiceConnectionMessenger.send(Msg);
	        } 
	    	catch (RemoteException e)
	    	{
	    		Log.e(TAG, "GPSUSBService -TxTelemetryServiceConnectionMessenger.send "+ e.getMessage());
	    		e.printStackTrace();
	    	}
	    }	
	}

	
	class GPSUSBServiceHandler extends Handler 
	{	
		   @Override 
		   public void handleMessage(Message msg)
		   { 	      
		      super.handleMessage(msg);
		   }

	}
	
	class TxTelemetryServiceHandler extends Handler 
	{
			
	   @Override 
	   public void handleMessage(Message msg)
	   { 
	      super.handleMessage(msg);
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
		        	Log.i(TAG, "GPSUSBService - TxTelemetryServiceConnection - SUCCESS");
		        	TxTelemetryServiceConnectionIsBound = true;
		        }
		        else
		        {
		        	Log.i(TAG, "GPSUSBService - TxTelemetryServiceConnection - FAIL");
		        	TxTelemetryServiceConnectionIsBound = false;
		        }
		   }
		     
		   @Override 
		   public void onServiceDisconnected(ComponentName arg0)
		   { 
			   TxTelemetryServiceConnectionMessenger = null; 
		   } 
	}
	 
	class SnapSatPayLoadManagerServiceHandler extends Handler
	{
										
       @Override 
		public void handleMessage(Message msg)
		{ 
		}
	}
	
	private Messenger GPSUSBServiceConnectionMessenger = new Messenger(new GPSUSBServiceHandler());
		
	@Override 
    public IBinder onBind(Intent intent)
     {
		    Log.i(TAG, "GPSUSBService - onBind");
		    GPSUSBServiceInit();
		    return GPSUSBServiceConnectionMessenger.getBinder();
	 }
		
	 @Override
	 public int onStartCommand(Intent intent, int flags, int startId)
	 {
	     Log.i(TAG, "GPSUSBService - onStartCommand");

	     return Service.START_STICKY;
	 }
	 
     @Override
	public void onDestroy() 
	{
    	 Log.i(TAG, "GPSUSBService - onDestroy");

         if(GPSThread != null)   
         {   
        	 GPSThread.interrupt();   
         }  
         
 		 unbindService(TxTelemetryService_conn);
		 super.onDestroy();
	
	}
}



