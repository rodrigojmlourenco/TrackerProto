package org.trace.tracking.store;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.trace.tracking.TrackingConstants;
import org.trace.tracking.store.exceptions.FirstTimeLoginException;
import org.trace.tracking.store.exceptions.InvalidAuthCredentialsException;
import org.trace.tracking.store.exceptions.LoginFailedException;
import org.trace.tracking.store.exceptions.UnableToPerformLogin;
import org.trace.tracking.store.exceptions.UserIsNotLoggedException;
import org.trace.tracking.store.remote.HttpClient;

/**
 * Created by Rodrigo Lourenço on 29/03/2016.
 */
public class TraceAuthenticationManager{

    private final String LOG_TAG = "Auth";

    private Context mContext;
    private AccountManager mAccountManager;

    public static final String TRACE_ACCOUNT_TYPE = "trace";

    private static TraceAuthenticationManager MANAGER = null;

    private HttpClient mHttpClient;

    private GrantType mCurrentGrantType = GrantType.none;

    private String mAuthenticationToken = null;

    private TraceAuthenticationManager(Context context){
        mContext = context;
        mAccountManager = AccountManager.get(context);
        mHttpClient = new HttpClient(context);
    }

    public static TraceAuthenticationManager getAuthenticationManager(Context context){

        synchronized (TraceAuthenticationManager.class){
            if(MANAGER == null)
                MANAGER = new TraceAuthenticationManager(context);
        }

        return MANAGER;
    }


    public String getAuthenticationToken() throws UserIsNotLoggedException {

        String authToken = mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE)
                            .getString(AUTH_TOKEN, "");

        if(authToken.isEmpty())
            throw new UserIsNotLoggedException();
        else
            return authToken;
    }

    public boolean isAuthenticated(){
        return mAuthenticationToken != null;
    }

    public void logout(){
        switch (mCurrentGrantType){
            case trace:
                //TODO
            case google:
                //TODO
            case none:
            default:
                throw new UnsupportedOperationException();
        }

    }

    /* TRACE Native Login
    /* TRACE Native Login
    /* TRACE Native Login
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public void login() throws FirstTimeLoginException {
        if(!isFirstTime())
            login(getUsername(), getPassword());
        else
            throw new FirstTimeLoginException();
    }

    public void login(final String username, final String password) throws InvalidAuthCredentialsException {

        if(isAuthenticated()){
            Log.i(LOG_TAG, "Current authentication still holds, proceeding without logging in.");
            return;
        }

        if(username == null || username.isEmpty()
                || password == null || password.isEmpty()) {
            Log.i(LOG_TAG, "The provided credentials are empty or null.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                boolean success = false;
                String authToken, error = "";

                try {

                    authToken = mHttpClient.login(username, password);

                    mAuthenticationToken = authToken;
                    storeAuthenticationToken(authToken);

                    Log.d(LOG_TAG, "Successfully logged in " + username + ", token is {" + authToken + "}");

                    if(isFirstTime())
                        storeCredentials(username, password);

                    success = true;

                } catch (UnableToPerformLogin | LoginFailedException | InvalidAuthCredentialsException e) {
                    e.printStackTrace();
                    error = e.getMessage();
                }

                mCurrentGrantType = GrantType.trace;

                //Broadcast the results of the login operation
                mContext.sendBroadcast(new Intent(TrackingConstants.store.LOGIN_ACTION)
                            .putExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA, success)
                            .putExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA, error));
            }
        }).start();

    }

    /* Google Federated Login
    /* Google Federated Login
    /* Google Federated Login
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public void login(final String idToken, final GrantType type){
        if(isAuthenticated()){
            Log.i(LOG_TAG, "Current authentication still holds, proceeding without logging in.");
            return;
        }

        if(idToken == null || idToken.isEmpty()) {
            Log.i(LOG_TAG, "The provided credentials are empty or null.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                boolean success = false;
                String authToken, error = "";

                try {

                    authToken = mHttpClient.federatedLogin(idToken);

                    mAuthenticationToken = authToken;
                    storeAuthenticationToken(authToken);

                    Log.d(LOG_TAG, "Successfully logged with grant " + type + ", token is {" + authToken + "}");

                    if(isFirstTime())
                        ;//storeCredentials(username, password);

                    mCurrentGrantType = type;
                    success = true;

                } catch (UnableToPerformLogin | LoginFailedException | InvalidAuthCredentialsException e) {
                    e.printStackTrace();
                    error = e.getMessage();
                }

                //Broadcast the results of the login operation
                mContext.sendBroadcast(new Intent(TrackingConstants.store.LOGIN_ACTION)
                        .putExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA, success)
                        .putExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA, error));
            }
        }).start();
    }


    /* Credential Storage
    /* Credential Storage
    /* Credential Storage
    /* TODO: THIS IS NOT SECURE!! Use Android recommended APIs
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public static final String AUTH_TOKEN = "auth_token";
    private static final String AUTH_SETTINGS_KEY = "auth_settings";

    public String getUsername(){
        SharedPreferences prefs =
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs.getString(TrackingConstants.store.USERNAME_KEY, "");
    }

    public String getPassword(){
        SharedPreferences prefs =
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs.getString(TrackingConstants.store.PASSWORD_KEY, "");
    }

    public void storeCredentials(String username, String password){

        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.putString(TrackingConstants.store.USERNAME_KEY, username);
        editor.putString(TrackingConstants.store.PASSWORD_KEY, password);
        editor.commit();

    }

    public boolean isFirstTime() {

        SharedPreferences prefs =
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs == null || !(prefs.contains(TrackingConstants.store.USERNAME_KEY) && prefs.contains(TrackingConstants.store.PASSWORD_KEY));

    }

    public static boolean isFirstTime(Context context){
        SharedPreferences prefs =
                context.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        if(prefs == null) return true;

        return !(prefs.contains(TrackingConstants.store.USERNAME_KEY) && prefs.contains(TrackingConstants.store.PASSWORD_KEY));
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
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.putString(AUTH_TOKEN, token);
        editor.commit();
    }

    public void clearAuthenticationToken(){
        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.remove(AUTH_TOKEN);
        editor.commit();
    }



    public static enum GrantType {
        google,
        trace,
        none
    }
}
