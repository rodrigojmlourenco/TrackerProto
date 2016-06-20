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
public class ConfigurationsManager {

    private Context mContext;
    private ConfigurationProfile mTrackingProfile;
    private Object mLock = new Object();

    private static ConfigurationsManager MANAGER = null;

    private ConfigurationsManager(Context context){
        mContext = context;

        String profile =
                context.getSharedPreferences(Constants.SETTINGS_PREFERENCES,Context.MODE_PRIVATE)
                        .getString(Constants.TRACKING_PROFILE_KEY, "");

        if(profile.isEmpty()) {
            mTrackingProfile = new ConfigurationProfile();
            saveTrackingProfile(mTrackingProfile);
        }else{
            JsonParser parser = new JsonParser();
            mTrackingProfile = new ConfigurationProfile((JsonObject) parser.parse(profile));
        }
    }

    /**
     * Fetches an instance of the SettingManager singleton.
     * @param context
     * @return An instance of the Setting Manager singleton.
     */
    public static ConfigurationsManager getInstance(Context context){

        synchronized (ConfigurationsManager.class){
            if(MANAGER == null)
                MANAGER = new ConfigurationsManager(context);
        }

        return MANAGER;
    }

    /**
     * Fetches the current tracking profile.
     * @return The ConfigurationProfile
     */
    public ConfigurationProfile getTrackingProfile(){
        synchronized (mLock) {
            return mTrackingProfile;
        }
    }

    /**
     * Saves the provided ConfigurationProfile as the current tracking profile.
     * @param profile The new ConfigurationProfile.
     */
    public void saveTrackingProfile(ConfigurationProfile profile){

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
