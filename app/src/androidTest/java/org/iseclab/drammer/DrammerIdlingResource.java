package org.iseclab.drammer;

import android.app.ActivityManager;
import android.content.Context;
import android.support.test.espresso.IdlingResource;
import android.util.Log;

import org.iseclab.drammer.code.DrammerService;
import org.iseclab.drammer.code.ProcessKiller;

import java.util.List;

public class DrammerIdlingResource implements IdlingResource {

    private static final String TAG = DrammerIdlingResource.class.getSimpleName();

    private IdlingResource.ResourceCallback resourceCallback;
    private Context context;

    private static String resourceName;
    private static boolean onStart;

    public DrammerIdlingResource(Context context) {
        this.context = context;
    }

    public void setResourceName(String resourceName){
        this.resourceName = resourceName;
    }

    public void setOnStart(boolean onStart){
        this.onStart = onStart;
    }

    @Override
    public String getName() {
        return DrammerIdlingResource.class.getName();
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = false;
        boolean running = isHammerRunning();
        Log.e(TAG, "DRAMMER IS RUNNING=" + running);

        if(onStart){
            idle = running;
            Log.e(TAG, "WAITING FOR START, idle=" + idle);
        } else {
            idle = !running;
            Log.e(TAG, "WAITING FOR STOP, idle=" + idle);
        }

        if (idle && resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
        return idle;

    }


    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo info: runningServices) {
            if (DrammerService.class.getName().equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isHammerRunning(){
        return new ProcessKiller().isRunning(resourceName);
    }


}