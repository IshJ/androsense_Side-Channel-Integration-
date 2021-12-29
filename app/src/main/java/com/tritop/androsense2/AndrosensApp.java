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

package com.tritop.androsense2;

import java.util.List;
import java.util.Vector;

import android.app.Application;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class AndrosensApp extends Application {
	private List<Sensor> mSensorList= new Vector<Sensor>();
	private SensorManager mSensorManager;
	

	public void setSensorManager(SensorManager mgr){
		this.mSensorManager = mgr;
	}
	
	public SensorManager getSensorManager(){
		return this.mSensorManager;
	}
	
	public void setSensorList(List<Sensor> list){
		this.mSensorList.clear();
		this.mSensorList.addAll(list);
	}
	
	public List<Sensor> getSensorList(){
		return this.mSensorList;
	}
	
	public Sensor getSensor(int index){
		if(index <0 || index >= mSensorList.size()) return null;
		else return mSensorList.get(index);
	}
}
