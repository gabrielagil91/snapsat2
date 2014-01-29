package com.example.snapsatpayload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver 
{
	private static final String TAG     = "SnapSatPayload";

	@Override
	public void onReceive(Context context, Intent intent) 
	{
    	Log.i(TAG, "******************");
    	Log.i(TAG, "START MainActivity");
    	Log.i(TAG, "******************");
		Intent App = new Intent(context, MainActivity.class);
		App.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(App);
	}

}
