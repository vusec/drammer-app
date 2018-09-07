package org.iseclab.drammer.code;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import org.iseclab.drammer.Constants;
import org.iseclab.drammer.Preferences;

import java.io.IOException;

public class AdvertisementID extends AsyncTask<Void, Void, String> {

    private static final String TAG = AdvertisementID.class.getSimpleName();

    private Context mContext;
    private SharedPreferences mPreferences;

    public AdvertisementID(Context context, SharedPreferences preferences){
        this.mContext = context;
        this.mPreferences = preferences;
    }

    protected String doInBackground(Void... urls) {
        String uniqueID = "N/A";

        AdvertisingIdClient.Info adInfo = null;
        try {
            int nrOfTries = 0;
            // Bug in Nougat where Advertisement ID on first execution is NULL?
            while (nrOfTries <= Preferences.RETRY_ADID) {
                adInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
                if (adInfo != null) {
                    uniqueID = adInfo.getId();
                    break;
                }
                nrOfTries += 1;
            }
        } catch (IOException e) {
            Log.e(TAG, "Google Play Services I/O exception");
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, "Google Play Services unavailable");
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, "Google Play Services error");
            e.printStackTrace();
        }

        return uniqueID;
    }

    protected void onPostExecute(String uniqueID) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(Constants.PREFS.ADVERTISEMENT_ID, uniqueID);
        editor.commit();
    }
}