package org.iseclab.drammer.code;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.iseclab.drammer.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

public class DeviceStatistics {

    private static final String TAG = DeviceStatistics.class.getSimpleName();

    //@TargetApi(21)
    public static String getArchitecture(){
        String ABIs = getProperty("ro.product.cpu.abilist");
        if(ABIs.isEmpty()){
           ABIs = getProperty("ro.product.cpu.abi");
        }
        if(ABIs.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                ABIs = TextUtils.join(" ", Build.SUPPORTED_ABIS);
            } else {
                ABIs = Build.CPU_ABI;
            }
        }

        Log.d(TAG, "Available architectures: " + ABIs);

        if(ABIs.contains(Constants.ARCH.ARM64))
            return Constants.ARCH.ARM64;
        else if(ABIs.contains(Constants.ARCH.ARM))
            return Constants.ARCH.ARM;
        else if(ABIs.contains(Constants.ARCH.x86_64))
            return Constants.ARCH.x86_64;
        else if(ABIs.contains(Constants.ARCH.x86))
            return Constants.ARCH.x86;

        return Constants.ARCH.UNKNOWN;
    }


    public static Map<String, String> getBuildProperties(Class c){
        Map<String, String> deviceStats = new TreeMap<String, String>();

        for(Field field: c.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            try {
                Object value = field.get(null);
                if (field.getType().isArray()) {
                    value = TextUtils.join(",", (String[]) value);
                } else if (value == null) {
                    value = "";
                }
                deviceStats.put(c.getSimpleName() + "." + field.getName(), value.toString());
            } catch (IllegalAccessException ignored){}
        }
        return deviceStats;
    }

    public static Map<String, String> getSystemProperties() {
        Map<String, String> deviceStats = new TreeMap<String, String>();

        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder().command("/system/bin/getprop");
            builder.redirectErrorStream(true);
            process = builder.start();
        } catch (IOException ignored) {}

        if(process == null){
            return deviceStats;
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if(parts.length != 2){
                    continue;
                }
                String key = parts[0].replace('[', ' ').replace(']', ' ').trim();
                String val = parts[1].replace('[', ' ').replace(']', ' ').trim();
                if(key.startsWith("ro")) {
                    deviceStats.put(key, val);
                }
            }
        } catch (IOException ignored) {}

        try {
            process.destroy();
        } catch (Exception ignored){}

        return deviceStats;
    }

    public static Map<String, String> fingerprintDevice(){
        Map<String, String> deviceStats = new TreeMap<String, String>();

        deviceStats.putAll(getBuildProperties(Build.class));
        deviceStats.putAll(getBuildProperties(Build.VERSION.class));
        deviceStats.putAll(getSystemProperties());

        deviceStats.put("patch_level", getProperty("ro.build.version.security_patch"));
        deviceStats.put("build_date", getProperty("ro.build.date.utc"));

        return deviceStats;

    }

    @SuppressWarnings("unchecked")
    private static String getProperty(String propertyName){
        String property = "";
        try {
            Class systemPropertyClass = Class.forName("android.os.SystemProperties");
            property = (String) systemPropertyClass.getMethod("get", new Class[]{String.class}).invoke(systemPropertyClass, new Object[]{propertyName});
        } catch(ClassNotFoundException e){
        } catch(NoSuchMethodException ignored) {
        } catch(IllegalAccessException ignored) {
        } catch(InvocationTargetException ignored) {}

        return property;

    }
}
