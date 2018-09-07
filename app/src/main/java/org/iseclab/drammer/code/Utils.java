package org.iseclab.drammer.code;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static String getAbsolutePath(Context context, String filename){
        return new File(context.getFilesDir(), filename).getAbsolutePath();
    }

    public static String getBasename(String filename){
        return new File(filename).getName();
    }

    public static String getFileHash(String filename) {
        String hash = null;
        InputStream is = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            is = new FileInputStream(new File(filename));
            byte[] buffer = new byte[16384];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] sha256sum = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < sha256sum.length; i++) {
                builder.append(Integer.toString((sha256sum[i] & 0xff) + 0x100, 16).substring(1));
            }
            hash = builder.toString();
        } catch (NoSuchAlgorithmException ignored) {
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        } finally {
            try {
                is.close();
            } catch (Exception ignored) {}
        }
        return hash;
    }


    public static String formatTime(long miliseconds){
        return String.format("%02d:%02d:%02d.%04d",
                TimeUnit.MILLISECONDS.toHours(miliseconds),
                TimeUnit.MILLISECONDS.toMinutes(miliseconds) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(miliseconds) % TimeUnit.MINUTES.toSeconds(1),
                miliseconds % TimeUnit.SECONDS.toMillis(1));
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return ((activeNetwork != null) && (activeNetwork.isConnectedOrConnecting()));
    }

    public static boolean isInternetAvailable(String domain) {
        try {
            InetAddress ipAddr = InetAddress.getByName(domain);
            return !ipAddr.equals("");
        } catch (UnknownHostException e) {
            return false;
        }
    }


}
