package org.trace.tracking.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
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
import org.trace.tracking.store.exceptions.UnableToPerformLogin;
import org.trace.tracking.store.exceptions.UserIsNotLoggedException;
import org.trace.tracking.store.remote.HttpClient;

/**
 * Created by Rodrigo Lourenço on 29/03/2016.
 */
public class TraceAuthenticationManager {


    private final String TAG = "Auth";

    private Context mContext;
    private static TraceAuthenticationManager MANAGER = null;
    private HttpClient mHttpClient;
    private GrantType mCurrentGrantType = GrantType.none;
    private String mAuthenticationToken = null;

    private TraceAuthenticationManager(Context context, GoogleApiClient credentialsApiClient){
        mContext = context;
        mHttpClient = new HttpClient(context);
        mCredentialsApiClient = credentialsApiClient;
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

    public void login() {
        retrieveCredentialsAndLogin();
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
    public static final int RC_LOAD = 1;
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

    private boolean isFirstTime = true;


    private void retrieveCredentialsAndLogin(){

        CredentialRequest mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE)
                .build();

        Auth.CredentialsApi.request(mCredentialsApiClient, mCredentialRequest).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult credentialRequestResult) {

                        if(credentialRequestResult.getStatus().isSuccess()) {
                            isFirstTime = false;
                            onCredentialRetrievedLogin(credentialRequestResult.getCredential());
                        }else {
                            resolveCredentialResult(credentialRequestResult.getStatus(), RC_LOAD);
                        }
                    }
                }
        );
    }

    public void removeAllStoredCredentials(){
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
                            Log.e(TAG, "DELETE: found no credentials to remove.");
                            resolveCredentialResult(credentialRequestResult.getStatus(), RC_DELETE);
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
                            Log.i(TAG, "Removed "+accountType);
                        }else
                            Log.e(TAG, "Did not remove "+accountType);
                    }
                }
        );
    }

    private void onCredentialRetrievedLogin(Credential credential){

        String accountType = credential.getAccountType();

        if(accountType == null){ //Username password credential
            Log.i(TAG, "Its a TRACE account");
            mCurrentCredential = credential;
            login(credential.getId(), credential.getPassword());
            return;
        }

        switch (accountType){
            case IdentityProviders.GOOGLE:

                if(!mCredentialsApiClient.hasConnectedApi(Auth.GOOGLE_SIGN_IN_API))
                    throw new MissingSignInApiException();

                mCurrentCredential = credential;

                OptionalPendingResult<GoogleSignInResult> opr =
                        Auth.GoogleSignInApi.silentSignIn(mCredentialsApiClient);

                opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                    @Override
                    public void onResult(GoogleSignInResult googleSignInResult) {
                        login(googleSignInResult.getSignInAccount(), GrantType.google);
                    }
                });

                break;
            default:

                return;
        }
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

    public boolean isFirstTime() {

        /*
        SharedPreferences prefs =
                mContext.getSharedPreferences(AUTH_SETTINGS_KEY, Context.MODE_PRIVATE);

        return prefs == null || !(prefs.contains(TrackingConstants.store.USERNAME_KEY) && prefs.contains(TrackingConstants.store.PASSWORD_KEY));
        */
        return isFirstTime;

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

    public class TraceRequestHelper{

        private final String TAG = "Auth";

        public int onRequestResult(int requestCode, int resultCode, Intent data){
            if(resultCode != -1) {
                Log.e(TAG, "Failed at code " + requestCode);
                return -1;
            }

            switch (requestCode){
                case TraceAuthenticationManager.RC_SIGN_IN:
                    GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (result.isSuccess()) {
                        GoogleSignInAccount acct = result.getSignInAccount();
                        login(acct, GrantType.google);
                    }
                    break;
                case TraceAuthenticationManager.RC_LOAD:
                    OptionalPendingResult<GoogleSignInResult> opr =
                            Auth.GoogleSignInApi.silentSignIn(mCredentialsApiClient);

                    opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                        @Override
                        public void onResult(GoogleSignInResult googleSignInResult) {
                            login(googleSignInResult.getSignInAccount(), TraceAuthenticationManager.GrantType.google);
                        }
                    });
                    break;
                case TraceAuthenticationManager.RC_SAVE:
                    Log.d(TraceAuthenticationManager.this.TAG, "TODO: Save");
                    break;
                case TraceAuthenticationManager.RC_DELETE:
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    removeCredential(credential);
                    break;
                default:
                    Log.e(TraceAuthenticationManager.this.TAG, "Unknown request code "+requestCode);
            }

            return requestCode;
        }
    }

    public enum GrantType {
        google,
        trace,
        none
    }
}
