package com.tritop.androsense2.fragments;

import java.io.IOException;
import java.util.ArrayList;

import com.tritop.androsense2.R;
import com.tritop.androsense2.log.GpsNMEALog;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsStatus.NmeaListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;

import static android.app.PendingIntent.getActivity;

public class GpsNmeaFragment extends Fragment implements NmeaListener, OnClickListener {
	
	public static final String SENSORTYPE = "GPS_NMEA";
	
	private ArrayList<String> mNmeaData = new ArrayList<String>();
	private ArrayAdapter mAAdapter;
	private ListView mNmeaListView;
	private LocationManager mLocationManager;
	
	private ImageButton mClearButton;
	private ToggleButton mLogToggle;
	private ToggleButton mActiveButton;
	
	private GpsNMEALog mGpsNMEALog;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		View view = inflater.inflate(R.layout.gpsnmeafragment_layout, container,false);
		setupClickListeners(view);
		return view;
	}

	public void setupClickListeners(View view){
		mClearButton = (ImageButton) view.findViewById(R.id.gpsnmeafragment_btn_clear);
		mLogToggle = (ToggleButton) view.findViewById(R.id.gpsnmeafragment_tb_log);
		mActiveButton = (ToggleButton) view.findViewById(R.id.gpsnmeafragment_tb_active);
		mClearButton.setOnClickListener(this);
		mLogToggle.setOnClickListener(this);
		mActiveButton.setOnClickListener(this);
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		mAAdapter = new  ArrayAdapter(getActivity(),R.layout.gpsnmea_list_item ,R.id.gpsnmea_listitem_textView);
		mNmeaListView = (ListView) this.getActivity().findViewById(R.id.gpsnmea_listView);
		mNmeaListView.setAdapter(mAAdapter);
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onPause() {
		stopListening();
		super.onPause();
	}

	
	@Override
	public void onResume() {
		super.onResume();
		if(mActiveButton.isChecked()){
			startListening();
		}
	}

	private void stopListening(){
		mLocationManager.removeNmeaListener(this);
	}
	
	private void startListening(){
		mLocationManager.addNmeaListener(this);
	}
	
	@Override
	public void onNmeaReceived(long timestamp, String nmea) {
		//mNmeaData.add(nmea);
		if(mLogToggle.isChecked() && mGpsNMEALog != null){
			try {
				mGpsNMEALog.logEvent(nmea);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mAAdapter.add(nmea);
		mAAdapter.notifyDataSetChanged();
		mNmeaListView.setSelection(mAAdapter.getCount() - 1);
	}

	private void toggleActive(boolean active){
		if(active){
			startListening();
		}
		else {
			stopListening();
		}
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
			mGpsNMEALog = new GpsNMEALog(SENSORTYPE, readDirectoryPreference());
		} catch (IOException e) {
			e.printStackTrace();
			mLogToggle.setChecked(false);
			//isLogging = false;
		}
	}
	
	private void stopLog(){
		if(mGpsNMEALog != null){
			try {
				mGpsNMEALog.close(getActivity());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void clearList(){
		mAAdapter.clear();
		mAAdapter.notifyDataSetChanged();
	}

	private String readDirectoryPreference(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		return settings.getString("logfiledirectory", "Androsens");
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.gpsnmeafragment_btn_clear:clearList();break;
			case R.id.gpsnmeafragment_tb_log:toggleLog(mLogToggle.isChecked());break;
			case R.id.gpsnmeafragment_tb_active:toggleActive(mActiveButton.isChecked());break;
		}
		
	}
	
	
}
