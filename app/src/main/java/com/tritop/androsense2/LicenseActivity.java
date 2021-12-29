package com.tritop.androsense2;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class LicenseActivity extends Activity {

	String mLicensepath;
	TextView mLicenseTextView;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_license);
		mLicenseTextView = (TextView) this.findViewById(R.id.license_activity_license_textview);
		mLicensepath = this.getString(R.string.license_activity_gpl_assets_name);
		loadLicense();
	}
	
	
	
	public void loadLicense(){
		if((mLicenseTextView) != null && (mLicensepath !=null)){
			try {
	            InputStream is = getAssets().open(mLicensepath);
	            int size = is.available();
	            byte[] buffer = new byte[size];
	            is.read(buffer);
	            is.close();
	            String license = new String(buffer);
	            mLicenseTextView.setText(license);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			
		}
	}
	
}
