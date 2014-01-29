package com.example.snapsatpayload;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.FrameLayout;

public class CamPreview extends TextureView implements SurfaceTextureListener 
{
	private static final String TAG = "SnapSatPayload";
	

	 private Camera mCamera;
	 private SurfaceTexture _surface  = null;

 
	  public CamPreview(Context context, Camera camera) 
	  
	  {
	    super(context);
	    mCamera = camera;  
	    
	  }
	  
	  public void TakePicture(String ClientfName, String ServerFileName)
	  {
		  if(_surface != null)
		  {
		    Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
		    setLayoutParams(new FrameLayout.LayoutParams(
		        previewSize.width, previewSize.height, Gravity.CENTER));
		    
			JPEGCallback jpegCallback = new JPEGCallback(ClientfName, ServerFileName);

		    try
		    {
		    	
		        mCamera.setPreviewTexture(_surface);
		        
			    mCamera.startPreview();
			    
			    Thread.sleep(100);
			    
			    //this.setVisibility(INVISIBLE); // Make the surface invisible as soon as it is created
			    
			    
			    mCamera.takePicture(null, null, jpegCallback);
			    Log.i(TAG, "onSurfaceTextureAvailable - Take Picture: ");
			   
		    } 
		    catch (IOException t) 
		    {
		    	Log.e(TAG, "onSurfaceTextureAvailable - : " + t.getMessage());
		    } 
		    catch 
		    (InterruptedException e) 
		    {
		    	Log.e(TAG, "onSurfaceTextureAvailable - : InterruptedException" + e.getMessage());
				e.printStackTrace();
			} 
		  }
	  }
	  
  
		/** Handles data for jpeg picture */
		//PictureCallback jpegCallback = new PictureCallback() 
		private class JPEGCallback implements PictureCallback
		{
			String _ClientfName    = null;
			String _ServerFileName = null;
			
			public JPEGCallback(String ClientfName, String ServerFileName)
			{
				_ClientfName    = ClientfName;
				_ServerFileName = ServerFileName;		
			}
			
			public void onPictureTaken(byte[] data, Camera camera) 
			{
				Log.i(TAG, "onPictureTaken - Start");
				FileOutputStream outStream = null;
				try 
				{
					outStream = new FileOutputStream(_ClientfName);
					outStream.write(data);
					Log.i(TAG, "onPictureTaken - outStream.flush()");
					outStream.flush();
					outStream.close();
					Log.i(TAG, "onPictureTaken - wrote bytes: " + data.length+ " "+ _ClientfName);
				
					FTPService.FTPFile(_ClientfName,_ServerFileName);
									
    			} 
				catch (FileNotFoundException e) 
				{
					Log.e(TAG, "onPictureTaken - "+ e.getMessage());
					e.printStackTrace();
				} 
				catch (IOException e) 
				{
					Log.e(TAG, "onPictureTaken - "+ e.getMessage());
					e.printStackTrace();
				}
			}
		};


	  @Override
	  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) 
	  {
		  _surface = surface;
	    
	  }

	  @Override
	  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
	  {
	      // Put code here to handle texture size change if you want to
	  }

	  @Override
	  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) 
	  {
	      Log.i(TAG, "onSurfaceTextureDestroyed - Release ");
		  
		  surface.release();
		  
	      return true;
	  }

	  @Override
	  public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	      // Update your view here!
	  }

}