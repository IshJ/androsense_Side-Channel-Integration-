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

package com.tritop.androsense2.log;

import java.io.IOException;
import java.util.LinkedList;

import android.content.Context;
import android.location.GpsSatellite;

public class GpsLog extends LogItem {
	
	LinkedList<String> data= new LinkedList<String>();
	
	
	public GpsLog(String type) throws IOException {
		super(type);
		this.setmLogId(false);
		this.setmLogTimestamp(false);
	}
	
	public GpsLog(String type, String directoryName) throws IOException {
		super(type, directoryName);
		this.setmLogId(false);
		this.setmLogTimestamp(false);
		writeHeader();
	}

	
	/**
	 * Writes CVS header to our logfile
	 * @throws IOException
	 */
	public void writeHeader() throws IOException{
		this.data.clear();
		this.data.add("TIME");
		this.data.add("LATITUDE");
		this.data.add("LONGITUDE");
		this.data.add("ALTITUDE");
		this.data.add("ACCURACY");
		this.data.add("TOTAL SATELLITES");
		this.data.add("USED IN FIX");
		this.data.add("PRN");
		this.data.add("SNR");
		this.data.add("AZIMUTH");
		this.data.add("ELEVATION");
		this.log(this.data);
	}
	
	
	/**
	 * Logs a new gps event
	 * @param time
	 * @param satcount
	 * @param gpsSats
	 * @param latitude
	 * @param longitude
	 * @param altitude
	 * @param acc
	 * @throws IOException
	 */
	public void logEvent(long time,int satcount, Iterable<GpsSatellite> gpsSats,double latitude, double longitude, double altitude, double acc) throws IOException{
		
		for(GpsSatellite satellite:gpsSats){
			this.data.clear();
			this.data.add(String.valueOf(time));
			this.data.add(String.valueOf(latitude));
			this.data.add(String.valueOf(longitude));
			this.data.add(String.valueOf(altitude));
			this.data.add(String.valueOf(acc));
			this.data.add(String.valueOf(satcount));
			this.data.add(String.valueOf(satellite.usedInFix()));
			this.data.add(String.valueOf(satellite.getPrn()));
			this.data.add(String.valueOf(satellite.getSnr()));
			this.data.add(String.valueOf(satellite.getAzimuth()));
			this.data.add(String.valueOf(satellite.getElevation()));
			this.log(this.data);
		}
      
	}
	
	public void endLog() throws IOException{
		closeLogFile();
	}
	
	
	/**
	 * Init a rescan of the log directory.
	 * This will make the new files appear in MTP.
	 * @param ctx
	 */
	public void rescanDir(Context ctx){
		this.rescanFiles(this.getLogDirectory(), ctx);
	}
	
	
	/**
	 * Close Log file.
	 * @param ctx
	 * @throws IOException
	 */
	public void close(Context ctx) throws IOException{
		closeLogFile(ctx);
	}

}
