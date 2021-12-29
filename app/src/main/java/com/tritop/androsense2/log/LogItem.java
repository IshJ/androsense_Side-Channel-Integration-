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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Base class for all logtypes.
 * @author Snyder
 *
 */
public abstract class LogItem {
	
	private File mLogFile;
	private File mLocation;
	private FileWriter mFileWriter;
	private LogCore mCore;
	
	
	/**
	 * Contains all data necessary to recreate a logitem from a parcel.
	 * @author Snyder
	 *
	 */
	public class LogCore implements Parcelable{

		protected boolean mLogTimestamp;
		protected boolean mLogId;
		protected String mDelimiter=";";
		protected String mType;
		protected String mTimeFormatFilename="dd-MM-yyyy_HH-mm-ss";
		protected String mTimeFormatLog="dd-MM-yyyy_HH:mm:ss";
		protected long mId=0;
		
		private String mLogFilePath;
		private String mLocationPath;
		
		public LogCore(){
			
		}
		
		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			
			dest.writeInt((mLogTimestamp) ? 1:0);
			dest.writeInt((mLogId) ? 1:0);
			dest.writeString(mDelimiter);
			dest.writeString(mType);
			dest.writeString(mTimeFormatFilename);
			dest.writeString(mTimeFormatLog);
			dest.writeLong(mId);
			dest.writeString(mLogFilePath);
			dest.writeString(mLocationPath);
			
		}
		
		public final Parcelable.Creator<LogCore> CREATOR = new Parcelable.Creator<LogItem.LogCore>() {

			@Override
			public LogCore createFromParcel(Parcel source) {
				return new LogCore(source);
			}

			@Override
			public LogCore[] newArray(int size) {
				return new LogCore[size];
			}
			
		};
		
		private LogCore(Parcel in){
			this.mLogTimestamp = (in.readInt()==1)?true:false;
			this.mLogId = (in.readInt()==1)?true:false;
			this.mDelimiter = in.readString();
			this.mType = in.readString();
			this.mTimeFormatFilename = in.readString();
			this.mTimeFormatLog = in.readString();
			this.mId = in.readLong();
			this.mLogFilePath = in.readString();
			this.mLocationPath = in.readString();
		}
		
		protected void dcopy(LogCore core){
			this.mLogTimestamp = core.mLogTimestamp;
			this.mLogId = core.mLogId;
			this.mDelimiter = core.mDelimiter;
			this.mType = core.mType;
			this.mTimeFormatFilename = core.mTimeFormatFilename;
			this.mTimeFormatLog = core.mTimeFormatLog;
			this.mId = core.mId;
			this.mLogFilePath = core.mLogFilePath;
			this.mLocationPath = core.mLocationPath;
		}
	}

	/**
	 * Constructor usefull when recreating a Logitem from a dumped core
	 * @param type
	 * @throws IOException
	 */
	LogItem(String type) throws IOException {
		this.mCore = new LogCore();
		this.mCore.mType = type;
		this.mCore.mLogTimestamp = true;
		this.mCore.mLogId = true;
		
	}
	
	
	/**
	 * Constructor, creates a new logitem of a given type inside the directory
	 * @param type
	 * @param directoryName
	 * @throws IOException
	 */
	LogItem(String type,String directoryName) throws IOException {
		this.mCore = new LogCore();
		this.mCore.mType = type;
		this.mCore.mLogTimestamp = true;
		this.mCore.mLogId = true;
		if(setLogDirectory(directoryName)){
			createLogFile(mLocation);
			openLogFile();
		} else {
			throw new IOException("No access to Log Directory");
		}
	}
	

	/**
	 * Restores the core of a Logitem. 
	 * @param core
	 * @throws IOException
	 */
	public void restoreCore(LogCore core) throws IOException{
		this.mCore = core;
		this.mLogFile = new File(this.mCore.mLogFilePath);
		this.mLocation = new File(this.mCore.mLocationPath);
		this.mFileWriter = new FileWriter(mLogFile, true);
		addResumeNotice();
	}
	
	/**
	 * Restores the core of a Logitem from a parcel
	 * @param in
	 * @throws IOException
	 */
	public void restoreCore(Parcel in) throws IOException{
		this.mCore = new LogCore(in);
		this.mLogFile = new File(this.mCore.mLogFilePath);
		this.mLocation = new File(this.mCore.mLocationPath);
		this.mFileWriter = new FileWriter(mLogFile, true);
		addResumeNotice();
	}
	
	
	/**
	 * Dump the core so we can send it in a parcel.
	 * @return
	 */
	public LogCore dumpCore(){
		this.mCore.mLocationPath = this.mLocation.getAbsolutePath();
		this.mCore.mLogFilePath = this.mLogFile.getAbsolutePath();
		return this.mCore;
	}
	
	/**
	 * Sets the delimiter for CSV logfiles
	 * @param delim
	 */
	public void setDelimiter(String delim){
		this.mCore.mDelimiter = delim;
	}
	
	public String getDelimiter(){
		return this.mCore.mDelimiter;
	}
	
	public File getLogDirectory(){
		return this.mLocation;
	}
	
	protected void setLogFile(File logFile){
		this.mLogFile = logFile;
	}
	
	protected File getLogFile(){
		return this.mLogFile;
	}
	
	
	protected boolean ismLogTimestamp() {
		return this.mCore.mLogTimestamp;
	}

	protected void setmLogTimestamp(boolean mLogTimestamp) {
		this.mCore.mLogTimestamp = mLogTimestamp;
	}

	protected boolean ismLogId() {
		return this.mCore.mLogId;
	}

	protected void setmLogId(boolean logId) {
		this.mCore.mLogId = logId;
	}
	
	protected String getmTimeFormatLog() {
		return this.mCore.mTimeFormatLog;
	}

	protected void setmTimeFormatLog(String mTimeFormatLog) {
		this.mCore.mTimeFormatLog = mTimeFormatLog;
	}
	
	protected long getmId() {
		return this.mCore.mId;
	}

	protected void setmId(long mId) {
		this.mCore.mId = mId;
	}
	
	protected void openLogFile() throws IOException{
	  if(this.mLogFile != null) {
		  this.mFileWriter = new FileWriter(this.mLogFile);
	  }
	}
	
	
	/**
	 * Close the logfile
	 * @throws IOException
	 */
	protected void closeLogFile() throws IOException{
		if(this.mFileWriter != null) {
			this.mFileWriter.flush();
			this.mFileWriter.close();
		}
	}
	
	
	/**
	 * Closes logfile and starts a rescan of the logdirectory by
	 * the MTP service.
	 * @param ctx
	 * @throws IOException
	 */
	protected void closeLogFile(Context ctx) throws IOException{
		if(this.mFileWriter != null){
			this.mFileWriter.flush();
			this.mFileWriter.close();
			rescanFiles(this.mLocation, ctx);
		}
	}
	
	protected void log(LinkedList<String> values) throws IOException{
		if(this.mCore.mLogTimestamp){
			if(this.mCore.mLogId){
				csvWriter(addTimeStamp(addId(values)));
			}
			else {
				csvWriter(addTimeStamp(values));
			}
			
		}
		else {
			if(this.mCore.mLogId){
				csvWriter(addId(values));
			}
			else {
				csvWriter(values);
			}
		}
	}
	
	
	/**
	 * Starts a rescan of the specified location.
	 * This will make generated logfiles visible to the MTP
	 * service.
	 * @param scanLocation	File or Directory to rescan
	 * @param ctx	Context needed to access MediaScanner
	 */
	protected void rescanFiles(File scanLocation,Context ctx){
		String[] filePaths;
		if(scanLocation.isDirectory()){
			File[] filesToScan = scanLocation.listFiles();
			filePaths = new String[filesToScan.length];
			int i=0;
			for(File scanF: filesToScan){
				filePaths[i++] = scanF.getAbsolutePath();
			}
		}
		else {
			filePaths = new String[]{scanLocation.getAbsolutePath()};
		}
		MediaScannerConnection.scanFile(ctx,
				filePaths, null,
		          new MediaScannerConnection.OnScanCompletedListener() {

			@Override
			public void onScanCompleted(String path, Uri uri) {
				
			}
		 });
	}
	
	
	/**
	 * Adds a unique increasing Id to the front of the values list
	 * @param values
	 * @return values list with prepended id
	 */
	private LinkedList<String> addId(LinkedList<String> values){
		values.addFirst(String.valueOf(mCore.mId++));
		return values;
	}
	
	/**
	 * Set the directory where the logfiles are created. The directory will be created
	 * in the external storage directory.
	 * @param directoryname
	 * @return
	 */
	private boolean setLogDirectory(String directoryname){
		Log.e("STATE","state "+Environment.getExternalStorageState());
		Log.e("dir","dir "+Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+directoryname);
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File location = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+directoryname);
			if(location.exists() && location.isDirectory()){
				this.mLocation = location;
				setLogFileAttributes(this.mLocation);
				return true;
			}
			else {
				this.mLocation = location;
				this.mLocation.mkdir();
				setLogFileAttributes(this.mLocation);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Creates the logfile in the specified directory
	 * @param directory
	 * @throws IOException
	 */
	private void createLogFile(File directory) throws IOException{
		Log.e("CREATE ","dir"+directory.getAbsolutePath()+"File:"+File.separator + this.getFilename());
		this.mLogFile = new File(directory, File.separator + this.getFilename());
		this.mLogFile.createNewFile();
		setLogFileAttributes(this.mLogFile);
	}
	
	
	/**
	 * Set logfile world read and writeable
	 * @param logFile
	 */
	private void setLogFileAttributes(File logFile){
		logFile.setReadable(true, false);
		logFile.setWritable(true, false);
	}
	
	
	/**
	 * Add a timestamp to the front of the value list.
	 * @param values
	 * @return
	 */
	private LinkedList<String> addTimeStamp(LinkedList<String> values){
		values.addFirst(String.valueOf(System.currentTimeMillis()));
		return values;
	}
	
	
    private void addResumeNotice() throws IOException{
    	csvWriterNote("Resuming");
    }
    
    
    private void csvWriterNote(String note) throws IOException{
    	if(this.mFileWriter != null){
    		this.mFileWriter.write(note);
    		this.mFileWriter.write("\n");
			this.mFileWriter.flush();
    	}
    }
    
    private void xmlWriterNote(String note){
		
	}
    
	private void csvWriter(LinkedList<String> values) throws IOException{
		if(this.mFileWriter != null){
			int column = 0;
			for(String value:values){
				if(column != 0)
					this.mFileWriter.write(this.mCore.mDelimiter);
				this.mFileWriter.write(value);
				column++;
			}
			this.mFileWriter.write("\n");
			this.mFileWriter.flush();
		}
	}
	
	private void xmlWriter(LinkedList<String> values){
		
	}
	
	/**
	 * Creates a filename for the logfile based on type and current time
	 * @return
	 */
	private String getFilename(){
		Calendar cal = Calendar.getInstance();
    	SimpleDateFormat sdf = new SimpleDateFormat(mCore.mTimeFormatFilename);
    	return sdf.format(cal.getTime()) + this.mCore.mType+".txt";
	}
}
