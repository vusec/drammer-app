package org.iseclab.drammer.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.android.device.DeviceName;

import org.iseclab.drammer.Constants;
import org.iseclab.drammer.Preferences;
import org.iseclab.drammer.R;
import org.iseclab.drammer.code.AdvertisementID;
import org.iseclab.drammer.code.DeviceStatistics;
import org.iseclab.drammer.code.DrammerService;
import org.iseclab.drammer.code.ProcessKiller;
import org.iseclab.drammer.code.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback  {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static BroadcastReceiver mReceiver;

    private Button mStartButton;
    private Button mStopButton;
    private TextView mResultView;
    private ScrollView mScrollView;
    private ProgressDialog mProgressDialog;
    private CheckBox mUploadCheck;
    private CheckBox mScrollCheck;

    private SeekBar mSeekBar;
    private ActionBar mActionBar;

    private String mDrammerBinary;
    private String mDrammerOutput;
    private boolean mHammerReady;

    private SharedPreferences mPreferences;

    private String mError = null;

    private boolean isVulnerable = false;

    private boolean autoScrollResult = true;

    private String IMEI = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = this.getSupportActionBar();
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setCustomView(R.layout.action_bar_title);
        TextView title = ((TextView) findViewById(R.id.action_bar_title));
        title.setText(getResources().getString(R.string.app_name_ascii));
        //this.getSupportActionBar().hide();

        mResultView = (TextView) findViewById(R.id.text_result);
        mResultView.setVisibility(View.VISIBLE);
        mResultView.setMovementMethod(new ScrollingMovementMethod());
        mScrollView = (ScrollView) findViewById(R.id.view_result);

        mStartButton = (Button) findViewById(R.id.button_start);
        mStartButton.setOnClickListener(this);
        mStartButton.setEnabled(false);

        mStopButton = (Button) findViewById(R.id.button_stop);
        mStopButton.setOnClickListener(this);
        mStopButton.setEnabled(false);

        mPreferences = getSharedPreferences(Preferences.DRAMMER_PREFS, Activity.MODE_PRIVATE);

        mDrammerOutput = Utils.getAbsolutePath(getApplicationContext(), Preferences.DRAMMER_LOG);

        mHammerReady = prepareHammer();

        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(Constants.PREFS.BINARY, mDrammerBinary);
        editor.putString(Constants.PREFS.OUTPUT, mDrammerOutput);
        editor.putBoolean(Constants.PREFS.VULNERABLE, mPreferences.getBoolean(Constants.PREFS.VULNERABLE, false));
        editor.putBoolean(Constants.PREFS.UPLOAD, mPreferences.getBoolean(Constants.PREFS.UPLOAD, true));
        editor.commit();

        identifyDevice();

        isVulnerable = mPreferences.getBoolean(Constants.PREFS.VULNERABLE, false);
        setIsVulnerable(isVulnerable);

        onFirstRun();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuhelp) {
            startActivity(new Intent(MainActivity.this, InfoActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();

        mUploadCheck = ((CheckBox)findViewById(R.id.upload_checkbox));
        mUploadCheck.setChecked(mPreferences.getBoolean(Constants.PREFS.UPLOAD, true));
        mUploadCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(Constants.PREFS.UPLOAD,isChecked);
                editor.commit();
            }
        });

        mScrollCheck = ((CheckBox)findViewById(R.id.scroll_checkbox));
        mScrollCheck.setChecked(autoScrollResult);
        mScrollCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                autoScrollResult = isChecked;
            }
        });

        mSeekBar = (SeekBar) findViewById(R.id.slider);
        mSeekBar.setMax(Preferences.DRAMMER_DEFRAG_MAX);
        mSeekBar.setProgress(mPreferences.getInt(Constants.PREFS.DEFRAGTIMEOUT, Preferences.DRAMMER_DEFRAG_TIMEOUT));
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt(Constants.PREFS.DEFRAGTIMEOUT, progress);
                editor.commit();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        isVulnerable = mPreferences.getBoolean(Constants.PREFS.VULNERABLE, false);
        setIsVulnerable(isVulnerable);

        mReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                Integer status = intent.getIntExtra(Constants.EXTRAS.STATUS, -1);
                switch (status) {
                    case Constants.STATUS.STARTED:
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.text_status_running), Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.STATUS.FINISHED:
                        mProgressDialog = new ProgressDialog(MainActivity.this);
                        mProgressDialog.setMessage(getResources().getString(R.string.text_upload));
                        mProgressDialog.setIndeterminate(true);
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setCanceledOnTouchOutside(true);
                        mProgressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getString(R.string.text_upload_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        mProgressDialog.show();

                        String result = intent.getStringExtra(Constants.EXTRAS.MESSAGE);
                        if((result != null)){
                            mResultView.setText(result);
                            if(autoScrollResult){
                                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        }
                        setIsVulnerable(isVulnerable);

                        break;
                    case Constants.STATUS.UPLOAD_COMPLETED:
                        resetHammering();
                        if((mProgressDialog != null) && mProgressDialog.isShowing()){
                            mProgressDialog.dismiss();
                        }
                        if(mError == null){
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.text_status_finished), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Constants.STATUS.UPDATE:
                        String update = intent.getStringExtra(Constants.EXTRAS.MESSAGE);
                        if(update != null){
                            mResultView.append(update);
                            if(autoScrollResult){
                                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                            if(!(isVulnerable) && update.contains(Constants.BITFLIP)){
                                isVulnerable = true;
                                setIsVulnerable(isVulnerable);
                            }

                        }
                        break;
                    case Constants.STATUS.BATCH_UPDATE:
                        String batchUpdate = intent.getStringExtra(Constants.EXTRAS.MESSAGE);
                        if((batchUpdate != null) && !batchUpdate.isEmpty()){
                            mResultView.setText(batchUpdate);
                            if(autoScrollResult){
                                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        }
                        break;
                    case Constants.STATUS.ERROR:
                        resetHammering();
                        mError = intent.getStringExtra(Constants.EXTRAS.MESSAGE);
                        if(mError != null) {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.text_status_error) + ":" + mError, Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(Constants.BROADCAST.DRAMMER_NOTIFICATION));

        boolean serviceRunning = isServiceRunning();
        boolean runningPrefFlag = mPreferences.getBoolean(Constants.PREFS.ISRUNNING, false);
        boolean hammerRunning = new ProcessKiller().isRunning(Utils.getBasename(mDrammerBinary));

        Log.d(TAG, "onResume: serviceRunning=" + serviceRunning + " runningPrefFlag=" + runningPrefFlag + " hammerRunning=" + hammerRunning);

        if(hammerRunning){
            mStopButton.setEnabled(true);
            mStartButton.setEnabled(false);
        } else if (mHammerReady){
            mStopButton.setEnabled(false);
            mStartButton.setEnabled(true);
        } else {
            mStopButton.setEnabled(false);
            mStartButton.setEnabled(false);
            return;
        }

        Intent resumeIntent = new Intent(MainActivity.this, DrammerService.class);
        resumeIntent.setAction(Constants.ACTION.ACTIVITY_RESUME);
        startService(resumeIntent);

        isVulnerable = mPreferences.getBoolean(Constants.PREFS.VULNERABLE, false);

        mResultView.setText(mPreferences.getString(Constants.PREFS.RESULT, ""));
        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    @Override
    public void onPause(){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(Constants.PREFS.RESULT, mResultView.getText().toString());
        editor.commit();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

        Intent pauseIntent = new Intent(MainActivity.this, DrammerService.class);
        pauseIntent.setAction(Constants.ACTION.ACTIVITY_PAUSE);
        startService(pauseIntent);
        super.onPause();
    }

    @Override
    public void onStart(){
        super.onStart();

        mResultView.setText(mPreferences.getString(Constants.PREFS.RESULT, ""));
        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);

        if(Preferences.RUN_AUTOMATICALLY){
            Log.e(TAG, "Starting hammering automatically");
            startHammering();
        }
    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onDestroy(){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(Constants.PREFS.RESULT, mResultView.getText().toString());
        editor.commit();

        boolean serviceRunning = isServiceRunning();
        boolean hammerRunning = new ProcessKiller().isRunning(Utils.getBasename(mDrammerBinary));

        if(serviceRunning && !hammerRunning) {
            resetHammering();
        }

        super.onDestroy();
    }

    private void onFirstRun(){
        if(!Preferences.SHOW_INFO_SCREEN) return;

        if(mPreferences.getBoolean(Constants.PREFS.FIRST_RUN, true)){
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
            alertBuilder.setMessage(R.string.text_info_short);
            alertBuilder.setTitle(R.string.text_info_title);
            alertBuilder.setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            final SharedPreferences.Editor editor = mPreferences.edit();
                            editor.putBoolean(Constants.PREFS.FIRST_RUN, false);
                            editor.commit();
                        }
                    });
            AlertDialog alert = alertBuilder.create();
            alert.show();
        }

    }


    public void identifyDevice(){
        final SharedPreferences.Editor editor = mPreferences.edit();

        String installUUID = mPreferences.getString(Constants.PREFS.INSTAlL_UUID, null);
        if(installUUID == null){
            installUUID = UUID.randomUUID().toString();
            editor.putString(Constants.PREFS.INSTAlL_UUID, installUUID);
            editor.commit();
        }

        String androidID = mPreferences.getString(Constants.PREFS.ANDROID_ID, null);
        if(androidID == null) {
            androidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            editor.putString(Constants.PREFS.ANDROID_ID, androidID);
            editor.commit();
        }

        String advertisementID = mPreferences.getString(Constants.PREFS.ADVERTISEMENT_ID, null);
        if(advertisementID == null){
            AdvertisementID resolver = new AdvertisementID(getApplicationContext(), mPreferences);
            resolver.execute();
        }

        String marketName = mPreferences.getString(Constants.PREFS.MARKET_NAME, null);
        String manufacturer = mPreferences.getString(Constants.PREFS.MANUFACTURER, null);
        if(marketName == null || manufacturer == null) {
            DeviceName.with(getApplicationContext()).request(new DeviceName.Callback() {
                @Override
                public void onFinished(DeviceName.DeviceInfo info, Exception error) {
                    editor.putString(Constants.PREFS.MARKET_NAME, info.marketName);
                    editor.putString(Constants.PREFS.MANUFACTURER, info.manufacturer);
                    editor.commit();
                }
            });
        }


    }


    public String getDrammerBinary(){
        return Utils.getBasename(mDrammerBinary);
    }


    private void setIsVulnerable(boolean isVulnerable){
        View content = findViewById(R.id.content_main);
        TextView label = (TextView) findViewById(R.id.text_vulnerable);

        if(isVulnerable){
            content.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBad));
            label.setText(getResources().getString(R.string.text_vulnerable));
            label.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBadText));
            //For API level < 23
            //content.setBackgroundColor(getResources().getColor(R.color.colorBad));
            //label.setTextColor(getResources().getColor(R.color.colorBadText));
        } else {
            //content.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGood));
            label.setText(getResources().getString(R.string.text_notvulnerable));
            label.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGoodText));
            // For API level < 23
            //label.setTextColor(getResources().getColor(R.color.colorGoodText));
        }
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(Constants.PREFS.VULNERABLE, isVulnerable);
        editor.commit();
    }

    private boolean isServiceRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DrammerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void getIMEIPermission(){
        boolean firstTime = !mPreferences.contains(Constants.PREFS.IMEI);

        if ((!firstTime) && mPreferences.getString(Constants.PREFS.IMEI, null) != null){
            return;
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (firstTime || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle(getResources().getString(R.string.text_permission_title));
                alertBuilder.setMessage(getResources().getString(R.string.text_permission_message));
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, Constants.PERMISSION_REQUEST_IMEI);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };
                alertBuilder.setNegativeButton(android.R.string.no, dialogClickListener);
                    alertBuilder.setPositiveButton(android.R.string.yes, dialogClickListener);
                AlertDialog alert = alertBuilder.create();
                alert.show();
                //Toast.makeText(getApplicationContext(), getResources().getString(R.string.text_permission_message), Toast.LENGTH_LONG).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, Constants.PERMISSION_REQUEST_IMEI);
            }
        }
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, Constants.PERMISSION_REQUEST_IMEI);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if (requestCode == Constants.PERMISSION_REQUEST_IMEI){
            if ((grantResults.length == 1) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                Log.d(TAG, "READ_PHONE_STATE_PERMISSION granted");
                try {
                    IMEI = ((TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                } catch (SecurityException ex) {
                    Log.d(TAG, "READ_PHONE_STATE_PERMISSION not avaiable");
                } catch (Exception ex) {}
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(Constants.PREFS.IMEI, IMEI);
                editor.commit();
            } else {
                Log.d(TAG, "READ_PHONE_STATE_PERMISSION denied");

            }
        }
    }



    private boolean prepareHammer(){
        String architecture = DeviceStatistics.getArchitecture();
        if(architecture.equals(Constants.ARCH.UNKNOWN) || (Preferences.DRAMMER_BINARIES.get(architecture) == null)){
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.text_unavailable), Toast.LENGTH_LONG).show();
            return false;
        }

        Preferences.DRAMMER_BINARY = Preferences.DRAMMER_BINARIES.get(architecture);
        mDrammerBinary = Utils.getAbsolutePath(getApplicationContext(), Preferences.DRAMMER_BINARY);

        if (isServiceRunning()){
            return true;
        }

        File binary = new File(mDrammerBinary);
        Date lastModified = null;
        Date lastUpdated = lastModified;

        if(binary.exists()) {
            lastModified = new Date(binary.lastModified());
        }
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            lastUpdated = new Date(info.lastUpdateTime);
        } catch (PackageManager.NameNotFoundException ignored) {}

        InputStream source = null;
        try {
            source = getAssets().open(Preferences.DRAMMER_BINARY);
        } catch (IOException ignored) {
            if(Preferences.USE_LOCAL_BINARY){
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.text_unavailable), Toast.LENGTH_LONG).show();
                return false;
            }
        }


        /* TODO: move to AsyncTask (NetworkOnMainThreadException)
        boolean inetAvailable = false;
        if(Utils.isNetworkConnected(getApplicationContext())){
            try{
                URI url = new URI(Preferences.DOWNLOAD_URL);
                Log.d(TAG, "Checking for availability of host... " + url.getHost());
                inetAvailable = Utils.isInternetAvailable(url.getHost());
                Log.d(TAG, "... success");
            } catch (Exception ignored){
                ignored.printStackTrace();
            }
        } */

        if(Preferences.USE_LOCAL_BINARY && (source != null)) {
            Log.d(TAG, "Using local binary: lastModified=" + lastModified + " lastUpdated=" + lastUpdated);
            if ((lastModified == null) || (lastModified.before(lastUpdated))) {
                FileOutputStream destination = null;
                try {
                    destination = new FileOutputStream(binary);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = source.read(buffer)) != -1) {
                        destination.write(buffer, 0, read);
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Failed to write to Drammer binary file to " + mDrammerBinary);
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to copy Drammer binary from assets to " + mDrammerBinary);
                    e.printStackTrace();
                } finally {
                    try {
                        if (destination != null)
                            destination.close();
                        if (source != null)
                            source.close();
                    } catch (IOException ignored) {
                    }
                }
                try {
                    Runtime.getRuntime().exec("/system/bin/chmod 744 " + mDrammerBinary);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to set permissions on Drammer binary " + mDrammerBinary);
                    e.printStackTrace();
                    return false;
                }
            }
        } else {
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage(getResources().getString(R.string.text_download));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);

            Log.v(TAG,"Downloading " + Preferences.DOWNLOAD_URL + Preferences.DRAMMER_BINARY);
            final DownloadTask downloadTask = new DownloadTask(MainActivity.this, lastModified);
            downloadTask.execute(Preferences.DOWNLOAD_URL + Preferences.DRAMMER_BINARY);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    downloadTask.cancel(true);
                }
            });
        }

        return true;
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_start:
                startHammering();
                break;
            case R.id.button_stop:
                stopHammering();
                break;
            default:
                break;
        }
    }

    private void startHammering(){

        mStartButton.setEnabled(false);
        mStopButton.setEnabled(true);
        mStopButton.setVisibility(View.VISIBLE);

        getIMEIPermission();

        mResultView.setText("");
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(Constants.PREFS.RESULT, mResultView.getText().toString());
        editor.commit();

        Intent startIntent = new Intent(MainActivity.this, DrammerService.class);
        startIntent.setAction(Constants.ACTION.START_HAMMERING);
        startService(startIntent);
    }

    private void stopHammering(){
        Intent stopIntent = new Intent(MainActivity.this, DrammerService.class);
        stopIntent.setAction(Constants.ACTION.STOP_HAMMERING);
        startService(stopIntent);
    }

    private void resetHammering(){
        Intent startIntent = new Intent(MainActivity.this, DrammerService.class);
        startIntent.setAction(Constants.ACTION.RESET_HAMMERING);
        startService(startIntent);
        if(mHammerReady) {
            mStartButton.setEnabled(true);
        }
        mStopButton.setEnabled(false);
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        private Date mLastDownloaded;

        public DownloadTask(Context context, Date lastDownloaded) {
            this.context = context;
            this.mLastDownloaded = lastDownloaded;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            FileOutputStream output = null;
            HttpsURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpsURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                Date lastModified = new Date(connection.getLastModified());
                if ((mLastDownloaded != null) && lastModified.before(mLastDownloaded)) {
                    return null;
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();


                // download the file
                input = connection.getInputStream();

                File binary = new File(mDrammerBinary);
                output = new FileOutputStream(binary);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }

                try {
                    Runtime.getRuntime().exec("/system/bin/chmod 744 " + mDrammerBinary);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to set permissions on Drammer binary " + mDrammerBinary);
                    e.printStackTrace();
                }

            } catch (SSLHandshakeException e){
                Log.e(TAG, "SSL error downloading Drammer binary: " + e.getMessage());
                Toast.makeText(context, getResources().getString(R.string.text_download_sslerror), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error downloading Drammer binary: " + e.getMessage());
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null) {
                Toast.makeText(context, getResources().getString(R.string.text_download_error) + ": " + result, Toast.LENGTH_LONG).show();
                mHammerReady = false;
            } else {
                Toast.makeText(context, getResources().getString(R.string.text_download_success), Toast.LENGTH_SHORT).show();
                mHammerReady = true;
            }
        }
    }


}
