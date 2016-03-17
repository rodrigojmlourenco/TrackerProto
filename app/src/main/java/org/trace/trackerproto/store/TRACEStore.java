package org.trace.trackerproto.store;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.store.api.TRACEStoreOperations;
import org.trace.trackerproto.store.auth.AuthenticationManager;
import org.trace.trackerproto.store.exceptions.AuthTokenIsExpiredException;
import org.trace.trackerproto.store.exceptions.InvalidAuthCredentialsException;
import org.trace.trackerproto.store.exceptions.LoginFailedException;
import org.trace.trackerproto.store.exceptions.RemoteTraceException;
import org.trace.trackerproto.store.exceptions.UnableToCloseSessionTokenExpiredException;
import org.trace.trackerproto.store.exceptions.UnableToPerformLogin;
import org.trace.trackerproto.store.exceptions.UnableToSubmitTrackTokenExpiredException;
import org.trace.trackerproto.tracking.storage.data.Track;

import java.math.BigInteger;
import java.security.SecureRandom;

import static us.monoid.web.Resty.content;

//TODO: refractorização no tratamento de AuthTokenExpired
public class TRACEStore extends IntentService{

    private final String LOG_TAG = this.getClass().getSimpleName();


    private final AuthenticationManager authManager;


    private final HttpClient mHttpClient;

    public TRACEStore() {
        super("TRACEStore");
        this.authManager = new AuthenticationManager(this);
        this.mHttpClient = new HttpClient();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        intent.hasExtra(Constants.OPERATION_KEY);

        TRACEStoreOperations op;
        try {
            op = TRACEStoreOperations.valueOf(intent.getStringExtra(Constants.OPERATION_KEY));
        }catch (NullPointerException e){
            Log.e(LOG_TAG, "Un-parseable operation");
            return;
        }

        switch (op){
            case login:
                performLogin(intent);
                break;
            case logout:
                performLogout(intent);
                break;
            case submitTrack:
                performSubmitTrack(intent);
                break;
            case initiateSession:
                try {
                    performInitiateSession(intent);
                } catch (RemoteTraceException e) {
                    e.printStackTrace();
                    postUserFeedback(e.getMessage());
                }
                break;
            default:
                Log.e(LOG_TAG, "Unknown operation "+op);
        }
    }

    /* Authentication Management
    /* Authentication Management
    /* Authentication Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private boolean login(String username, String password) throws InvalidAuthCredentialsException {

        String authToken;
        boolean success = false;

        try {

            authToken = mHttpClient.login(username, password);
            authManager.storeAuthenticationToken(authToken);
            Log.i(LOG_TAG, "Login was successful");
            success = true;

        } catch (LoginFailedException | UnableToPerformLogin e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        if(success && authManager.isFirstTime())
            authManager.storeCredentials(username, password);


        return success;
    }

    private void performLogin(Intent intent){

        String authToken = authManager.getAuthenticationToken();

        //Check if already logged in
        if(authToken!=null && !authToken.isEmpty()){
            Log.i(LOG_TAG, "AuthToken still holds, proceeding without requesting for a new one.");
            return;
        }

        String username, password, error ="";
        boolean isFirst = authManager.isFirstTime(), success = false;

        if(isFirst){
            username = intent.getStringExtra(Constants.USERNAME_KEY);
            password = intent.getStringExtra(Constants.PASSWORD_KEY);
        }else{
            username = authManager.getUsername();
            password = authManager.getPassword();
        }

        if(username == null || password == null || username.isEmpty() || password.isEmpty()) {

            if(isFirst)
                error =  "Failed because the provided credentials were null";
            else
                error = "Failed because the stored credentials were null";

            success = false;

        }else
            try {
                success = login(username, password);
            } catch (InvalidAuthCredentialsException e) {
                error = e.getMessage();
            }


        Intent loginI = new Intent(Constants.LOGIN_ACTION)
                .putExtra(Constants.SUCCESS_LOGIN_KEY, success)
                .putExtra(Constants.LOGIN_ERROR_MSG_KEY, error);

        sendBroadcast(loginI);

    }

    private void performLogout(Intent intent) {
        Log.i(LOG_TAG, "Logging out");

        String authToken = authManager.getAuthenticationToken();
        authManager.clearAuthenticationToken();

        try {
            mHttpClient.logout(authToken);
        } catch (RemoteTraceException | AuthTokenIsExpiredException e) {
            e.printStackTrace();
        }
    }

    /* Tracking Session Management
    /* Tracking Session Management
    /* Tracking Session Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public void performInitiateSession(Intent intent) throws RemoteTraceException {

        Log.d(LOG_TAG, "Attempting session acquisition...");

        String session = "";
        String authToken = authManager.getAuthenticationToken();
        boolean isValid = false;

        try {

            session = mHttpClient.requestTrackingSession(authToken);
            isValid = true;

        } catch (RemoteTraceException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (AuthTokenIsExpiredException e) {

            Log.i(LOG_TAG, "Session request failed due to expired authentication token, requesting new one...");

            try {
                login(authManager.getUsername(), authManager.getPassword());
                session = mHttpClient.requestTrackingSession(authToken);
                isValid = true;
            } catch (AuthTokenIsExpiredException e1) {
                Log.e(LOG_TAG, e1.getMessage());
            }

        }finally {

            if(session.isEmpty()){ //Create fake session
                Log.i(LOG_TAG, "Unable to acquire session due to connectivity problems, proceeding with fake one...");
                SecureRandom random = new SecureRandom();
                session = "local_"+new BigInteger(130, random).toString(16);
                isValid = true;
            }

            TRACEStoreApiClient.setSessionId(session, isValid);
        }
    }

    /* Information Uploading
    /* Information Uploading
    /* Information Uploading
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private void performSubmitTrack(Intent intent) {

        Track track;
        if(!intent.hasExtra(Constants.TRACK_EXTRA)) return;

        track = intent.getParcelableExtra(Constants.TRACK_EXTRA);

        //Check if the track has already been uploaded, proceed otherwise.
        if(!track.isLocalOnly()) {
            postUserFeedback("This track has already been uploaded.");
            return;
        }else
            postUserFeedback("Uploading track with session '"+track.getSessionId()+"'...");


        try {
            if(mHttpClient.submitTrackAndCloseSession(authManager.getAuthenticationToken(), track))
                postUserFeedback("Track successfully posted.");
            else
                postUserFeedback("Track was not successfully posted.");

        } catch (RemoteTraceException e) {
            Log.e(LOG_TAG, e.getMessage());
            postUserFeedback("Track was not posted because " + e.getMessage());

        } catch (UnableToSubmitTrackTokenExpiredException e){

            login(authManager.getUsername(), authManager.getPassword());

            try {

                if(mHttpClient.submitTrackAndCloseSession(authManager.getAuthenticationToken(), track))
                    postUserFeedback("Track successfully posted.");
                else
                    postUserFeedback("Track was not successfully posted.");

            } catch (RemoteTraceException | UnableToCloseSessionTokenExpiredException | UnableToSubmitTrackTokenExpiredException e1) {
                Log.e(LOG_TAG, e.getMessage());
                postUserFeedback("Track was not posted because " + e.getMessage());
            }

        } catch (UnableToCloseSessionTokenExpiredException e) {

            login(authManager.getUsername(), authManager.getPassword());

            try {
                mHttpClient.closeTrackingSession(authManager.getAuthenticationToken(), track.getSessionId());
                postUserFeedback("Track successfully posted.");
            } catch (RemoteTraceException | AuthTokenIsExpiredException e1) {
                Log.d(LOG_TAG, "Unable to close session '" + track.getSessionId() + "' due to " + e1.getMessage());
                postUserFeedback("Track was not posted because " + e.getMessage());
            }
        }
    }


    /* User Feedback
    /* User Feedback
    /* User Feedback
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private Handler mHandler = new Handler();

    private void postUserFeedback(String feedback){
        mHandler.post(new DisplayToast(this, feedback));
    }

    private class DisplayToast implements Runnable {

        private final Context mContext;
        private String message;

        public DisplayToast(Context context, String message){
            this.mContext = context;
            this.message  = message;
        }

        @Override
        public void run() {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    }
}