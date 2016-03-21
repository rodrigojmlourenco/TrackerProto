package org.trace.tracking.store.auth;

import android.content.Context;
import android.content.SharedPreferences;

import org.trace.tracking.Constants;

/**
 * TODO: THIS IS NOT SECURE!! Use Android recommended APIs
 */
public class AuthenticationManager {

    public static final String AUTH_TOKEN = "auth_token";
    private static final String AUTH_SETTINGS_KEY = "auth_settings";



    private String sessionId;
    private final Context context;

    public AuthenticationManager(Context context){
        this.context = context;
    }


    public String getUsername(){
        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs.getString(Constants.store.USERNAME_KEY, "");
    }

    public String getPassword(){
        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs.getString(Constants.store.PASSWORD_KEY, "");
    }

    public void storeCredentials(String username, String password){

        SharedPreferences.Editor editor =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.putString(Constants.store.USERNAME_KEY, username);
        editor.putString(Constants.store.PASSWORD_KEY, password);
        editor.commit();

    }

    public boolean isFirstTime() {

        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs == null || !(prefs.contains(Constants.store.USERNAME_KEY) && prefs.contains(Constants.store.PASSWORD_KEY));

    }

    public static boolean isFirstTime(Context context){
        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        if(prefs == null) return true;

        return !(prefs.contains(Constants.store.USERNAME_KEY) && prefs.contains(Constants.store.PASSWORD_KEY));
    }

    public static boolean clearCredentials(Context context){
        SharedPreferences.Editor editor =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.clear();
        editor.commit();

        return true;
    }


    public void storeAuthenticationToken(String token){
        SharedPreferences.Editor editor =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.putString(AUTH_TOKEN, token);
        editor.commit();
    }

    public String getAuthenticationToken(){
        return context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE)
                .getString(AUTH_TOKEN, "");
    }

    public void clearAuthenticationToken(){
        SharedPreferences.Editor editor =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.remove(AUTH_TOKEN);
        editor.commit();
    }
}
