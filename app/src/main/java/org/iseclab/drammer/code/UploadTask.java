package org.iseclab.drammer.code;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import org.iseclab.drammer.Constants;
import org.iseclab.drammer.Preferences;
import org.iseclab.drammer.R;
import org.iseclab.drammer.ui.MainActivity;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class UploadTask extends AsyncTask<String, Void, Integer> {

    private static final String TAG = UploadTask.class.getSimpleName();

    private MainActivity mActivity;
    private UploadListener mListener;
    private ProgressDialog mProgressDialog;

    private String filename;
    private long start;
    private long end;
    private long stopped;
    private String hash;
    private String cmdLine;
    private int exitCode;
    private String versionName;
    private int versionCode;

    private SharedPreferences mPreferences;

    public UploadTask(Context context, MainActivity activity){
        this(context, activity, "", 0, 0, 0, "", "", Constants.DEFAULT_EXIT_CODE);
    }

    public UploadTask(Context context, MainActivity activity, String filename, long start, long end, long stopped, String hash, String cmdLine, int exitCode) {
        mActivity = activity;
        if(mActivity == null){
            mProgressDialog = null;
        } else {
            mProgressDialog = new ProgressDialog(mActivity);
        }

        try {
            PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            this.versionName = pkgInfo.versionName;
            this.versionCode = pkgInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get package info!");
            this.versionCode = 0;
            this.versionName = "?";
        }

        this.mPreferences = context.getSharedPreferences(Preferences.DRAMMER_PREFS, Activity.MODE_PRIVATE);

        this.filename = filename;
        this.start = end;
        this.end = start;
        this.stopped = stopped;
        this.hash = hash;
        this.cmdLine = cmdLine;
        this.exitCode = exitCode;
    }

    public void setListener(UploadListener listener){
        mListener = listener;
    }

    @Override
    protected void onPreExecute(){
        if((mActivity != null) && (!mActivity.isFinishing())){
            mProgressDialog.setMessage(mActivity.getResources().getString(R.string.text_upload));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(true);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, mActivity.getResources().getString(R.string.text_upload_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            mProgressDialog.show();
        }
    }

    @Override
    protected Integer doInBackground(String... sUrl) {
        HttpsURLConnection connection = null;
        FileInputStream inputStream = null;
        DataOutputStream uploadStream = null;

        byte[] buffer;
        int bytesAvailable, bufferSize, bytesRead;
        int maxBufferSize = 1 * 1024 * 1024;

        Map<String, String> deviceStats = DeviceStatistics.fingerprintDevice();

        File logFile = new File(filename);
        if (!logFile.isFile()) {
            logFile = null;
            Log.e(TAG, "Logfile does not exist: " + filename);
        } else {
            try {
                inputStream = new FileInputStream(logFile);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Cannot open file: " + filename);
                e.printStackTrace();
            }
        }

        StringBuilder statistics = new StringBuilder();
        statistics.append("drammer_sha256=" + hash + "\n");
        statistics.append("hammer_start=" + start + "\n");
        statistics.append("hammer_end=" + end + "\n");
        statistics.append("hammer_stopped=" + stopped + "\n");
        statistics.append("cmdline=" + cmdLine + "\n");
        statistics.append("exit_code=" + exitCode + "\n");
        statistics.append("versionCode=" + versionCode + "\n");
        statistics.append("versionName=" + versionName + "\n");
        statistics.append("imei=" + mPreferences.getString(Constants.PREFS.IMEI, "") + "\n");
        statistics.append("install_uuid=" + mPreferences.getString(Constants.PREFS.INSTAlL_UUID, "") + "\n");
        statistics.append("android_id=" + mPreferences.getString(Constants.PREFS.ANDROID_ID, "") + "\n");
        statistics.append("advertisement_id=" + mPreferences.getString(Constants.PREFS.ADVERTISEMENT_ID, "") + "\n");
        statistics.append("manufacturer=" + mPreferences.getString(Constants.PREFS.MANUFACTURER, "") + "\n");
        statistics.append("market_name=" + mPreferences.getString(Constants.PREFS.MARKET_NAME, "") + "\n");

        for (Map.Entry<String, String> entry : deviceStats.entrySet()) {
            statistics.append(entry.getKey() + "=" + entry.getValue() + "\n");
        }

        try {
            String lineEnd = "\r\n";
            String hyphens = "--";
            String boundary = "---------------------------13575110919745193892037427964";

            URL url = new URL(sUrl[0]);
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            uploadStream = new DataOutputStream(connection.getOutputStream());

            uploadStream.writeBytes(hyphens + boundary + lineEnd);
            uploadStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + Preferences.DRAMMER_LOG + "\"" + lineEnd);
            uploadStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            uploadStream.writeBytes(lineEnd);

            if ((logFile != null) && (inputStream != null)) {
                bytesAvailable = inputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = inputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    uploadStream.write(buffer, 0, bufferSize);
                    bytesAvailable = inputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = inputStream.read(buffer, 0, bufferSize);
                }
                uploadStream.writeBytes(lineEnd);
            }

            uploadStream.writeBytes("[DEVICE STATISTICS]" + lineEnd);
            uploadStream.writeBytes(statistics.toString());
            uploadStream.writeBytes(lineEnd);

            uploadStream.writeBytes(hyphens + boundary + lineEnd);
            uploadStream.writeBytes("Content-Disposition: form-data; name=\"submit\"" + lineEnd);
            uploadStream.writeBytes(lineEnd + "upload" + lineEnd);

            uploadStream.writeBytes(hyphens + boundary + hyphens + lineEnd + lineEnd);

            uploadStream.flush();
            uploadStream.close();


            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            Log.d(TAG, "Upload finished: Server returned HTTP " + responseCode + " " + responseMessage);


            return responseCode;

        } catch (IOException e) {
            Log.e(TAG, "Unable to send logfile: " + logFile);
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null)
                try{ inputStream.close(); } catch (IOException ignored){}
            if (uploadStream != null)
                try{ uploadStream.close(); } catch (IOException ignored){}
            if (connection != null)
                connection.disconnect();
        }
    }

    @Override
    protected void onPostExecute(Integer responseCode) {
        if((mProgressDialog !=null) && (mProgressDialog.isShowing())){
            mProgressDialog.dismiss();
        }
        if(mListener != null){
            mListener.onUploadCompleted();
        }
        super.onPostExecute(responseCode);
    }



}
