/*
    Copyright (C) 2013-2014 Christian Schneider
    christian.d.schneider@googlemail.com
    
    This file is part of Androsens 2.

    Androsens 2 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Androsens 2 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Androsens 2.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.tritop.androsense2.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tritop.androsense2.AndrosensApp;
import com.tritop.androsense2.R;
import com.tritop.androsense2.helpers.SensorInfo;
import com.tritop.androsense2.log.GpsLog;
import com.tritop.androsense2.log.SensorLog;
import com.tritop.androsense2.log.LogItem.LogCore;
import com.tritop.androsense2.views.SatView;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;

public class GpsFragment extends Fragment implements LocationListener, Listener, SensorEventListener, OnClickListener {

	
	public static final String SENSORTYPE = "GPS";
	public static final String SAT_PNR_KEY = "PNR";
	public static final String SAT_FIX_KEY = "FIX";
	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
    private LocationListener locListener;
    private GpsStatus.Listener gpslistener;
    private Sensor defaultAccel,defaultMag;
    private float[] mAccel;
    private float[] mMag;
    private SatView satView;
    private TextView mLatitudeView;
    private TextView mAltitudeView;
    private TextView mLongitudeView;
    
    private double mLatitude=0;
    private double mAltitude=0;
    private double mLongitude=0;
    private double mAccuracy=0;
    
    private float aRotation=0;
    private GpsLog mGpsLog;
    
    private ToggleButton mLogToggle;
	private boolean isLogging;
    
	
	private int[] boundViews = new int[]{R.id.gpsfragment_sat_item_tv,R.id.gpsfragment_sat_item_tv};
	private String[] boundRows = new String[]{SAT_PNR_KEY,SAT_FIX_KEY};
	
	
	private GridView gpsGrid;
	private GridView glonassGrid;
	
	private SimpleAdapter mGpsListAdapter;
	private SimpleAdapter mGlonassListAdapter;
	private List<Map<String,String>> itemListGps = new ArrayList<Map<String,String>>();
	private List<Map<String,String>> itemListGlonass = new ArrayList<Map<String,String>>();
	
	Display display;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		mSensorManager = ((AndrosensApp)getActivity().getApplication()).getSensorManager();
		defaultAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		defaultMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mGpsListAdapter = new SimpleAdapter(getActivity(), itemListGps, R.layout.gpsfragment_sat_item, boundRows, boundViews);
		mGlonassListAdapter = new SimpleAdapter(getActivity(), itemListGlonass, R.layout.gpsfragment_sat_item, boundRows, boundViews);
		setupViewBinders(mGpsListAdapter,R.drawable.shape_satitem_background_nofix,R.drawable.shape_satitem_background_fix);
		setupViewBinders(mGlonassListAdapter,R.drawable.shape_satitem_background_glonass_nofix,R.drawable.shape_satitem_background_glonass_fix);
		display = getActivity().getWindowManager().getDefaultDisplay();
		return inflater.inflate(R.layout.gpsfragment_layout, container,false);
	}

	
    public void pauseSensors(){
    	mLocationManager.removeUpdates(this);
		mLocationManager.removeGpsStatusListener(this);
		mSensorManager.unregisterListener(this);
    }
	
	@Override
	public void onPause() {
		pauseSensors();
		super.onPause();
	}

	public void resumeSensors(){
		mSensorManager.registerListener(this, defaultAccel, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, defaultMag, SensorManager.SENSOR_DELAY_UI);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		mLocationManager.addGpsStatusListener(this);
	}
	
	
	
	@Override
	public void onResume() {
		super.onResume();
		gpsGrid = (GridView) getActivity().findViewById(R.id.gpsfragment_grid_gps);
		glonassGrid = (GridView) getActivity().findViewById(R.id.gpsfragment_grid_glonass);
		mLatitudeView = (TextView) getActivity().findViewById(R.id.gpsfragment_tv_position_lat_value);
	    mAltitudeView = (TextView) getActivity().findViewById(R.id.gpsfragment_tv_position_alt_value);
	    mLongitudeView = (TextView) getActivity().findViewById(R.id.gpsfragment_tv_position_lon_value);
	    gpsGrid.setAdapter(mGpsListAdapter);
	    glonassGrid.setAdapter(mGlonassListAdapter);
		satView = (SatView) getActivity().findViewById(R.id.view_gpsSatView);
		processSmoothnessPref();
		resumeSensors();
		
		
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mLogToggle.isChecked() && (mGpsLog != null)){
			outState.putParcelable("logcore", mGpsLog.dumpCore());
			isLogging = true;
		}
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mLogToggle = (ToggleButton) getActivity().findViewById(R.id.gpsfragment_toggleButton_log);
		mLogToggle.setOnClickListener(this);
		if(mLogToggle.isChecked()){	
			try {
				LogCore core = savedInstanceState.getParcelable("logcore");
				mGpsLog = new GpsLog(SENSORTYPE);
				mGpsLog.restoreCore(core);
				isLogging = true;
			} catch (IOException e) {
				e.printStackTrace();
				mLogToggle.setChecked(false);
				isLogging = false;
			}
		}
	}


	@Override
	public void onLocationChanged(Location loc) {
		mLatitude = loc.getLatitude();
	    mAltitude = loc.getAltitude();
	    mLongitude = loc.getLongitude();
	    mAccuracy = loc.getAccuracy();
		mLatitudeView.setText(SensorInfo.formatGpsPosition(Location.convert(mLatitude,Location.FORMAT_SECONDS)));
		mLongitudeView.setText(SensorInfo.formatGpsPosition(Location.convert(mLongitude,Location.FORMAT_SECONDS)));
		mAltitudeView.setText(String.format("%.0f", mAltitude)+" m");
	}


	
	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onGpsStatusChanged(int arg0) {
		long triggerTime = System.currentTimeMillis();
		int gpsSatellitesCount=0;
		int glonassSatellitesCount=0;
		HashMap<String,String> mSatInfo;
		itemListGps.clear();
		itemListGlonass.clear();
		GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
		Iterable<GpsSatellite> gpsSats = gpsStatus.getSatellites();
		for(GpsSatellite sat: gpsSats){
			if(sat.getPrn()<=32){
				gpsSatellitesCount++;
				mSatInfo = new HashMap<String,String>();
				mSatInfo.put(SAT_PNR_KEY, String.valueOf(sat.getPrn()));
				String usedinfix = (sat.usedInFix())?"TRUE":"FALSE";
				mSatInfo.put(SAT_FIX_KEY, usedinfix);
				itemListGps.add(mSatInfo);
			}
			else {
				glonassSatellitesCount++;
				mSatInfo = new HashMap<String,String>();
				mSatInfo.put(SAT_PNR_KEY, String.valueOf(sat.getPrn()));
				String usedinfix = (sat.usedInFix())?"TRUE":"FALSE";
				mSatInfo.put(SAT_FIX_KEY, usedinfix);
				itemListGlonass.add(mSatInfo);
			}
		}
		if(isLogging && mGpsLog != null){
			try {
				mGpsLog.logEvent(triggerTime,gpsSatellitesCount+glonassSatellitesCount,gpsSats,mLatitude, mLongitude, mAltitude, mAccuracy);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		satView.setSatellites(gpsSats);
		mGpsListAdapter.notifyDataSetChanged();
		mGlonassListAdapter.notifyDataSetChanged();
		satView.invalidate();
	}


	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		
	}


	@Override
	public void onSensorChanged(SensorEvent event) {
	    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
	    	mAccel = event.values;
	    }
	    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
	    	mMag = event.values;
	    }
		if((mAccel != null) && (mMag != null)){
			float R[] = new float[9];
		    float I[] = new float[9];
		    float Rot[] = new float[9];
		    if(SensorManager.getRotationMatrix(R, I, mAccel, mMag)){
		    	float orientation[] = new float[3];
		    	int axisX=0,axisY=0;
		    	switch (display.getRotation()) {
		        case Surface.ROTATION_0:
		            axisX = SensorManager.AXIS_X;
		            axisY = SensorManager.AXIS_Y;
		            break;
		        case Surface.ROTATION_90:
		            axisX = SensorManager.AXIS_Y;
		            axisY = SensorManager.AXIS_MINUS_X;
		            break;
		        case Surface.ROTATION_180:
		            axisX = SensorManager.AXIS_MINUS_X;
		            axisY = SensorManager.AXIS_MINUS_Y;
		            break;
		        case Surface.ROTATION_270:
		            axisX = SensorManager.AXIS_MINUS_Y;
		            axisY = SensorManager.AXIS_X;
		            break;
		        default:
		            break;
		    	}
	
		    	SensorManager.remapCoordinateSystem(R, axisX,axisY, Rot);
		        SensorManager.getOrientation(Rot, orientation);
		        aRotation= orientation[0];
		        aRotation = (int) Math.toDegrees(aRotation);
		        satView.setAzimutRotation(-aRotation);
				satView.invalidate();
		    }
		}
	}

	
	public void processSmoothnessPref(){
		setNewlowpassSmoothness(getLowPassSmoothnessPref());
	}
	
	public void setNewlowpassSmoothness(float smooth){
		if(satView != null){
			smooth = (smooth>1)?1:smooth;
			smooth = (smooth<0)?0:smooth;
			satView.setLowPassSmoothness(smooth);
		}
	}
	
	private float getLowPassSmoothnessPref(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
	    return  Float.valueOf(settings.getString("smoothness", "0.1"));
	}
	
	private void setupViewBinders(SimpleAdapter adapter, final int stdColorResource,final int fixColorResource){
		adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Object data,
					String textRepresentation) {
				String gpsdata;
				switch(view.getId()){
					case R.id.gpsfragment_sat_item_tv: gpsdata = (String) data;
													   if(gpsdata.equals("TRUE")){
														   view.setBackgroundResource(fixColorResource);
													   }
													   else if(gpsdata.equals("FALSE")){
														   view.setBackgroundResource(stdColorResource);
													   }
													   else {
														   ((TextView)view).setText(gpsdata);
													   }
													   return true;
					default: return false;
				}
			}
		});
	}
	
	private String readDirectoryPreference(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		return settings.getString("logfiledirectory", "Androsens");
	}
	
	private void toggleLog(boolean log){
		if(log){
			startLog();
		}
		else {
			stopLog();
		}
	}
	
	private void startLog(){
		try {
			mGpsLog = new GpsLog(SENSORTYPE, readDirectoryPreference());
		} catch (IOException e) {
			e.printStackTrace();
			mLogToggle.setChecked(false);
			isLogging = false;
		}
	}
	
	private void stopLog(){
		if(mGpsLog != null){
			try {
				mGpsLog.close(getActivity());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.gpsfragment_toggleButton_log:isLogging = mLogToggle.isChecked();toggleLog(isLogging);break;
		default:break;
		}
		
	}

}
