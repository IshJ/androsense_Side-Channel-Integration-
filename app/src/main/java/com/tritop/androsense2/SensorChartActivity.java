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

import java.io.IOException;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.tritop.androsense2.helpers.SensorInfo;
import com.tritop.androsense2.log.SensorLog;
import com.tritop.androsense2.log.LogItem.LogCore;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SensorChartActivity extends Activity implements SensorEventListener, OnNavigationListener, OnClickListener {

	
	private final static int BAR_CHART = 0;
	private final static int BUBBLE_CHART = 1;
	private final static int COMBINED_CHART = 2;
	private final static int CUBELINE_CHART = 3;
	private final static int LINE_CHART = 4;
	private final static int RANGEBAR_CHART = 5;
	private final static int SCATTER_CHART = 6;	
	private final static int TIME_CHART = 7;
	
	
	private final static int VALUES_HARDLIMIT = 50;
	
	private BarChart.Type defaultBarChartType = BarChart.Type.DEFAULT;
	private float defaultCubeLineSmooth = .5f;
	
	private Sensor mSensor;
	private SensorManager mSensorManager;
	private TextView mInfo;
	private TextView mSensorAccuracy;
	private ToggleButton mLogToggle;
	private boolean isLogging;
	
	private SensorLog mSensorLog;
	
	private int mChartType = BAR_CHART;
	private XYMultipleSeriesRenderer mChartRenderer = new XYMultipleSeriesRenderer();
	private XYMultipleSeriesDataset mChartDataset = new XYMultipleSeriesDataset();
	private XYSeries xSeries,ySeries,zSeries;
	private XYSeriesRenderer mxRenderer,myRenderer,mzRenderer;
	private GraphicalView mChartView;
	
	private long yAx=0;
	
	private TextView mSensorValueX;
	private TextView mSensorValueY;
	private TextView mSensorValueZ;
	
	private LinearLayout llYvalues;
	private LinearLayout llZvalues;
	
	private ProgressBar mProgressX;
	private ProgressBar mProgressY;
	private ProgressBar mProgressZ;
	
	private float mConversionMulti=1.0f;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		int index=0;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensorchartactivity_layout);
		populateActionBarMenu();
		setupViews();
		isLogging = mLogToggle.isChecked();
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Intent intent = getIntent();
		if (intent != null) {
		    index = intent.getIntExtra("SENSORINDEX", 0);
		    
		}
		mSensor = ((AndrosensApp) this.getApplication()).getSensor(index);
		mInfo.setText(mSensor.getName());
		resetChart();
		mLogToggle.setOnClickListener(this);
		chooseProgressMaximum(mSensor.getMaximumRange());
	}


	
	
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		if(mLogToggle.isChecked()){
			
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(mLogToggle.isChecked()){
			
		}
		setChartType(mChartType);
		/*
		if (mChartView == null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout_chart1);
		    mChartView = getChartByType(mChartType);
		    layout.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		}
		else mChartView.repaint();
		*/
		mSensorManager.registerListener(this, mSensor, getSensorDelayPref());
		
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		switch(accuracy){
			case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:mSensorAccuracy.setText("ACCURACY HIGH");break;
			case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:mSensorAccuracy.setText("ACCURACY MEDIUM");break;
			case SensorManager.SENSOR_STATUS_ACCURACY_LOW:mSensorAccuracy.setText("ACCURACY LOW");break;
			case SensorManager.SENSOR_STATUS_UNRELIABLE:mSensorAccuracy.setText("STATUS UNRELIABLE");break;
			default:mSensorAccuracy.setText("unknow accuracy");break;
		}
		
	}

	
	

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(isLogging && mSensorLog != null){
			try {
				mSensorLog.logEvent(event.values);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(xSeries.getItemCount()>VALUES_HARDLIMIT){
			clearSeries(1);
		}
		if(SensorInfo.getSignificantValues(mSensor.getType()) >1){
		    xSeries.add(yAx,event.values[0]);
		    ySeries.add(yAx,event.values[1]);
		    zSeries.add(yAx,event.values[2]);
		}
		else {
			xSeries.add(yAx,event.values[0]);
			}
		mChartView.repaint();
		yAx++;
		
		drawProgressbar(event);
		
	}
	
	
	protected void drawProgressbar(SensorEvent event){
		
		if(SensorInfo.getSignificantValues(mSensor.getType()) >1){
			llYvalues.setVisibility(View.VISIBLE);
			llZvalues.setVisibility(View.VISIBLE);
			
			
			mSensorValueX.setText(SensorInfo.formatSensorfloat(event.values[0],3));
			mSensorValueY.setText(SensorInfo.formatSensorfloat(event.values[1],3));
			mSensorValueZ.setText(SensorInfo.formatSensorfloat(event.values[2],3));
			
			mProgressX.setProgress((int)(Math.abs(event.values[0])*mConversionMulti));
			mProgressY.setProgress((int)(Math.abs(event.values[1])*mConversionMulti));
			mProgressZ.setProgress((int)(Math.abs(event.values[2])*mConversionMulti));
		    
		}
		else {
			llYvalues.setVisibility(View.GONE);
			llZvalues.setVisibility(View.GONE);
			mSensorValueX.setText(SensorInfo.formatSensorfloat(event.values[0],3));
			mProgressX.setProgress((int)(Math.abs(event.values[0])*mConversionMulti));
		}
		
	}
	 
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		switch(itemPosition){
			case 0: setChartType(BAR_CHART);return true;
			//case 1:	return false;//setChartType(BUBBLE_CHART);return true;
			case 1: setChartType(CUBELINE_CHART);return true;
			case 2:	setChartType(LINE_CHART);return true;
			case 3: setChartType(RANGEBAR_CHART);return true;
			case 4:	setChartType(SCATTER_CHART);return true;
			default: setChartType(LINE_CHART);return false;
		}
	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if(mLogToggle.isChecked()){	
			LogCore core = savedInstanceState.getParcelable("logcore");
			try {
				mSensorLog = new SensorLog(mSensor.getName());
				mSensorLog.restoreCore(core);
				isLogging = true;
			} catch (IOException e) {
				e.printStackTrace();
				mLogToggle.setChecked(false);
				isLogging = false;
			}
		}
		xSeries = (XYSeries) savedInstanceState.getSerializable("xseries");
		ySeries = (XYSeries) savedInstanceState.getSerializable("yseries");
		zSeries = (XYSeries) savedInstanceState.getSerializable("zseries");
		mxRenderer = (XYSeriesRenderer) savedInstanceState.getSerializable("xrenderer");
		myRenderer = (XYSeriesRenderer) savedInstanceState.getSerializable("yrenderer");
		mzRenderer = (XYSeriesRenderer) savedInstanceState.getSerializable("zrenderer");
		mChartDataset = (XYMultipleSeriesDataset) savedInstanceState.getSerializable("dataset");
		mChartRenderer = (XYMultipleSeriesRenderer) savedInstanceState.getSerializable("renderer");
		yAx = savedInstanceState.getLong("yAx");
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mLogToggle.isChecked() && (mSensorLog != null)){
			outState.putParcelable("logcore", mSensorLog.dumpCore());
			isLogging = true;
		}
		outState.putSerializable("dataset", mChartDataset);
	    outState.putSerializable("renderer", mChartRenderer);
	    outState.putSerializable("xseries", xSeries);
	    outState.putSerializable("yseries", ySeries);
	    outState.putSerializable("zseries", zSeries);
	    outState.putSerializable("xrenderer", mxRenderer);
	    outState.putSerializable("yrenderer", myRenderer);
	    outState.putSerializable("zrenderer", mzRenderer);
	    outState.putLong("yAx", yAx);
	}
	
	private void setupViews(){
		
		mInfo = (TextView) findViewById(R.id.textView_sensorchartinfo);
		mSensorAccuracy = (TextView) findViewById(R.id.textView_sensoraccuracy);
		mLogToggle = (ToggleButton) findViewById(R.id.sensorchartactivity__toggleButton_log);
		
		llYvalues = (LinearLayout) this.findViewById(R.id.activity_sensor_LL_Y);
		llZvalues = (LinearLayout) this.findViewById(R.id.activity_sensor_LL_Z);
		
		mProgressX  =  (ProgressBar) this.findViewById(R.id.activity_sensor_progressBarX);
		mProgressY =  (ProgressBar) this.findViewById(R.id.activity_sensor_progressBarY);
	    mProgressZ =  (ProgressBar) this.findViewById(R.id.activity_sensor_progressBarZ);
	    
	    mProgressX.setIndeterminate(false);
	    mProgressY.setIndeterminate(false);
	    mProgressZ.setIndeterminate(false);
	    mSensorValueX = (TextView) this.findViewById(R.id.activity_sensor_textView_xvalue);
		mSensorValueY = (TextView) this.findViewById(R.id.activity_sensor_textView_yvalue);
		mSensorValueZ = (TextView) this.findViewById(R.id.activity_sensor_textView_zvalue);
	}
	
	private void setChartType(int type){
		mChartType=type;
		if (mChartView == null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout_chart1);
		    mChartView = getChartByType(mChartType);
		    layout.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		}
		else {
			LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout_chart1);
			if(layout.getChildCount()>0)
				layout.removeAllViews();
		    mChartView = getChartByType(mChartType);
		    layout.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
			mChartView.repaint();
		}
	}
	
	private GraphicalView getChartByType(int type){
		switch(type){
			case BAR_CHART: return ChartFactory.getBarChartView(this, mChartDataset, mChartRenderer,defaultBarChartType);
			case BUBBLE_CHART: return ChartFactory.getBubbleChartView(this, mChartDataset, mChartRenderer);
			//case COMBINED_CHART: ChartFactory.getCombinedXYChartView(this, mChartDataset, mChartRenderer);
			case CUBELINE_CHART: return ChartFactory.getCubeLineChartView(this, mChartDataset, mChartRenderer,defaultCubeLineSmooth);
			case LINE_CHART: return ChartFactory.getLineChartView(this, mChartDataset, mChartRenderer);
			case RANGEBAR_CHART: return ChartFactory.getRangeBarChartView(this, mChartDataset, mChartRenderer,defaultBarChartType);
			case SCATTER_CHART: return ChartFactory.getScatterChartView(this, mChartDataset, mChartRenderer);
			//case TIME_CHART: ChartFactory.getTimeChartView(this, mChartDataset, mChartRenderer);
			default: return ChartFactory.getLineChartView(this, mChartDataset, mChartRenderer);
		}
	}
	
	private void resetChart(){
		mChartRenderer.removeAllRenderers();
		mChartDataset.clear();
		
		xSeries = new XYSeries("X");
		ySeries = new XYSeries("Y");
		zSeries = new XYSeries("Z");
		mxRenderer = new XYSeriesRenderer();
		myRenderer = new XYSeriesRenderer();
		mzRenderer = new XYSeriesRenderer();
		mxRenderer.setPointStyle(PointStyle.CIRCLE);
		myRenderer.setPointStyle(PointStyle.DIAMOND);
		myRenderer.setColor(Color.RED);
		mzRenderer.setPointStyle(PointStyle.SQUARE);
		mzRenderer.setColor(Color.GREEN);
		
		mChartRenderer.setMargins(new int[] { 20, 50, 20, 20 });
		mChartRenderer.setLabelsTextSize(20);
		mChartRenderer.setLabelsColor(Color.BLACK);
		mChartRenderer.setLegendTextSize(20);
		mChartRenderer.setAxesColor(Color.BLACK);
		mChartRenderer.setXLabelsColor(Color.BLACK);
		mChartRenderer.setYLabelsAlign(Align.RIGHT);
		mChartRenderer.setYLabelsColor(0,Color.BLACK);
		mChartRenderer.setMarginsColor(Color.WHITE);
		mChartDataset.addSeries(xSeries);
		mChartRenderer.addSeriesRenderer(mxRenderer);
		if(SensorInfo.getSignificantValues(mSensor.getType()) >1){
			mChartDataset.addSeries(ySeries);
			mChartRenderer.addSeriesRenderer(myRenderer);
			mChartDataset.addSeries(zSeries);
			mChartRenderer.addSeriesRenderer(mzRenderer);
		}
		if(SensorInfo.getSignificantValues(mSensor.getType())>1)
			mChartRenderer.setYAxisMax(mSensor.getMaximumRange());
		mChartRenderer.setShowGridX(true);
		mChartRenderer.setPanEnabled(false);
		mChartRenderer.setZoomEnabled(false);
	}
	
	private void populateActionBarMenu(){
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(actionBar.getDisplayOptions() ^ ActionBar.DISPLAY_SHOW_TITLE);
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		final String[] dropdownValues = getResources().getStringArray(R.array.sensorchartactivity_actionbaritem);
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
	       android.R.layout.simple_spinner_item, android.R.id.text1,
	        dropdownValues);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    actionBar.setListNavigationCallbacks(adapter, this);
	}


	private void clearSeries(int clearCount){
		if(xSeries.getItemCount() > clearCount){
			for(int i=0;i<clearCount;i++){
				xSeries.remove(0);
			}
		}
		if(ySeries.getItemCount() > clearCount){
			for(int i=0;i<clearCount;i++)
				ySeries.remove(0);
		}
		if(zSeries.getItemCount() > clearCount){
			for(int i=0;i<clearCount;i++)
				zSeries.remove(0);
		}
	}
	
	private void chooseProgressMaximum(float sensorMax){
		if(sensorMax > 100){
			setProgressBarMax((int) sensorMax);
		}
		else {
			mConversionMulti = 100 / sensorMax;
			setProgressBarMax((int) (sensorMax*mConversionMulti));
		}
	}
	
	private void setProgressBarMax(int maximum){
		mProgressX.setMax(maximum);
		mProgressY.setMax(maximum);
		mProgressZ.setMax(maximum);
	}
	
	private String readDirectoryPreference(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		return settings.getString("logfiledirectory", "Androsens");
	}
	
	private void startLog(){
		try {
			mSensorLog = new SensorLog(mSensor.getName(), readDirectoryPreference());
		} catch (IOException e) {
			e.printStackTrace();
			mLogToggle.setChecked(false);
			isLogging = false;
		}
	}

	private void stopLog(){
		if(mSensorLog != null){
			try {
				mSensorLog.close(this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void toggleLog(boolean log){
		if(log){
			startLog();
		}
		else stopLog();
	}

	
	private int getSensorDelayPref(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    int delay = Integer.valueOf(settings.getString("sensorDelay", "2"));
	    switch(delay){
	    	case SensorManager.SENSOR_DELAY_FASTEST:return SensorManager.SENSOR_DELAY_FASTEST; 
	    	case SensorManager.SENSOR_DELAY_GAME:   return SensorManager.SENSOR_DELAY_GAME; 
	    	case SensorManager.SENSOR_DELAY_UI:     return SensorManager.SENSOR_DELAY_UI; 
	    	case SensorManager.SENSOR_DELAY_NORMAL: return SensorManager.SENSOR_DELAY_NORMAL; 
	    	default: return SensorManager.SENSOR_DELAY_NORMAL;
	    }
	}
	
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.sensorchartactivity__toggleButton_log:isLogging = mLogToggle.isChecked();toggleLog(isLogging);break;
			default:break;
		}
	}

}
