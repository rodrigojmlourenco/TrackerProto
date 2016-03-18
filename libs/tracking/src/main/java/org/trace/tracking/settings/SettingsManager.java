package org.trace.tracking.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Rodrigo Lourenço
 * @version 0.0
 *
 * This class provides a single point of entry for the management of the applications tracking
 * settings. These include not only the tracking settings, but the uploading as well.
 */
public class SettingsManager {

    private Context mContext;
    private TrackingProfile mTrackingProfile;
    private Object mLock = new Object();

    private static SettingsManager MANAGER = null;

    private SettingsManager(Context context){
        mContext = context;

        String profile =
                context.getSharedPreferences(Constansts.SETTINGS_PREFERENCES,Context.MODE_PRIVATE)
                        .getString(Constansts.TRACKING_PROFILE_KEY, "");

        if(profile.isEmpty()) {
            mTrackingProfile = new TrackingProfile();
            saveTrackingProfile(mTrackingProfile);
        }else{
            JsonParser parser = new JsonParser();
            mTrackingProfile = new TrackingProfile((JsonObject) parser.parse(profile));
        }
    }

    public static SettingsManager getInstance(Context context){

        synchronized (SettingsManager.class){
            if(MANAGER == null)
                MANAGER = new SettingsManager(context);
        }

        return MANAGER;
    }

    public TrackingProfile getTrackingProfile(){
        synchronized (mLock) {
            return mTrackingProfile;
        }
    }

    public void saveTrackingProfile(TrackingProfile profile){

        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(Constansts.SETTINGS_PREFERENCES, Context.MODE_PRIVATE).edit();

        editor.putString(Constansts.TRACKING_PROFILE_KEY, profile.toString());

        synchronized (mLock){
            editor.commit();
            mTrackingProfile = profile;
        }

    }

    public interface Constansts {
        public final String SETTINGS_PREFERENCES = "settings";
        public final String TRACKING_PROFILE_KEY = "trackingProfile";
    }

}
