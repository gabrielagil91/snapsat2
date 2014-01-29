package com.example.snapsatpayload;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

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



public class FTPService extends Service
{

	private static final String TAG = "SnapSatPayload";
	
	private static Thread FTPThread        = null;
	private static Stack<String> ftpStack = new Stack<String>();
		
	private TxTelemetryServiceConnection TxTelemetryService_conn   = new TxTelemetryServiceConnection();
	private static Messenger TxTelemetryServiceConnectionMessenger = null;
	static boolean TxTelemetryServiceConnectionIsBound             = false;
	
	void FTPServiceInit()
	{
	    Log.i(TAG, "FTPServiceInit - Start");
		TxTelemetryServiceConnectionIsBound = bindService(new Intent(this, TxTelemetryService.class), TxTelemetryService_conn, Context.BIND_AUTO_CREATE);
		
		if(TxTelemetryServiceConnectionIsBound)
		{
			Log.i(TAG, "FTPService - bindService to TxTelemetryService - SUCCESS");  
		}
		else
		{
			Log.e(TAG, "FTPService - bindService to TxTelemetryService - FAIL"); 
		}
					 		    
	    TxTelemetryServiceConnectionMessenger = new Messenger(new TxTelemetryServiceHandler());
	    
	    StartFTPThrread();
	}
	
	
	//////////////////////////  F T P  S E C T I O N /////////////////////////////////////////////////////////
	
	public static void StartFTPThrread() 
	{ 
	    FTPThread = new ftpFileThread();
	    FTPThread.setPriority(Thread.MAX_PRIORITY);
	    FTPThread.start();  
	} 
	

	private static class ftpFileThread extends Thread
	{	
  		
		public void run() 
		{
      	  	Boolean ThreadAlive  = true;
  	  	    FTPClient ftpClient  = new FTPClient();
  	  	    ftpClient.setControlKeepAliveTimeout(300);
      	  	
      	  	while((!FTPThread.isInterrupted()))
      	  	{
      	  		try
      	  		{
      	  			if(ThreadAlive)
      	  			{
	      	  			synchronized(FTPThread)
	      	  			{
	      	  				wait();
	      	  			}
      	  			}
      	  			else
      	  			{
      	  				break;
      	  			}
	      	  		while (!ftpStack.empty())
	      	  		{
	
	      	  			String data                = ftpStack.pop();    	  			
				        String[] separated         = data.split(",");
				        String Clientfname         = separated[0];
				        String ServerFileName      = separated[1];
			      	  	
			      	  	if(SnapSatPayLoadManagerService.IsNetworkAvailable())
			      	  	{	        
			            	Log.i(TAG, "FTPService - ftpFile - Start Transfer");
			            	
			                if(ftpClient.isConnected())
			                {
				                Log.e(TAG, "FTPService - ftpClient.isConnected() - disconnect");
			                	ftpClient.disconnect();
			                }
		            				            	
			            	ftpClient.setConnectTimeout(3000); // 3 Seconds

			            	Log.i(TAG, "FTPService - ftpFile - connect"); 
		            		ftpClient.connect(MainActivity.FTPHostAddress, MainActivity.FTPport);
		            	    int reply = ftpClient.getReplyCode();
	
	
		            	    if(!FTPReply.isPositiveCompletion(reply)) 
			            	{
			                	ftpClient.disconnect();
			            		Log.e(TAG, "FTPService - setConnectTimeout - Gave up after 10 sec");
			            	}
			            	else
			            	{
	
			            		Log.i(TAG, "FTPService - ftpFile - login");	            		
				                if(ftpClient.login(MainActivity.FTPUserName, MainActivity.FTPPasssword))
				                {
					                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
					                Log.i(TAG, "FTPService - ftpFile - FileInputStream"); 
					          	  	BufferedInputStream buffIn = new BufferedInputStream(new FileInputStream(Clientfname));
					                if(SnapSatPayLoadManagerService.IsNetworkAvailable())
					                {
						                if(ftpClient.isAvailable())
						                {
						                	Log.i(TAG, "FTPService - ftpFile - store");
							            	ftpClient.setSoTimeout(10000); // 10 Sec to store file
						                	ftpClient.storeFile(ServerFileName, buffIn);
							                Log.i(TAG, "FTPService - ftpFile - Transfer Complete");
							                if(ftpClient.isConnected())
							                {
								                Log.i(TAG, "FTPService - ftpFile - logout");
								                ftpClient.logout();
								                if(ftpClient.isConnected())
								                {
								                	Log.i(TAG, "FTPService - ftpFile - disconnect");
								                	ftpClient.disconnect();
								                }
								                
								                Log.i(TAG, "FTPService - ftpFile - SendTransferComplete");
								                SendTransferComplete(ServerFileName);
							                }
						                }
					                }
					                Log.i(TAG, "FTPService - ftpFile - buffIn.close()");
					                buffIn.close();
				                }
			            	}
						}
			      	  	else
			      	  	{
			      	  		Log.e(TAG, "FTPService - Network Not Avaiable");
			      	  	}
		      	  	} //while(!ftpStack.empty())
				}//try
      	  		catch (SocketException e) 
      	  		{
      	  			Log.e(TAG, "FTPService - SocketException "+ e.getMessage());
      	  			e.printStackTrace();
		    	}

			    catch (IOException e) 
			    {
			    	Log.e(TAG, "FTPService - IOException "+ e.getMessage());
			    	e.printStackTrace();
			    }
			    catch (InterruptedException e2) 
			    {
			    	Log.i(TAG, "FTPService - ThreadAlive = false ");
			    	ThreadAlive = false;
			    }
			    catch (Exception e) 
			    {
			    	Log.e(TAG, "FTPService - Exception "+ e.getMessage());
			    	e.printStackTrace();
			    }
	    	
		    }//while((!FTPThread.isInterrupted()))
		    
			Log.i(TAG, "FTPService - ftpFileThread - FTPThread Ended");
			FTPThread = null;
		}
	}
		
	static void FTPFile(String ClientfName, String ServerFileName)
	{
		String data = ClientfName + "," + ServerFileName;
		Log.i(TAG, "FTPService - ftpFileThread - ftpStack.push(data)");
        ftpStack.push(data);
        synchronized(FTPThread)
        {
        	FTPThread.notify();
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
	
	static void SendTransferComplete(String ServerFileName)
	{
   	    SimpleDateFormat s = new SimpleDateFormat("yyyyMMddhhmmss");
   	    String ts = s.format(new Date());
   	    String msgID = Integer.toString(TxTelemetryService.CameraFile);
		
	    Message Msg = new Message();
	    Bundle bundle = Msg.getData();
	    Msg.what = TxTelemetryService.CameraFile;
	        		        
	    bundle.putString("CameraFile",ts + "," + msgID + "," + ServerFileName);
	    Msg.setData(bundle);
	    if(TxTelemetryServiceConnectionIsBound)
	    {
	        try 
	        {
	        	TxTelemetryServiceConnectionMessenger.send(Msg);
	        } 
	    	catch (RemoteException e)
	    	{
	    		Log.e(TAG, "FTPService - TxTelemetryServiceConnectionMessenger " + e.getMessage());
	    		e.printStackTrace();
	    	}
	    }	
	}

	
	class FTPServiceHandler extends Handler 
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
		        	Log.i(TAG, "FTPService - TxTelemetryServiceConnection - SUCCESS");
		        	TxTelemetryServiceConnectionIsBound = true;
		        }
		        else
		        {
		        	Log.e(TAG, "FTPService - TxTelemetryServiceConnection - FAIL");
		        	TxTelemetryServiceConnectionIsBound = false;
		        }
		   }
		     
		   @Override 
		   public void onServiceDisconnected(ComponentName arg0)
		   { 
			   TxTelemetryServiceConnectionMessenger = null; 
		   } 
	}
	
	
    private Messenger messenger = new Messenger(new FTPServiceHandler());
	
	@Override 
	public IBinder onBind(Intent intent)
	{
       Log.i(TAG, "FTPService - onBind");
       
       FTPServiceInit();

       return messenger.getBinder();
	}
	
    @Override
	public void onDestroy() 
	{
    	Log.i(TAG, "FTPService - onDestroy");
    	
        if(FTPThread != null)   
        {             
       	 	FTPThread.interrupt(); 
        }
        
		unbindService(TxTelemetryService_conn);
		super.onDestroy();
	}
}
