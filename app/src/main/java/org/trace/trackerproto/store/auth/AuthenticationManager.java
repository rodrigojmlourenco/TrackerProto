package org.trace.trackerproto.store.auth;

import android.content.Context;
import android.content.SharedPreferences;

import org.trace.trackerproto.Constants;

/**
 * TODO: THIS IS NOT SECURE!! Use Android recommended APIs
 */
public class AuthenticationManager {

    private static final String AUTH_SETTINGS_KEY = "auth_settings";



    private String sessionId;
    private final Context context;

    public AuthenticationManager(Context context){
        this.context = context;
    }

    /*
    public void login(){
        String username, password;
        SharedPreferences prefs =
            context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        username = prefs.getString(Constants.USERNAME_KEY, "");
        password = prefs.getString(Constants.PASSWORD_KEY, "");

        if(username.isEmpty() || password.isEmpty())
            return;

    }
    */

    public String getUsername(){
        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs.getString(Constants.USERNAME_KEY, "");
    }

    public String getPassword(){
        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs.getString(Constants.PASSWORD_KEY, "");
    }

    public void storeCredentials(String username, String password){

        SharedPreferences.Editor editor =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.putString(Constants.USERNAME_KEY, username);
        editor.putString(Constants.PASSWORD_KEY, password);
        editor.commit();

    }

    public boolean isFirstTime() {

        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs == null || !(prefs.contains(Constants.USERNAME_KEY) && prefs.contains(Constants.PASSWORD_KEY));

    }

    public static boolean isFirstTime(Context context){
        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        if(prefs == null) return true;

        return !(prefs.contains(Constants.USERNAME_KEY) && prefs.contains(Constants.PASSWORD_KEY));
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

        editor.putString(Constants.AUTH_TOKEN, token);
        editor.commit();
    }

    public String getAuthenticationToken(){
        return context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE)
                .getString(Constants.AUTH_TOKEN, "");
    }

    public void clearAuthenticationToken(){
        SharedPreferences.Editor editor =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.remove(Constants.AUTH_TOKEN);
        editor.commit();
    }
}
