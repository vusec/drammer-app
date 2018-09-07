package org.iseclab.drammer.code;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.iseclab.drammer.Constants;
import org.iseclab.drammer.Preferences;
import org.iseclab.drammer.R;
import org.iseclab.drammer.ui.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DrammerService extends Service implements UploadListener {

    private static final String TAG = DrammerService.class.getSimpleName();

    private static String mDrammerBinary = null;
    private static String mDrammerOutput = null;

    private Process mHammerProc = null;
    private Thread mHammerThread = null;
    private PowerManager.WakeLock mWakeLock = null;
    private String mCommand = null;

    private long mHammerStart = 0;
    private long mHammerEnd = 0;
    private long mHammerStopped = 0;
    private int mExitCode = Constants.DEFAULT_EXIT_CODE;

    private StringBuilder mResult = null;

    private static SharedPreferences mPreferences;

    private static boolean isActivityRunning;

    @Override
    public void onCreate(){
        super.onCreate();
        mPreferences = getSharedPreferences(Preferences.DRAMMER_PREFS, Activity.MODE_PRIVATE);
        mDrammerBinary = mPreferences.getString(Constants.PREFS.BINARY, null);
        mDrammerOutput = mPreferences.getString(Constants.PREFS.OUTPUT, null);

        mResult = new StringBuilder();

        setIsRunning(false);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());

        isActivityRunning = true;
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        if((intent == null) || (intent.getAction() == null) || mDrammerBinary == null){
            return START_STICKY;
        }
        if(!isRunning() && (intent.getAction().equals(Constants.ACTION.START_HAMMERING))) {
            setIsRunning(true);
            isActivityRunning = true;
            sendStatusBroadcast(Constants.STATUS.STARTED, null);
            showNotification(false);
            mHammerThread = new Thread(new Runnable(){
                @Override
                public void run() {
                    mWakeLock.acquire();
                    Log.d(TAG, "Aquired wake lock: isHeld=" + mWakeLock.isHeld());

                    mHammerStart = System.currentTimeMillis();

                    try {
                        hammerTime();
                        Log.d(TAG, "Background Drammer service finished");
                    } catch (IOException exception) {
                        Log.e(TAG, "Error running command line: " + exception.getMessage());
                        exception.printStackTrace();
                        sendStatusBroadcast(Constants.STATUS.ERROR, exception.getMessage());
                    } catch (InterruptedException exception) {
                        Log.e(TAG, "Error running command line: " + exception.getMessage());
                        //exception.printStackTrace();
                        sendStatusBroadcast(Constants.STATUS.ERROR, exception.getMessage());
                    } catch (Exception exception) {
                        Log.e(TAG, "Error running command line: " + exception.getMessage());
                        //exception.printStackTrace();
                        sendStatusBroadcast(Constants.STATUS.ERROR, exception.getMessage());
                    }

                    mHammerEnd = System.currentTimeMillis();

                    Log.d(TAG, "Execution took " + Utils.formatTime(mHammerEnd-mHammerStart));

                    Intent intent = new Intent(DrammerService.this, MainActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);

                    if(mPreferences.getBoolean(Constants.PREFS.UPLOAD, false) == true) {
                        String drammerHash = Utils.getFileHash(mDrammerBinary);
                        UploadTask uploadTask = new UploadTask(getApplicationContext(), null, mDrammerOutput, mHammerStart, mHammerEnd, mHammerStopped, drammerHash, mCommand, mExitCode);
                        uploadTask.setListener(DrammerService.this);
                        uploadTask.execute(Preferences.UPLOAD_URL);
                    }

                    mWakeLock.release();
                    Log.d(TAG, "Released wake lock: isHeld=" + mWakeLock.isHeld());

                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putString(Constants.PREFS.RESULT, mResult.toString());
                    editor.commit();

                    if(Preferences.KEEP_NOTIFICATION){
                        showNotification(true);
                    }
                    if(!Preferences.VERBOSE_OUTPUT){
                        Log.e(TAG, mResult.toString());
                    }
                    sendFinishedBroadcast(Constants.STATUS.FINISHED, mResult.toString());


                    if(mPreferences.getBoolean(Constants.PREFS.UPLOAD, false) == false) {
                        onUploadCompleted();
                    }
                }
            });
            mHammerThread.start();

        } else if((intent.getAction().equals(Constants.ACTION.STOP_HAMMERING))) {
            mHammerStopped = System.currentTimeMillis();
            killHammer();
        } else if((intent.getAction().equals(Constants.ACTION.ACTIVITY_PAUSE))) {
            isActivityRunning = false;
        } else if((intent.getAction().equals(Constants.ACTION.ACTIVITY_RESUME))) {
            isActivityRunning = true;
            sendStatusBroadcast(Constants.STATUS.BATCH_UPDATE, mResult.toString());
        } else if((intent.getAction().equals(Constants.ACTION.RESET_HAMMERING))) {
            setIsRunning(false);
            stopSelf();
            stopForeground(true);
        }


        return START_STICKY;
    }

    public void onUploadCompleted(){
        sendStatusBroadcast(Constants.STATUS.UPLOAD_COMPLETED, null);
        setIsRunning(false);
        stopSelf();
        stopForeground(true);
    }


    private void setIsRunning(boolean isRunning){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(Constants.PREFS.ISRUNNING, isRunning);
        editor.commit();
    }

    private boolean isRunning(){
        return mPreferences.getBoolean(Constants.PREFS.ISRUNNING, false);
    }

    private void sendStatusBroadcast(int status, String message){
        Intent intent = new Intent(Constants.BROADCAST.DRAMMER_NOTIFICATION);
        intent.putExtra(Constants.EXTRAS.STATUS, status);
        if(message != null){
            intent.putExtra(Constants.EXTRAS.MESSAGE, message);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendFinishedBroadcast(int status, String message){
        Intent intent = new Intent(Constants.BROADCAST.DRAMMER_NOTIFICATION);
        intent.putExtra(Constants.EXTRAS.STATUS, status);
        intent.putExtra(Constants.EXTRAS.MESSAGE, message);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void hammerTime() throws IOException, InterruptedException {

        if(Preferences.KILL_BACKGROUND_PROCESSES){
            killBackgroundProcesses();
        }

        int defragTimeout = mPreferences.getInt(Constants.PREFS.DEFRAGTIMEOUT, Preferences.DRAMMER_DEFRAG_TIMEOUT);

        ProcessBuilder builder = new ProcessBuilder(mDrammerBinary, "-f"+mDrammerOutput, "-t"+Preferences.DRAMMER_TIMEOUT, "-d"+defragTimeout);
        builder.directory(getApplicationContext().getFilesDir());
        builder.redirectErrorStream(true);

        mCommand = TextUtils.join(" ", builder.command());
        Log.d(TAG, "Executing " + mCommand);
        mHammerProc = builder.start();

        if(Preferences.VERBOSE_OUTPUT) {
            InputStream stdout = mHammerProc.getInputStream();
            String line;
            mResult = new StringBuilder();
            int numRead;
            byte[] buffer = new byte[1024];
            while ((numRead = stdout.read(buffer)) > 0) {
                line = new String(buffer, 0, numRead);
                mResult.append(line);
                if(isActivityRunning){
                    sendStatusBroadcast(Constants.STATUS.UPDATE, line);
                }
            }
        }

        if(mHammerProc != null){
            mHammerProc.waitFor();
            Log.d(TAG, "Drammer finished with exitValue=" + mHammerProc.exitValue());
            mExitCode = mHammerProc.exitValue();
        }
    }

    private void killHammer(){
        if(mHammerThread != null){
            mHammerThread.interrupt();
            mHammerThread = null;
        }
        if(mHammerProc != null){
            Log.d(TAG, "Killing Drammer process");
            new ProcessKiller().killProcesses(Preferences.DRAMMER_BINARY, android.os.Process.SIGNAL_KILL);
            mHammerProc.destroy();
            mHammerProc = null;
            Log.d(TAG, "Done killing");
        }
    }

    private void killBackgroundProcesses(){
        Log.d(TAG, "Killing background processes...");
        Context aContext = getApplicationContext();
        ActivityManager aManager = (ActivityManager) aContext.getSystemService(aContext.ACTIVITY_SERVICE);
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            aManager.killBackgroundProcesses(packageInfo.packageName);
        }
    }

    private void showNotification(boolean update){
        Intent notificationIntent = new Intent(DrammerService.this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setTicker(getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_stat_hammer_notification)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if(update){
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notifyBuilder.setContentText(getResources().getString(R.string.text_notification_done));
            notifyBuilder.setAutoCancel(true);
            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notifyBuilder.build());
        } else {
            notifyBuilder.setContentText(getResources().getString(R.string.text_notification));
            Notification notification = notifyBuilder.build();
            notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
        }
    }

    @Override
    public void onDestroy() {
        killHammer();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG, "LOW MEMORY!");
        new ProcessKiller().killProcesses(Preferences.DRAMMER_BINARY, android.os.Process.SIGNAL_USR1);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, "Could not sleep!");
        }
    }

    @Override
    public void onTrimMemory(int level) {
        Log.d(TAG, "ON TRIM MEMORY " + level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL) {
            new ProcessKiller().killProcesses(Preferences.DRAMMER_BINARY, android.os.Process.SIGNAL_USR1);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

