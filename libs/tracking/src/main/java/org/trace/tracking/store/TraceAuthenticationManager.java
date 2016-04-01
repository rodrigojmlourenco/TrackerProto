package org.trace.tracking.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.trace.tracking.R;
import org.trace.tracking.TrackingConstants;
import org.trace.tracking.store.exceptions.InvalidAuthCredentialsException;
import org.trace.tracking.store.exceptions.LoginFailedException;
import org.trace.tracking.store.exceptions.MissingSignInApiException;
import org.trace.tracking.store.exceptions.NetworkConnectivityRequiredException;
import org.trace.tracking.store.exceptions.UnableToPerformLogin;
import org.trace.tracking.store.exceptions.UnsupportedIdentityProvider;
import org.trace.tracking.store.exceptions.UserIsNotLoggedException;
import org.trace.tracking.store.remote.HttpClient;

public class TraceAuthenticationManager {

    private final String TAG = "Auth";

    private Context mContext;
    private static TraceAuthenticationManager MANAGER = null;
    private HttpClient mHttpClient;
    private GrantType mCurrentGrantType = GrantType.none;
    private String mAuthenticationToken = null;

    private ConnectivityManager mConnectivityManager;

    public TraceAuthenticationManager(Context context, GoogleApiClient credentialsApiClient){
        mContext = context;
        mHttpClient = new HttpClient(context);
        mCredentialsApiClient = credentialsApiClient;
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static TraceAuthenticationManager getAuthenticationManager(Context context, GoogleApiClient credentialsApiClient){

        synchronized (TraceAuthenticationManager.class){
            if(MANAGER == null)
                MANAGER = new TraceAuthenticationManager(context, credentialsApiClient);
            else
                MANAGER.updateContext(context, credentialsApiClient);
        }

        return MANAGER;
    }

    private void updateContext(Context context, GoogleApiClient credentialsApiClient){
        this.mContext = context;
        this.mCredentialsApiClient = credentialsApiClient;
        this.mConnectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void login() {

        //Only attempt login if the network is connected
        boolean attemptLogin = isNetworkConnected();


        if(mCurrentCredential!=null) //a) Check if there is any active credential and use it to login
            login(mCurrentCredential);
        else //b) Attempt to login from one of the stored credentials
            retrieveCredentials(attemptLogin);
    }

    private void login(Credential credential){
        String accountType = credential.getAccountType();

        if(accountType == null) { //password-based accout, i.e. native trace
            login(credential.getId(), credential.getPassword());
        }else{
            switch (accountType){
                case IdentityProviders.GOOGLE:
                    performSilentGoogleLogin();
                    break;
                default:
                    throw new UnsupportedIdentityProvider(accountType);
            }
        }
    }

    public void logout(){
        switch (mCurrentGrantType){
            case trace:
                //TODO
            case google:
                //TODO
            case none:
            default:
                ;
        }

        clearAuthenticationToken();
    }

    /* Authentication Token Management
    /* Authentication Token Management
    /* Authentication Token Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private void storeAuthenticationToken(String token){
        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.putString(AUTH_TOKEN, token);
        editor.commit();
    }

    public String getAuthenticationToken() throws UserIsNotLoggedException {

        String authToken = mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE)
                .getString(AUTH_TOKEN, "");

        if(authToken.isEmpty())
            throw new UserIsNotLoggedException();
        else
            return authToken;
    }

    public void clearAuthenticationToken(){
        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE).edit();

        editor.remove(AUTH_TOKEN);
        editor.commit();
    }

    /* TRACE Native Login
    /* TRACE Native Login
    /* TRACE Native Login
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public void login(final String username, final String password) throws InvalidAuthCredentialsException {

        Log.i(TAG, "Native login as "+username);

        if(username == null || username.isEmpty()
                || password == null || password.isEmpty()) {
            Log.i(TAG, "The provided credentials are empty or null.");
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

                    Log.d(TAG, "Successfully logged in " + username + ", token is {" + authToken + "}");

                    storeTraceNativeCredentials(username, password);

                    success = true;

                } catch (UnableToPerformLogin | LoginFailedException e) {
                    e.printStackTrace();
                    error = e.getMessage();
                }catch ( InvalidAuthCredentialsException e){
                    removeCredential(mCurrentCredential);
                }

                mCurrentGrantType = GrantType.trace;

                //Broadcast the results of the login operation
                mContext.sendBroadcast(getFailedLoginIntent(success, error));
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

    public void login(final GoogleSignInAccount account, final GrantType type){

        Log.i(TAG, "Google login as "+account.getDisplayName());

        final String idToken = account.getIdToken();

        if(idToken == null || idToken.isEmpty()) {
            Log.i(TAG, "The provided credentials are empty or null.");
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

                    Log.d(TAG, "Successfully logged with grant " + type + ", token is {" + authToken + "}");

                    storeGoogleCredentials(account);

                    mCurrentGrantType = type;
                    success = true;

                } catch (UnableToPerformLogin | LoginFailedException | InvalidAuthCredentialsException e) {
                    e.printStackTrace();
                    error = e.getMessage();
                }

                //Broadcast the results of the login operation
                mContext.sendBroadcast(getFailedLoginIntent(success, error));
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

    /* SmartLock - Credential Storage
    /* SmartLock - Credential Storage
    /* SmartLock - Credential Storage
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private Credential mCurrentCredential;
    private GoogleApiClient mCredentialsApiClient;

    public static final int RC_SAVE     = 0;
    public static final int RC_LOAD     = 1;
    public static final int RC_SIGN_IN  = 2;
    public static final int RC_DELETE   = 3;

    private void storeTraceNativeCredentials(String username, String password){

        Credential credential = new Credential.Builder(username)
                                    .setPassword(password)
                                    .build();

        storeGenericCredential(credential);
    }

    private void storeGoogleCredentials(GoogleSignInAccount account){
        Credential credential = new Credential.Builder(account.getEmail())
                .setAccountType(IdentityProviders.GOOGLE)
                .setName(account.getDisplayName())
                .setProfilePictureUri(account.getPhotoUrl())
                .build();


        storeGenericCredential(credential);
    }

    private void storeGenericCredential(Credential credential){

        //Testing
        mCurrentCredential = credential;

        Auth.CredentialsApi.save(mCredentialsApiClient, credential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess())
                            Log.d(TAG, "SAVE: OK");
                        else {

                            if (status.hasResolution()) {
                                try {
                                    status.startResolutionForResult((Activity) mContext, RC_SAVE);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Log.e(TAG, "Failed to save the credentials");
                            }
                        }
                    }
                }
        );
    }

    /* Login Support
    /* Login Support
    /* Login Support
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private void retrieveCredentials(final boolean attemptLogin){
        final CredentialRequest mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE)
                .build();

        Auth.CredentialsApi.request(mCredentialsApiClient, mCredentialRequest).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult credentialRequestResult) {

                        if (credentialRequestResult.getStatus().isSuccess()) {
                            if (attemptLogin)
                                onCredentialRetrievedLogin(credentialRequestResult.getCredential());
                            else
                                mContext.sendBroadcast(getFailedLoginIntent());
                        } else {

                            Status status = credentialRequestResult.getStatus();

                            if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {

                                if (attemptLogin) {
                                    try {
                                        status.startResolutionForResult((Activity) mContext, RC_LOAD);
                                    } catch (IntentSender.SendIntentException e) {
                                        e.printStackTrace();
                                    }
                                } else
                                    mContext.sendBroadcast(getSuccessLoginIntent());

                            } else {
                                mContext.sendBroadcast(getFailedLoginIntent());
                            }


                        }
                    }
                }
        );
    }

    private void performSilentGoogleLogin(){
        OptionalPendingResult<GoogleSignInResult> opr =
                Auth.GoogleSignInApi.silentSignIn(mCredentialsApiClient);

        opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
            @Override
            public void onResult(GoogleSignInResult googleSignInResult) {
                login(googleSignInResult.getSignInAccount(), GrantType.google);
            }
        });
    }

    private void onCredentialRetrievedLogin(Credential credential){
        mCurrentCredential = credential;
        login(credential);
    }

    /* Credential Removal
    /* Credential Removal
    /* Credential Removal
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * Removes all stored credentials in the smart lock.
     * @throws NetworkConnectivityRequiredException This operation required connectivity in order to be performed.
     */
    public void removeAllStoredCredentials() throws NetworkConnectivityRequiredException {

        if(!isNetworkConnected())
            throw new NetworkConnectivityRequiredException();

        CredentialRequest mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE)
                .build();

        Auth.CredentialsApi.request(mCredentialsApiClient, mCredentialRequest).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult credentialRequestResult) {

                        if(credentialRequestResult.getStatus().isSuccess()) {
                            Log.i(TAG, "DELETE: found credential to remove.");
                            removeCredential(credentialRequestResult.getCredential());
                        }else {
                            Log.i(TAG, "DELETE: there are several credentials, choosing one...");

                            Status status = credentialRequestResult.getStatus();

                            if(status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED){

                                try {
                                    status.startResolutionForResult((Activity) mContext, RC_DELETE);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }

                            }else{
                                Log.e(TAG, "DELETE: found no credentials to remove.");
                            }
                        }
                    }
                }
        );
    }


    public void removeCredential(final Credential credential){
        Auth.CredentialsApi.delete(mCredentialsApiClient, credential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                        String accountType = credential.getAccountType() == null ? "unknown" : credential.getAccountType();

                        if (status.isSuccess()) {
                            Log.i(TAG, "Removed " + accountType);
                        } else
                            Log.e(TAG, "Did not remove " + accountType);
                    }
                }
        );
    }



    /*
     * When user input is required to select a credential, the getStatusCode() method returns
     * RESOLUTION_REQUIRED. In this case, call the status object's startResolutionForResult() method
     * to prompt the user to choose an account.
     */
    private void resolveCredentialResult(Status status, int code){

        if(status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED){

            try {
                status.startResolutionForResult((Activity) mContext, code);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }

        }else{
            mContext.sendBroadcast(getFailedLoginIntent());
        }
    }


    /* Others
    /* Others
    /* Others
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private boolean isNetworkConnected(){
        return mConnectivityManager.getActiveNetworkInfo() != null;
    }


    /* Helpers
    /* Helpers
    /* Helpers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public static GoogleSignInOptions getTraceGoogleSignOption(Context context){
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.trace_client_id))
                .requestEmail()
                .build();
    }

    private Intent getSuccessLoginIntent(){
        return new Intent(TrackingConstants.store.LOGIN_ACTION)
                .putExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA, true)
                .putExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA, "");
    }

    private Intent getFailedLoginIntent(){
        return new Intent(TrackingConstants.store.LOGIN_ACTION)
                .putExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA, false)
                .putExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA, "First time login");
    }

    private Intent getFailedLoginIntent(boolean success, String error){
        return new Intent(TrackingConstants.store.LOGIN_ACTION)
                .putExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA, success)
                .putExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA, error);
    }

    public enum GrantType {
        google,
        trace,
        none
    }
}
