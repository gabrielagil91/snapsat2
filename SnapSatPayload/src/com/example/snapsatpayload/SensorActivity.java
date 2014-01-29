package com.example.snapsatpayload;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;


public class SensorActivity extends Activity implements SensorEventListener {
	private SensorManager mSensorManager;
	private Sensor mPressure;

	@Override
	public final void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Get an instance of the sensor service, and use that to get an instance
		// of a particular sensor
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
	}
	
	public void sensorInit(){
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		Log.i("Sensor test", mPressure.getName());
	}
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// Do something here if sensor accuracy changes
		
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		float millibars_of_pressure = event.values[0];
		Log.d("Sensor Data", "pressure: "+millibars_of_pressure);
	
		int accuracy = event.accuracy;
		long timestamp = event.timestamp;
		// TODO print/save/transfer the sensor data?
	}
	
	@Override
	public void onResume() {
		// Register a listener for the sensor
		super.onResume();
		mSensorManager.registerListener(this,  mPressure, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	public void onPause() {
		// Be sure to unregister the sensor when the activity pauses
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

}
