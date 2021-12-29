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

package com.tritop.androsense2.helpers;

import java.text.DecimalFormatSymbols;

import com.tritop.androsense2.R;

import android.content.Context;

public class SensorInfo {
	
	public static final int TYPE_ACCELEROMETER = 1;
	public static final int TYPE_ALL = -1;
	public static final int TYPE_AMBIENT_TEMPERATURE = 13;
	public static final int TYPE_GAME_ROTATION_VECTOR = 15;
	public static final int TYPE_GEOMAGNETIC_ROTATION_VECTOR = 20;
	public static final int TYPE_GRAVITY = 9;
	public static final int TYPE_GYROSCOPE = 4;
	public static final int TYPE_GYROSCOPE_UNCALIBRATED = 16;
	public static final int TYPE_LIGHT = 5;
	public static final int TYPE_LINEAR_ACCELERATION = 10;
	public static final int TYPE_MAGNETIC_FIELD = 2;
	public static final int TYPE_ORIENTATION = 3;
	public static final int TYPE_PRESSURE = 6;
	public static final int TYPE_PROXIMITY = 8;
	public static final int TYPE_RELATIVE_HUMIDITY = 12;
	public static final int TYPE_ROTATION_VECTOR = 11;
	public static final int TYPE_SIGNIFICANT_MOTION = 17;
	public static final int TYPE_STEP_COUNTER = 19;
	public static final int TYPE_STEP_DETECTOR = 18;
	public static final int TYPE_TEMPERATURE = 7;
	public static final int TYPE_MAGNETIC_FIELD_UNCALIBRATED=14;
	
	
	private static SensorInfo me = new SensorInfo();
	
	private SensorInfo() {
	}
	
    public static SensorInfo getInstance() {
    		return me;
    }
    
	/**
	 * Return a string representing the type of the sensor
	 * @param ctx
	 * @param sensorType
	 * @return
	 */
	public static String getStringType(Context ctx, int  sensorType){
		switch(sensorType){
			case TYPE_ACCELEROMETER: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_ACCELEROMETER);
			case TYPE_ALL: 	return ctx.getResources().getString(R.string.SENSORINFO_TYPE_ALL);
			case TYPE_AMBIENT_TEMPERATURE: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_AMBIENT_TEMPERATURE);
			case TYPE_GAME_ROTATION_VECTOR: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_GAME_ROTATION_VECTOR);
			case TYPE_GEOMAGNETIC_ROTATION_VECTOR: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_GEOMAGNETIC_ROTATION_VECTOR);
			case TYPE_GRAVITY: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_GRAVITY);
			case TYPE_GYROSCOPE: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_GYROSCOPE);
			case TYPE_GYROSCOPE_UNCALIBRATED: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_GYROSCOPE_UNCALIBRATED);
			case TYPE_LIGHT: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_LIGHT);
			case TYPE_LINEAR_ACCELERATION: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_LINEAR_ACCELERATION);
			case TYPE_MAGNETIC_FIELD: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_MAGNETIC_FIELD);
			case TYPE_MAGNETIC_FIELD_UNCALIBRATED: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_MAGNETIC_FIELD_UNCALIBRATED);
			case TYPE_ORIENTATION: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_ORIENTATION);
			case TYPE_PRESSURE: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_PRESSURE);
			case TYPE_PROXIMITY: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_PROXIMITY);
			case TYPE_RELATIVE_HUMIDITY: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_RELATIVE_HUMIDITY);
			case TYPE_ROTATION_VECTOR: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_ROTATION_VECTOR);
			case TYPE_SIGNIFICANT_MOTION: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_SIGNIFICANT_MOTION);
			case TYPE_STEP_COUNTER: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_STEP_COUNTER);
			case TYPE_STEP_DETECTOR: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_STEP_DETECTOR);
			case TYPE_TEMPERATURE: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_TEMPERATURE);
			default: return ctx.getResources().getString(R.string.SENSORINFO_TYPE_UNKNOWN);
		}
	}
	
	
	public static String getUnitType(int  sensorType){
		switch(sensorType){
			case TYPE_ACCELEROMETER: return "m/s^2";
			case TYPE_ALL: 	return "";
			case TYPE_AMBIENT_TEMPERATURE: return "C";
			case TYPE_GAME_ROTATION_VECTOR: return "";
			case TYPE_GEOMAGNETIC_ROTATION_VECTOR: return "";
			case TYPE_GRAVITY: return "m/s^2";
			case TYPE_GYROSCOPE: return "rad/s";
			case TYPE_GYROSCOPE_UNCALIBRATED: return "rad/s";
			case TYPE_LIGHT: return "lux";
			case TYPE_LINEAR_ACCELERATION: return "m/s^2";
			case TYPE_MAGNETIC_FIELD: return "T";
			case TYPE_MAGNETIC_FIELD_UNCALIBRATED: return "T";
			case TYPE_ORIENTATION: return "o";
			case TYPE_PRESSURE: return "hPa";
			case TYPE_PROXIMITY: return "cm";
			case TYPE_RELATIVE_HUMIDITY: return "%";
			case TYPE_ROTATION_VECTOR: return "";
			case TYPE_SIGNIFICANT_MOTION: return "";
			case TYPE_STEP_COUNTER: return "";
			case TYPE_STEP_DETECTOR: return "";
			case TYPE_TEMPERATURE: return "C";
			default: return "";
		}
	}
	
	public static byte getSignificantValues(int sensorType){

		switch(sensorType){
			case TYPE_ACCELEROMETER: return 3;
			case TYPE_ALL: 	return 0;
			case TYPE_AMBIENT_TEMPERATURE: return 1;
			case TYPE_GAME_ROTATION_VECTOR: return 3;
			case TYPE_GEOMAGNETIC_ROTATION_VECTOR: return 3;
			case TYPE_GRAVITY: return 3;
			case TYPE_GYROSCOPE: return 3;
			case TYPE_GYROSCOPE_UNCALIBRATED: return 6;
			case TYPE_LIGHT: return 1;
			case TYPE_LINEAR_ACCELERATION: return 3;
			case TYPE_MAGNETIC_FIELD: return 3;
			case TYPE_MAGNETIC_FIELD_UNCALIBRATED: return 6;
			case TYPE_ORIENTATION: return 3;
			case TYPE_PRESSURE: return 1;
			case TYPE_PROXIMITY: return 1;
			case TYPE_RELATIVE_HUMIDITY: return 1;
			case TYPE_ROTATION_VECTOR: return 4;
			case TYPE_SIGNIFICANT_MOTION: return 1;
			case TYPE_STEP_COUNTER: return 1;
			case TYPE_STEP_DETECTOR: return 1;
			case TYPE_TEMPERATURE: return 1;
			default: return 0;
		}
	}
	
	public static String formatSensorfloat(float sFloat,int res){
		return String.format("%."+res+"f",sFloat).replaceAll("\\.?0*$", "").replaceAll(DecimalFormatSymbols.getInstance().getDecimalSeparator()+"$", "");
	}
	
	public static String formatGpsPosition(String gpsString){
		return gpsString.replaceFirst(":","o ").replaceFirst(":", "' ").replaceFirst(",", "''").replaceAll("\\d*$", "");
	}
}
