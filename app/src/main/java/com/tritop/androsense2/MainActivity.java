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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.tritop.androsense2.fragments.GpsFragment;
import com.tritop.androsense2.fragments.GpsNmeaFragment;
import com.tritop.androsense2.fragments.SensorsListFragment;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends FragmentActivity implements androidx.viewpager.widget.ViewPager.OnPageChangeListener {
    public static final String TAG = "MainActivity#";
    public static final String PACKAGE_NAME = "com.tritop.androsense2";

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    private static Long timingCount;
    static Lock ground_truth_insert_locker = new ReentrantLock();
    static int waitVal = 1000;
    Map<String, String> configMap = new HashMap<>();
    static final String CONFIG_FILE_PATH = "/data/local/tmp/config.out";
    public static Map<String, Integer> methodIdMap = new HashMap<>();

    public static CacheScan cs = null;

    public static int fd = -2;
    private Messenger mService;

    private Messenger replyMessenger = new Messenger(new MessengerHandler());
    public static ArrayList<SideChannelValue> sideChannelValues = new ArrayList<>();
    public static ArrayList<GroundTruthValue> groundTruthValues = new ArrayList<>();
    public static final List<MethodStat> methodStats = new ArrayList<>();

    private static Context mContext;

    public static String sideChannelDPPath;
    public static String mainAppDPPath;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        Log.d(TAG, "Inside oncreate");

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED

        ) {

            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS
                            , Manifest.permission.WRITE_EXTERNAL_STORAGE
                            , Manifest.permission.CAMERA
                            , Manifest.permission.ACCESS_FINE_LOCATION
                            , Manifest.permission.ACCESS_COARSE_LOCATION},
                    10);
        } else {
            setUpandRun();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 10: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    setUpandRun();
                } else {
                    finish();
                }
            }
        }
    }

    protected void setUpandRun() {
        sideChannelDPPath = getDatabasePath("SideScan").toString();
        mainAppDPPath = getDatabasePath("MainApp").toString();
        fd = createAshMem();
        if (fd < 0) {
            Log.d("ashmem ", "not set onCreate " + fd);
        }

        copyOdex();

        configMap = readConfigFile();
//        configMap.entrySet().forEach(e -> Log.d("configMap: ", e.getKey() + " " + e.getValue()));


        initializeDB();
        initializeDBAop();
        Intent begin = new Intent(this, SideChannelJob.class);
        bindService(begin, conn, Context.BIND_AUTO_CREATE);
        startForegroundService(begin);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        setContentView(R.layout.activity_main);

        ((AndrosensApp) this.getApplication()).setSensorManager((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        ((AndrosensApp) getApplication()).setSensorList(((AndrosensApp) getApplication()).getSensorManager().getSensorList(Sensor.TYPE_ALL));

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.viewPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(this);

    }

    private static class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d("ashmem", "Received information from the server: " + msg.getData().getString("reply"));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            Message msg = Message.obtain(null, 0);
            Bundle bundle = new Bundle();
            if (fd < 0) {
                Log.d("ashmem ", "not set onServiceConnected " + fd);
            }
            setAshMemVal(fd, 4l);
            try {
                ParcelFileDescriptor desc = ParcelFileDescriptor.fromFd(fd);
                bundle.putParcelable("msg", desc);
                msg.setData(bundle);
                msg.replyTo = replyMessenger;      // 2
                mService.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    };

    private Map<String, String> readConfigFile() {
        Map<String, String> configMap = new HashMap<>();
        try {
            List<String> configs = Files.lines(Paths.get(CONFIG_FILE_PATH)).collect(Collectors.toList());
            configs.stream().filter(c -> !c.contains("//") && c.contains(":")).forEach(c -> configMap.put(c.split(":")[0].trim(), c.split(":")[1].trim()));

        } catch (IOException e) {
            Log.d(TAG + "#", e.toString());
        }
        return configMap;
    }

    private void copyOdex() {
        try {

            String oatHome = "/sdcard/Documents/oatFolder/oat/arm64/";
            Optional<String> baseOdexLine = Files.lines(Paths.get("/proc/self/maps")).collect(Collectors.toList())
                    .stream().sequential().filter(s -> s.contains(PACKAGE_NAME) && s.contains("base.odex"))
                    .findAny();
            Log.d("odex", Files.lines(Paths.get("/proc/self/maps")).collect(Collectors.joining("\n")));
            if (baseOdexLine.isPresent()) {
                String odexpath = "/data/app/" + baseOdexLine.get().split("/data/app/")[1];
                String vdexpath = "/data/app/" + baseOdexLine.get().split("/data/app/")[1].replace("odex", "vdex");
//                String odexRootPath = "/data/app/"+baseOdexLine.get().split("/data/app/")[1].replace("/oat/arm64/base.odex","*");
                Log.d(TAG + "#", odexpath);
                Log.d(TAG + "#", "cp " + odexpath + " " + oatHome);
                Process p = Runtime.getRuntime().exec("cp " + odexpath + " " + oatHome);
                p.waitFor();
                p = Runtime.getRuntime().exec("cp " + vdexpath + " " + oatHome);
                Log.d(TAG + "#", "cp " + vdexpath + " " + oatHome);

                p.waitFor();
                Log.d(TAG + "#", "odex copied");

            } else {
                Log.d(TAG + "#", "base odex absent");
            }

        } catch (IOException | InterruptedException e) {
            Log.d(TAG + "#", e.toString());
        }
    }

    public static void copyMethodMap() {
        String methodMapString = methodIdMap.entrySet().parallelStream().map(Object::toString).collect(Collectors.joining("|"));
//        log only allows a max of 4000 chars
        if (methodMapString.length() > 4000) {
            int chunkCount = methodMapString.length() / 4000;     // integer division

            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= methodMapString.length()) {
                    Log.d("MethodMap"+i, methodMapString.substring(4000 * i));
                } else {
                    Log.d("MethodMap"+i, methodMapString.substring(4000 * i, max));
                }
            }
        }
        Log.d("MethodCount", String.valueOf(methodIdMap.size()));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

//                    p.waitFor();
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent startIntent = new Intent(this, SettingsActivity.class);
                startActivity(startIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private SensorsListFragment sensfragment;
        private GpsFragment gpsfragment;
        private GpsNmeaFragment gpsNmeafragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    sensfragment = new SensorsListFragment();
                    return sensfragment;
                case 1:
                    gpsfragment = new GpsFragment();
                    return gpsfragment;
                case 2:
                    gpsNmeafragment = new GpsNmeaFragment();
                    return gpsNmeafragment;
                default:
                    return null;
            }

        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }


    }

    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }


    @Override
    public void onPageSelected(int arg0) {

    }

    /**
     * Method to initialize database
     */
    void initializeDB() {
        // Creating the database file in the app sandbox
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
                MODE_PRIVATE, null);
        Locale locale = new Locale("EN", "SG");
        db.setLocale(locale);
        // Creating the schema of the database
        String sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.GROUND_TRUTH + " (" +
                SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.LABEL + " TEXT, " +
                SideChannelContract.Columns.COUNT + " INTEGER);";
        db.execSQL(sSQL);
        sSQL = "DELETE FROM " + SideChannelContract.GROUND_TRUTH;
        db.execSQL(sSQL);
        db.close();
    }

    void initializeDBAop() {
        // Creating the database file in the app sandbox
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
                MODE_PRIVATE, null);
        Locale locale = new Locale("EN", "SG");
        db.setLocale(locale);
        // Creating the schema of the database
        String sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.GROUND_TRUTH_AOP + " (" +
                SideChannelContract.Columns.METHOD_ID + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.START_COUNT + " INTEGER, " +
                SideChannelContract.Columns.END_COUNT + " INTEGER);";
        db.execSQL(sSQL);
        sSQL = "DELETE FROM " + SideChannelContract.GROUND_TRUTH_AOP;
        db.execSQL(sSQL);
        Log.d("dbinfo", SideChannelContract.GROUND_TRUTH_AOP + " count: " + getRecordCount(SideChannelContract.GROUND_TRUTH_AOP));
        db.close();
    }

    public long getRecordCount(String tableName) {
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
                MODE_PRIVATE, null);
        long count = DatabaseUtils.queryNumEntries(db, tableName);
        db.close();
        return count;
    }

    public static native int setSharedMap();

    public native void setSharedMapChildTest(int shared_mem_ptr, char[] fileDes);

    public native int createAshMem();

    public static native long readAshMem(int fd);

    public static native void setAshMemVal(int fd, long val);


}
