package org.trace.storeclient;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.trace.storeclient.cache.RouteCache;
import org.trace.storeclient.cache.exceptions.UnableToCreateRouteCopyException;
import org.trace.storeclient.data.Route;
import org.trace.storeclient.data.RouteSummary;
import org.trace.storeclient.exceptions.AuthTokenIsExpiredException;
import org.trace.storeclient.exceptions.AuthenticationTokenNotSetException;
import org.trace.storeclient.exceptions.InvalidEmailException;
import org.trace.storeclient.exceptions.InvalidPasswordException;
import org.trace.storeclient.exceptions.InvalidUsernameException;
import org.trace.storeclient.exceptions.NonMatchingPasswordsException;
import org.trace.storeclient.exceptions.RemoteTraceException;
import org.trace.storeclient.exceptions.UnableToRegisterUserException;
import org.trace.storeclient.exceptions.UnableToRequestGetException;
import org.trace.storeclient.exceptions.UnableToSubmitTrackTokenExpiredException;
import org.trace.storeclient.remote.HttpClient;
import org.trace.storeclient.remote.RewardHttpClient;
import org.trace.storeclient.remote.RouteHttpClient;
import org.trace.storeclient.cache.storage.RouteStorage;
import org.trace.storeclient.utils.FormFieldValidator;

import java.math.BigInteger;
import java.security.SecureRandom;

//TODO: Tratamento de AuthTokenExpired
public class TRACEStore extends IntentService{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private final HttpClient mHttpClient;
    private RouteCache mCache;
    private RouteStorage mLocalRouteStorage;

    public TRACEStore() {
        super("TRACEStore");
        this.mHttpClient = new HttpClient(this);
        mCache = RouteCache.getCacheInstance(this);
        mLocalRouteStorage = RouteStorage.getStorageInstance(this);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        intent.hasExtra(StoreClientConstants.OPERATION_KEY);

        Operations op;
        try {
            op = Operations.valueOf(intent.getStringExtra(StoreClientConstants.OPERATION_KEY));
        }catch (NullPointerException e){
            Log.e(LOG_TAG, "Un-parseable operation");
            return;
        }

        switch (op){
            case registerUser:
                performUserRegistry(intent);
                break;
            case submitTrack:
                performSubmitTrack(intent);
                break;
            case submitRoute:
                performSubmitRoute(intent);
                break;
            case uploadTrackSummary:
                performUploadRoute(intent);
                break;
            case initiateSession:

                try {
                    performInitiateSession(intent);
                } catch (RemoteTraceException e) {
                    e.printStackTrace();
                }

                break;
            case fetchShopsWithRewards:

                try {
                    JsonArray response = performFetchShopsWithRewards(intent);
                    broadcastResult(Operations.fetchShopsWithRewards, String.valueOf(response));
                } catch (UnableToRequestGetException e) {
                    e.printStackTrace();
                    broadcastFailedOperation(Operations.fetchShopsWithRewards, false, -1, e.getMessage());
                }catch(RemoteTraceException e){
                    e.printStackTrace();
                    broadcastFailedOperation(Operations.fetchShopsWithRewards, false, e.getErrorCode(), e.getErrorMessage());
                }

                break;
            default:
                Log.e(LOG_TAG, "Unknown operation "+op);
        }
    }

    private void performSubmitRoute(Intent intent){
        String authToken = extractAuthenticationToken(intent);
        Route route = intent.getParcelableExtra(StoreClientConstants.TRACK_EXTRA);


        try {
            mCache.saveRoute(authToken, route);
        } catch (UnableToCreateRouteCopyException e) {
            e.printStackTrace();
        }

    }

    //TODO: deal with possible fails, and relevant fails by removing the session from the server
    private void performUploadRoute(Intent intent) {


        RouteHttpClient httpClient = new RouteHttpClient();

        String authToken = extractAuthenticationToken(intent);
        String track = intent.getStringExtra(StoreClientConstants.TRACK_EXTRA);
        String trace = intent.getStringExtra(StoreClientConstants.TRACE_EXTRA);

        Route r = null;
        String session = null;
        RouteSummary rs = null;
        JsonParser parser = new JsonParser();
        boolean success = true;

        //Step 1 - The Route is stored in the local cache

        //Step 1 - Upload the Route Summary
        try {

            rs = httpClient.submitRouteSummary(authToken, track);
            session = rs.getSession();

        } catch (RemoteTraceException | AuthTokenIsExpiredException e) {
            e.printStackTrace();
            success = false;

        } finally {
            if(!success)
                return;
        }

        //Step 2  - Upload the Route's trace (150 at each time)
        //Step 2a - Split the trace into batches of up to MAX_SIZE points
        int MAX_SIZE = 15;
        int size = 1, excess;
        JsonArray[] queue;
        JsonArray jTrace = (JsonArray) parser.parse(trace);

        if(jTrace.size() <= MAX_SIZE){
            queue = new JsonArray[size];
            queue[0] = jTrace;
        }else{

            excess = jTrace.size() % MAX_SIZE;
            size = (jTrace.size() / MAX_SIZE) + (excess == 0 ? 0 : 1 );

            queue = new JsonArray[size];

            for(int i=0, left=0, right=MAX_SIZE; i < size; i++){

                JsonArray batch = new JsonArray();

                for(int j = left; j < right && j < jTrace.size() ; j++){
                    batch.add(jTrace.get(j));
                }

                left   = right++;
                right += MAX_SIZE;
                queue[i] = batch;
            }
        }

        //Step 2b - Upload each of the batches and analyze the response
        // NOTE: Only if all batches are uploaded successfully with the track summary be stored.
        success = true;
        for(JsonArray batch : queue){
            try {
                httpClient.uploadTraceBatch(authToken, session, batch);
            } catch (RemoteTraceException e) {
                e.printStackTrace();
            } catch (AuthTokenIsExpiredException e) {
                e.printStackTrace();
            }
        }

        //Step 3 - Request the map-matched values

    }

    private JsonArray performFetchShopsWithRewards(Intent intent) throws RemoteTraceException, UnableToRequestGetException {
        double latitude, longitude, radius;

        latitude = intent.getDoubleExtra(StoreClientConstants.LATITUDE, 0);
        longitude= intent.getDoubleExtra(StoreClientConstants.LONGITUDE, 0);
        radius   = intent.getDoubleExtra(StoreClientConstants.RADIUS, 0);

        RewardHttpClient httpClient = new RewardHttpClient();
        return httpClient.fetchShopsWithRewards(latitude, longitude, radius);
    }



    /* Tracking Session Management
    /* Tracking Session Management
    /* Tracking Session Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private String extractAuthenticationToken(Intent data){
        if(!data.hasExtra(StoreClientConstants.AUTH_TOKEN_EXTRA))
            throw new AuthenticationTokenNotSetException();

        return data.getStringExtra(StoreClientConstants.AUTH_TOKEN_EXTRA);
    }

    public void performInitiateSession(Intent intent) throws RemoteTraceException {

        Log.d(LOG_TAG, "Attempting session acquisition...");

        String session = "";
        String authToken = extractAuthenticationToken(intent);
        boolean isValid = false;

        try {

            session = mHttpClient.requestTrackingSession(authToken);
            isValid = true;

        } catch (RemoteTraceException e) {
            Log.e(LOG_TAG, e.getMessage());
        }catch(AuthTokenIsExpiredException e){
            Log.e(LOG_TAG, "Authentication token has expired");
            //TODO: tento renovar logo o token o desisto... e logo tento no upload?
        } finally {

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

    private void broadcastFailedOperation(Operations op, boolean success, int errorCode, String errorMsg) {
        Intent failedOperation = new Intent(StoreClientConstants.FAILED_OPERATION);
        failedOperation.putExtra(StoreClientConstants.OPERATION_EXTRA, op);
        failedOperation.putExtra(StoreClientConstants.response.SUCCESS, success);
        failedOperation.putExtra(StoreClientConstants.response.ERROR_CODE, errorCode);
        failedOperation.putExtra(StoreClientConstants.response.ERROR_MSG, errorMsg);
        sendBroadcast(failedOperation);
    }

    private void broadcastResult(Operations op, String payload) {
        Intent failedOperation = new Intent(StoreClientConstants.FAILED_OPERATION);
        failedOperation.putExtra(StoreClientConstants.OPERATION_EXTRA, op);
        failedOperation.putExtra(StoreClientConstants.response.SUCCESS, true);
        failedOperation.putExtra(StoreClientConstants.response.PAYLOAD, payload);
        sendBroadcast(failedOperation);
    }

    private void performUserRegistry(Intent intent) {

        JsonObject userRegistryRequest;
        JsonParser parser = new JsonParser();

        if(!intent.hasExtra(StoreClientConstants.USER_REQUEST_EXTRA)){
            return;
        }

        userRegistryRequest =
                (JsonObject) parser.parse(intent.getStringExtra(StoreClientConstants.USER_REQUEST_EXTRA));

        try {

            mHttpClient.registerUser(userRegistryRequest);
            broadcastFailedOperation(Operations.registerUser, true, 0, "");
            Log.d(LOG_TAG, "user successfully registered.");

        } catch (UnableToRegisterUserException e) {
            e.printStackTrace();
            broadcastFailedOperation(Operations.registerUser, false, e.getErrorCode(), e.getMessage());
        }
    }


    private void performSubmitTrack(Intent intent) {
        JsonObject track;
        JsonParser parser = new JsonParser();
        String authToken = extractAuthenticationToken(intent);
        if(!intent.hasExtra(StoreClientConstants.TRACK_EXTRA)) return;

        track = (JsonObject) parser.parse(intent.getStringExtra(StoreClientConstants.TRACK_EXTRA));

        try {
            if(mHttpClient.submitTrack(authToken, track))
                postUserFeedback("Track successfully posted.");
            else
                postUserFeedback("Track was not successfully posted.");

        } catch (RemoteTraceException e) {
            Log.e(LOG_TAG, e.getMessage());
            postUserFeedback("Track was not posted because " + e.getMessage());

        } catch (UnableToSubmitTrackTokenExpiredException e){
            //TODO: Token expired broadcast that information
            Log.e(LOG_TAG, "Token expired");
            broadcastFailedSubmitOperation(track.toString());
        }
    }



    private void broadcastFailedSubmitOperation(String track) {
        //Intent i = AuthenticationRenewalListener.getFailedToSubmitTrackIntent(track);
        //sendBroadcast(i);
        Log.e(LOG_TAG, "Not doing anything in @broadcastFailedSubmitOperation!");
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
        registerUser,
        initiateSession,
        submitTrack,
        submitRoute,
        uploadTrackSummary,
        fetchShopsWithRewards,
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
         * Requests for a new tracking session to be initiated if possible.
         * @param context The current context.
         */
        /*
        public static void requestInitiateSession(Context context, String authToken){
            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(StoreClientConstants.store.OPERATION_KEY, Operations.initiateSession.toString());
            mI.putExtra(StoreClientConstants.store.AUTH_TOKEN_EXTRA, authToken);
            //context.startService(mI);
        }
        */

        /**
         * Uploads a complete track. It is important to note that the method was designed to handle
         * the possibility of the track being a local track, i.e. it is not associated with a valid
         * session identifier from the server's standpoint. Therefore, if the session identifier is
         * not a valid one, this method handles the track's session identifier renewal.
         * @param context The current context.
         * @param track The Track to be uploaded.
         */
        public static void uploadTrack(Context context, String authToken, JsonObject track){

            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(StoreClientConstants.OPERATION_KEY, Operations.submitTrack.toString());
            mI.putExtra(StoreClientConstants.TRACK_EXTRA, track.toString());
            mI.putExtra(StoreClientConstants.AUTH_TOKEN_EXTRA, authToken);
            context.startService(mI);
        }


        public static void uploadTrack(Context context, String authToken, String session, String track){
            //track.setSessionId(session);
            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(StoreClientConstants.OPERATION_KEY, Operations.submitTrack.toString());
            mI.putExtra(StoreClientConstants.TRACK_EXTRA, track);
            mI.putExtra(StoreClientConstants.AUTH_TOKEN_EXTRA, authToken);
            context.startService(mI);
        }

        /**
         * TODO: document
         * @param context
         * @param latitude
         * @param longitude
         * @param radius
         * @return
         */
        public static void fetchShopsWithRewards(Context context, double latitude, double longitude, double radius){
            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(StoreClientConstants.OPERATION_KEY, Operations.fetchShopsWithRewards.toString());
            mI.putExtra(StoreClientConstants.LATITUDE, latitude);
            mI.putExtra(StoreClientConstants.LONGITUDE, longitude);
            mI.putExtra(StoreClientConstants.RADIUS, radius);
            context.startService(mI);
        }

        public static void registerUser(Context context, String username, String email, String password, String confirmPassword)
                throws InvalidUsernameException, InvalidEmailException, InvalidPasswordException, NonMatchingPasswordsException {

            if(!FormFieldValidator.isValidUsername(username))
                throw new InvalidUsernameException();

            if(!FormFieldValidator.isValidEmail(email))
                throw new InvalidEmailException();

            if(!FormFieldValidator.isValidPassword(password))
                throw new InvalidPasswordException();

            if(!password.equals(confirmPassword))
                throw new NonMatchingPasswordsException();

            JsonObject userRegistryRequest = new JsonObject();
            userRegistryRequest.addProperty(StoreClientConstants.register_user.USERNAME, username);
            userRegistryRequest.addProperty(StoreClientConstants.register_user.EMAIL, email);
            userRegistryRequest.addProperty(StoreClientConstants.register_user.PASSWORD, password);
            userRegistryRequest.addProperty(StoreClientConstants.register_user.CONFIRMATION_PASSWORD, confirmPassword);

            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(StoreClientConstants.OPERATION_KEY, Operations.registerUser.toString());
            mI.putExtra(StoreClientConstants.USER_REQUEST_EXTRA, userRegistryRequest.toString());
            context.startService(mI);
        }

        public static void log(String log){
            Log.d(LOG_TAG, log);
        }

        /*
         * Version 2.0 Ijsberg
         */


        public static void uploadTrackSummary(Context context, String authToken, JsonObject trackSummary, JsonArray trace){

            Intent mI = new Intent(context, TRACEStore.class);
            mI.putExtra(StoreClientConstants.OPERATION_KEY, Operations.uploadTrackSummary.toString());
            mI.putExtra(StoreClientConstants.TRACK_EXTRA, trackSummary.toString());
            mI.putExtra(StoreClientConstants.TRACE_EXTRA, trace.toString());
            mI.putExtra(StoreClientConstants.AUTH_TOKEN_EXTRA, authToken);
            context.startService(mI);

        }

        public static void submitRoute(Context context, String authToken, Route route){

            Intent mI = new Intent(context, TRACEStore.class);

            mI.putExtra(StoreClientConstants.OPERATION_KEY, Operations.submitRoute.toString());
            mI.putExtra(StoreClientConstants.AUTH_TOKEN_EXTRA, authToken);
            mI.putExtra(StoreClientConstants.TRACK_EXTRA, route);

            context.startService(mI);

        }
    }
}