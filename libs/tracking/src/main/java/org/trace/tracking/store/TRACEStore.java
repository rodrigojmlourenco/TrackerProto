package org.trace.tracking.store;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.trace.tracking.Constants;
import org.trace.tracking.store.auth.AuthenticationManager;
import org.trace.tracking.store.exceptions.AuthTokenIsExpiredException;
import org.trace.tracking.store.exceptions.InvalidAuthCredentialsException;
import org.trace.tracking.store.exceptions.LoginFailedException;
import org.trace.tracking.store.exceptions.RemoteTraceException;
import org.trace.tracking.store.exceptions.UnableToCloseSessionTokenExpiredException;
import org.trace.tracking.store.exceptions.UnableToPerformLogin;
import org.trace.tracking.store.exceptions.UnableToSubmitTrackTokenExpiredException;
import org.trace.tracking.store.remote.HttpClient;
import org.trace.tracking.tracker.storage.data.Track;

import java.math.BigInteger;
import java.security.SecureRandom;

//TODO: refractorização no tratamento de AuthTokenExpired
public class TRACEStore extends IntentService{

    private final String LOG_TAG = this.getClass().getSimpleName();


    private final AuthenticationManager authManager;


    private final HttpClient mHttpClient;

    public TRACEStore() {
        super("TRACEStore");
        this.authManager = new AuthenticationManager(this);
        this.mHttpClient = new HttpClient(this);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        intent.hasExtra(Constants.store.OPERATION_KEY);

        Operations op;
        try {
            op = Operations.valueOf(intent.getStringExtra(Constants.store.OPERATION_KEY));
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
                } catch (InvalidAuthCredentialsException e){
                    //TODO: broadcast something that enables the application to re-login
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
            username = intent.getStringExtra(Constants.store.USERNAME_KEY);
            password = intent.getStringExtra(Constants.store.PASSWORD_KEY);
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


        Intent loginI = new Intent(Constants.store.LOGIN_ACTION)
                .putExtra(Constants.store.SUCCESS_LOGIN_KEY, success)
                .putExtra(Constants.store.LOGIN_ERROR_MSG_KEY, error);

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
                Log.e(LOG_TAG, ""+e1.getMessage());
            }

        }finally {

            if(session.isEmpty()){ //Create fake session
                Log.i(LOG_TAG, "Unable to acquire session due to connectivity problems, proceeding with fake one...");
                SecureRandom random = new SecureRandom();
                session = "local_"+new BigInteger(130, random).toString(16);
                isValid = false;
            }

            Client.setSessionId(session, isValid);
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
        if(!intent.hasExtra(Constants.store.TRACK_EXTRA)) return;

        track = intent.getParcelableExtra(Constants.store.TRACK_EXTRA);

        /*
        //Check if the track has already been uploaded, proceed otherwise.
        if(!track.isLocalOnly()) {
            postUserFeedback("This track has already been uploaded.");
            return;
        }else
            postUserFeedback("Uploading track with session '"+track.getSessionId()+"'...");
        */


        try {
            if(mHttpClient.submitTrack(authManager.getAuthenticationToken(), track))
                postUserFeedback("Track successfully posted.");
            else
                postUserFeedback("Track was not successfully posted.");

        } catch (RemoteTraceException e) {
            Log.e(LOG_TAG, e.getMessage());
            postUserFeedback("Track was not posted because " + e.getMessage());

        } catch (UnableToSubmitTrackTokenExpiredException e){

            login(authManager.getUsername(), authManager.getPassword());

            try {

                if(mHttpClient.submitTrack(authManager.getAuthenticationToken(), track))
                    postUserFeedback("Track successfully posted.");
                else
                    postUserFeedback("Track was not successfully posted.");

            } catch (RemoteTraceException | UnableToSubmitTrackTokenExpiredException e1) {
                Log.e(LOG_TAG, e.getMessage());
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

    /* Supported Operations
    /* Supported Operations
    /* Supported Operations
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public enum Operations {
        login,
        logout,
        initiateSession,
        submitTrack,
        unknown
    }

    /**
     * This class operates as an abstraction layer over the communication between the application
     * and corresponding activities and the TRACEStore IntentService. All the methods provided are static.
     * <br>
     * <emph>Note: </emph> The employed design pattern greatly differs with the one employed in the TRACETracker.Client.
     */
    public static class Client {

        private static final String LOG_TAG = "TRACEStore";

        private static String sessionId;
        private static boolean isValid;

        protected static void setSessionId(String session){
            sessionId = session;
            isValid = true;
        }

        protected static void setSessionId(String session, boolean valid){
            sessionId = session;
            isValid = valid;
        }

        /**
         * Checks if the current session identifier is a valid one, i.e. one generated by the  TRACEStore
         * server.
         * @return True if the current session identifier is valid, false otherwise.
         */
        public static  boolean isValidSession(){
            return isValid;
        }

        /**
         * Returns the current session identifier. This session identifier should be used to uniquely
         * identify a traced track.
         * @return The session identifier.
         */
        public static String getSessionId(){
            return sessionId;
        }

        /**
         * The method initiates communication with the TraceStore service, as to perform login.
         * <br>
         * The results of this operation are then broadcasted under the Constants.LOGIN_ACTION action,
         * with two payloads Constants.SUCCESS_LOGIN_KEY and Constants.LOGIN_ERROR_MSG_KEY. The first
         * is a boolean denoting if the operation was successful, and the second corresponds to the
         * error message.
         *
         * @param context The current context
         * @param username The user's username
         * @param password The user's password
         */
        public static void requestLogin(Context context, String username, String password){
            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(Constants.store.OPERATION_KEY, Operations.login.toString());
            mI.putExtra(Constants.store.USERNAME_KEY, username);
            mI.putExtra(Constants.store.PASSWORD_KEY, password);
            context.startService(mI);
        }

        /**
         * Signs-outs the current user.
         * @param context The current context.
         */
        public static void requestLogout(Context context){
            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(Constants.store.OPERATION_KEY, Operations.logout.toString());
            context.startService(mI);
        }

        /**
         * Requests for a new tracking session to be initiated if possible.
         * @param context The current context.
         */
        public static void requestInitiateSession(Context context){
            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(Constants.store.OPERATION_KEY, Operations.initiateSession.toString());
            context.startService(mI);
        }

        /**
         * Checks if this is the user has ever performed login.
         * @param context The current context.
         * @return True if the user has never logged in, false otherwise.
         */
        public static boolean isFirstTime(Context context){
            return AuthenticationManager.isFirstTime(context);
        }

        /**
         * Uploads a complete track. It is important to note that the method was designed to handle
         * the possibility of the track being a local track, i.e. it is not associated with a valid
         * session identifier from the server's standpoint. Therefore, if the session identifier is
         * not a valid one, this method handles the track's session identifier renewal.
         * @param context The current context.
         * @param track The Track to be uploaded.
         * @see Track
         */
        public static void uploadWholeTrack(Context context, Track track){
            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(Constants.store.OPERATION_KEY, Operations.submitTrack.toString());
            mI.putExtra(Constants.store.TRACK_EXTRA, track);
            context.startService(mI);
        }
    }
}