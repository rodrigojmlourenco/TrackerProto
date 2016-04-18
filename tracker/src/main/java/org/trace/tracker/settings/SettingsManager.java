package org.trace.tracker.settings;

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
 * <br>
 * These settings are stored persistently inside the applications Shared Preferences.
 * <br>
 * The direct use of this class is unadvisable, instead, the Client should be used.
 * TODO: dar suporte a partir do Client
 */
public class SettingsManager {

    private Context mContext;
    private TrackingProfile mTrackingProfile;
    private Object mLock = new Object();

    private static SettingsManager MANAGER = null;

    private SettingsManager(Context context){
        mContext = context;

        String profile =
                context.getSharedPreferences(Constants.SETTINGS_PREFERENCES,Context.MODE_PRIVATE)
                        .getString(Constants.TRACKING_PROFILE_KEY, "");

        if(profile.isEmpty()) {
            mTrackingProfile = new TrackingProfile();
            saveTrackingProfile(mTrackingProfile);
        }else{
            JsonParser parser = new JsonParser();
            mTrackingProfile = new TrackingProfile((JsonObject) parser.parse(profile));
        }
    }

    /**
     * Fetches an instance of the SettingManager singleton.
     * @param context
     * @return An instance of the Setting Manager singleton.
     */
    public static SettingsManager getInstance(Context context){

        synchronized (SettingsManager.class){
            if(MANAGER == null)
                MANAGER = new SettingsManager(context);
        }

        return MANAGER;
    }

    /**
     * Fetches the current tracking profile.
     * @return The TrackingProfile
     */
    public TrackingProfile getTrackingProfile(){
        synchronized (mLock) {
            return mTrackingProfile;
        }
    }

    /**
     * Saves the provided TrackingProfile as the current tracking profile.
     * @param profile The new TrackingProfile.
     */
    public void saveTrackingProfile(TrackingProfile profile){

        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(Constants.SETTINGS_PREFERENCES, Context.MODE_PRIVATE).edit();

        editor.putString(Constants.TRACKING_PROFILE_KEY, profile.toString());

        synchronized (mLock){
            editor.commit();
            mTrackingProfile = profile;
        }

    }

    private interface Constants {
        String SETTINGS_PREFERENCES = "settings";
        String TRACKING_PROFILE_KEY = "trackingProfile";
    }
}
