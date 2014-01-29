package com.example.snapsatpayload;



import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.widget.FrameLayout;

public class MainActivity extends Activity
{
	public  static final String VERSION = "SnapSat Payload Version 1.0";
	private static final String TAG     = "SnapSatPayload";
	boolean SnapSatPayLoadManagerServiceIsBound = false;
		
	private Messenger SnapSatPayLoadManagerServiceConnectionMessenger; 
	private static Intent SnapSatPayLoadManagerServiceIntent = null;
	
	private FTPServiceConnection FTPService_conn           = new FTPServiceConnection();
	private static Messenger FTPServiceConnectionMessenger = null;
	static boolean FTPServiceConnectionIsBound             = false;
	
	private static FrameLayout PreviewSurface    = null;
	private static Camera SnapSatCamera          = null;
	private static CamPreview camPreview         = null;
	private static Context context               = null;
	
	private WakeLock wakeLock;
	
	private Timer CameraTimer                  = new Timer();
	private CameraTimerTask cameraTimer        = new CameraTimerTask();
	public static String CameraDir             = "/sdcard/DCIM/camera";
	
    private static SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddhhmmss");
	private static String ts = simpleDate.format(new Date());
	
	public static final String UDPaddress      = "192.168.1.115";
	public static final int UDPport            = 4444;
	
	public static final String FTPHostAddress   = "192.168.1.115";
	public static final int    FTPport          = 21;
	public static final String FTPUserName      = "gswiech";
	public static final String FTPPasssword     = "";
	
	private SensorManager mSensorManager;
	private Sensor mPressure;
	//private Sensor mPressure; accel, magnatometer, gyrescope, humidity, temperature
	@Override 
	public void onCreate(Bundle savedInstanceState)
	{	
		Log.i(TAG, "MainActivity - onCreate");
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.activity_main);
		
	
		// Get an instance of the sensor service, and use that to get an instance
		// of a particular sensor
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);	
		Log.i("Sensor test", mPressure.getName());
	  // SensorActivity s = new SensorActivity();
	    
	
	    
		Log.i(TAG, VERSION);
		
		context = this;
        
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Lock");
        wakeLock.acquire();
	   
		PreviewSurface           = (FrameLayout ) findViewById(R.id.PreviewSurface);
		
		if(SnapSatCamera == null)
		{
			SnapSatCamera = Camera.open();
		}
		
		if(camPreview == null)
		{	
			camPreview     = new CamPreview(MainActivity.this, SnapSatCamera);
	    	camPreview.setSurfaceTextureListener(camPreview);
	    	PreviewSurface.addView(camPreview);
		}
			      
        
		if(SnapSatPayLoadManagerServiceIntent == null)
		{
		    SnapSatPayLoadManagerServiceIntent = new Intent(this, SnapSatPayLoadManagerService.class);
		    startService(SnapSatPayLoadManagerServiceIntent);
		}	
		
		FTPServiceConnectionIsBound = bindService(new Intent(this, FTPService.class), FTPService_conn, Context.BIND_AUTO_CREATE);	
		if(FTPServiceConnectionIsBound)
		{
			Log.i(TAG, "MainActivity - bindService to FTPService - SUCCESS");  
		}
		else
		{
			Log.e(TAG, "MainActivity - bindService to FTPService - FAIL"); 
		}	
		   
		// 11  seconds timer
		CameraTimer.schedule(cameraTimer, 11000, 11000);
		Log.i(TAG, "MainActivity - Camera Timer started");
		
	    File dir = new File(CameraDir);
	    if(dir.exists() && dir.isDirectory())
	    {
	    	Log.i(TAG, "MainActivity - Exists "+ CameraDir);
	    }
	    else
	    {
	    	dir.mkdir();
	    	Log.i(TAG, "MainActivity - Create Dir "+ CameraDir);
	    }
	    
	    dir = new File(MainActivity.getLogDir());
	    if(dir.exists() && dir.isDirectory())
	    {
	    	Log.i(TAG, "MainActivity - Exists "+ MainActivity.getLogDir());
	    }
	    else
	    {
	    	dir.mkdir();
	    	Log.i(TAG, "MainActivity - Create Dir "+ MainActivity.getLogDir());
	    }
	   
	}
	

	@Override 
	protected void onDestroy()
	{
		Log.i(TAG, "MainActivity - onDestroy");
		
		super.onDestroy();
		
        wakeLock.release();
		
		cameraTimer.cancel();
		
		PreviewSurface.removeViewInLayout(camPreview);
		

		stopService(SnapSatPayLoadManagerServiceIntent);
		unbindService(FTPService_conn);
		
		if (SnapSatCamera != null) 
	    {
			SnapSatCamera.release();
			SnapSatCamera = null;
		}
		
		SnapSatPayLoadManagerServiceIntent    = null;
		camPreview                            = null;		

	}
	
    @Override
    protected void onRestart() 
    {
    	super.onRestart();
    	
 	   if(SnapSatCamera == null)
 	   {
 		   SnapSatCamera     = Camera.open();
 	   }
 	   
 	   Log.i(TAG, "MainActivity - onRestart");
    }
    
    @Override
    protected void onResume() 
    {
 	   super.onResume();
 	   
	   if(SnapSatCamera == null)
	   {
		   SnapSatCamera     = Camera.open();
	   }
 	   
 	   Log.i(TAG, "MainActivity - onResume");

    }
    
    @Override
    protected void onPause() 
    {
    	super.onPause();
    	
		if (SnapSatCamera != null) 
	    {
			SnapSatCamera.release();
			SnapSatCamera = null;
		}
		
    	Log.i(TAG, "MainActivity - onPause");
    }
    
    @Override
    protected void onStart() 
    {
    	super.onStart();
    	
		if(SnapSatCamera == null)
		{
			SnapSatCamera = Camera.open();
		}
		
		if(camPreview == null)
		{	
			camPreview     = new CamPreview(MainActivity.this, SnapSatCamera);
	    	camPreview.setSurfaceTextureListener(camPreview);
	    	PreviewSurface.addView(camPreview);
		}
		
    	Log.i(TAG, "MainActivity - onStart");		
    }
    
    @Override
    protected void onStop() 
    {
    	super.onStop();
    	
		if (SnapSatCamera != null) 
	    {
			SnapSatCamera.release();
			SnapSatCamera = null;
		}
		
    	Log.i(TAG, "MainActivity - onStop");
    }
    
    static String getLogFileName()
    {
    	return String.format("SnapSat%s.log", ts);
    }
    
    static String getLogDir()
    {
    	return String.format("/sdcard/log/");
    }
    
	class SnapSatPayLoadManagerServiceConnection implements ServiceConnection
	{ 
	   @Override 
	   public void onServiceConnected(ComponentName arg0, IBinder binder)
	   {
		   Log.i(TAG, "MainActivity - SnapSatPayLoadManagerService onServiceConnected - SUCCESS");
		   SnapSatPayLoadManagerServiceConnectionMessenger = new Messenger(binder);
	        if(SnapSatPayLoadManagerServiceConnectionMessenger != null)
	        {
	        	Log.i(TAG, "MainActivity - SnapSatPayLoadManagerService onServiceConnected - SUCCESS");
	        	SnapSatPayLoadManagerServiceIsBound = true;
	        }
	        else
	        {
	        	Log.e(TAG, "MainActivity - SnapSatPayLoadManagerService onServiceConnected - FAIL");
	        	SnapSatPayLoadManagerServiceIsBound = false;
	        }
	   }

	   @Override
	   public void onServiceDisconnected(ComponentName name)
	   {
		   SnapSatPayLoadManagerServiceConnectionMessenger = null;	
	   }
	}
	
 
	class FTPServiceConnection implements ServiceConnection
	 { 
		@Override 
		public void onServiceConnected(ComponentName arg0, IBinder binder)
		{
			FTPServiceConnectionMessenger = new Messenger(binder);
		        if(FTPServiceConnectionMessenger != null)
		        {
		        	Log.i(TAG, "MainActivity - FTPServiceConnection - SUCCESS");
		        	FTPServiceConnectionIsBound = true;
		        }
		        else
		        {
		        	Log.e(TAG, "MainActivity - FTPServiceConnection - FAIL");
		        	FTPServiceConnectionIsBound = false;
		        }
		   }
		     
		   @Override 
		   public void onServiceDisconnected(ComponentName arg0)
		   { 
			   FTPServiceConnectionMessenger = null; 
		   } 
	}
	 
	//////////////////////////  C A M E R A  S E C T I O N ///////////////////////////////////////////////////
		
    static Runnable TakePicture = new Runnable() 
    {
        public void run() 
        {
	 	    try 
		    {
 
	 	    	Parameters SnapSatCameraParam = SnapSatCamera.getParameters();
	    	    SnapSatCamera.setParameters(SnapSatCameraParam);
	    	    
	    	    // Check what resolutions are supported
	    	    List<Size> sizes = SnapSatCameraParam.getSupportedPictureSizes();
	    	    	    	    
	    	    // Iterate through all available resolutions and pick highest
	    	    int width  = 0;
	    	    int height = 0;	    
	    	    for (Size size : sizes) 
	    	    {
	    	    	
	    	    	if (size.width > width)
	    	    	{
	    	    		width  = size.width;
	    	    		height = size.height;
	    	    	}
	    	    }
	    	    
	    		SnapSatCameraParam.setPictureSize(width, height);
	    	    SnapSatCamera.setParameters(SnapSatCameraParam);
	    		Log.i(TAG, "MainActivity: Set Camera Resolution "+ width+" "+ height);

                long timeStamp = System.currentTimeMillis();
                String ServerFileName = String.format("SnapSat%d.jpg", timeStamp);
				String ClientfName    = String.format(CameraDir + "/SnapSat%d.jpg", timeStamp);
		    	  	
				camPreview.TakePicture(ClientfName,ServerFileName);
				
		    } 
		    catch (Exception e) 
		    {
		    	Log.e(TAG, "MainActivity - TakePicture" + e.getMessage());

				if(SnapSatCamera != null)
				{
					SnapSatCamera.release();
					SnapSatCamera = Camera.open();
					camPreview     = new CamPreview(context, SnapSatCamera);
			    	camPreview.setSurfaceTextureListener(camPreview);
			    	PreviewSurface.addView(camPreview);
				}
				
	        	Log.e(TAG, "MainActivity - reconnect Camera");

		    }
        }

    };
    
	private class CameraTimerTask extends TimerTask
	{
		
		@Override
		public void run()
		{
			Log.i(TAG, "MainActivity: Set Camera CameraTimerTask ");
			Handler takePictureHandler = new Handler(Looper.getMainLooper());
			takePictureHandler.post(TakePicture); 
		}   	
	}

	 
	class SnapSatPayLoadManagerServiceHandler extends Handler
	{
										
	    @Override 
	    public void handleMessage(Message msg)
		{ 
	
		}
	}
}
