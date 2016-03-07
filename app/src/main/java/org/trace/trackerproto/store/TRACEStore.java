package org.trace.trackerproto.store;


import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.store.api.TRACEStoreOperations;
import org.trace.trackerproto.store.auth.AuthenticationManager;
import org.trace.trackerproto.store.exceptions.AuthTokenIsExpiredException;
import org.trace.trackerproto.store.exceptions.InvalidAuthCredentialsException;
import org.trace.trackerproto.store.exceptions.LoginFailedException;
import org.trace.trackerproto.store.exceptions.RemoteTraceException;
import org.trace.trackerproto.store.exceptions.UnableToPerformLogin;
import org.trace.trackerproto.tracking.data.Track;

import static us.monoid.web.Resty.content;

public class TRACEStore extends IntentService{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private final String BASE_URI = "http://146.193.41.50:8080/trace";


    private final AuthenticationManager authManager;


    private final HttpClient mHttpClient;

    public TRACEStore() {
        super("TRACEStore");
        this.authManager = new AuthenticationManager(this);
        this.mHttpClient = new HttpClient();
    }


    private void performSubmitTrack(Intent intent) {

        Track track;
        if(!intent.hasExtra(Constants.TRACK_EXTRA)) return;

        track = intent.getParcelableExtra(Constants.TRACK_EXTRA);

        try {
            mHttpClient.submitTrackAndCloseSession(authManager.getAuthenticationToken(), track);
        } catch (RemoteTraceException e) {
            e.printStackTrace();
        }
    }


    private boolean login(String username, String password) throws InvalidAuthCredentialsException {

        String authToken;

        try {

            authToken = mHttpClient.login(username, password);
            authManager.storeAuthenticationToken(authToken);

        } catch (LoginFailedException e) {
            e.printStackTrace();
        } catch (UnableToPerformLogin e) {
            e.printStackTrace();
            Log.e("LOGIN", "Unable to login... Generating local session");
            authToken = String.valueOf(Math.random());
            TRACEStoreApiClient.setSessionId(authToken);
            return true;
        }

            if(authManager.isFirstTime())
                authManager.storeCredentials(username, password);

            Log.i(LOG_TAG, "Login was successful");
            return  true;
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
        } catch (RemoteTraceException e) {
            e.printStackTrace();
        } catch (AuthTokenIsExpiredException e){
            e.printStackTrace();
        }
    }

    public void performInitiateSession(Intent intent){

        Log.d(LOG_TAG, "Attempting session acquisition...");

        String session;
        String authToken = authManager.getAuthenticationToken();

        try {
            session = mHttpClient.requestTrackingSession(authToken);
            TRACEStoreApiClient.setSessionId(session);
        } catch (RemoteTraceException e) {
            e.printStackTrace();
        }

    }

    public void performTerminateSession(Intent intent){
        //TODO
        throw new UnsupportedOperationException("performSubmitLocation");
    }

    private void performSubmitLocation(Intent intent){
        //TODO
        throw new UnsupportedOperationException("performSubmitLocation");
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
            case submitLocation:
                performSubmitLocation(intent);
                break;
            case submitTrack:
                performSubmitTrack(intent);
                break;
            case initiateSession:
                performInitiateSession(intent);
                break;
            case terminateSession:
                performTerminateSession(intent);
                break;
            default:
                Log.e(LOG_TAG, "Unknown operation "+op);
        }
    }


}
