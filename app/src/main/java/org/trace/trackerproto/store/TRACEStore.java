package org.trace.trackerproto.store;



import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.store.api.TRACEStoreOperations;
import org.trace.trackerproto.store.auth.AuthenticationManager;
import org.trace.trackerproto.store.exceptions.InvalidAuthCredentialsException;
import org.trace.trackerproto.tracking.data.SerializableLocation;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.storage.TrackInternalStorage;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToLoadStoredTrackException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import us.monoid.json.JSONException;
import us.monoid.web.Resty;

import static us.monoid.web.Resty.content;
import static us.monoid.web.Resty.form;

public class TRACEStore extends IntentService{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private final String BASE_URI = "http://146.193.41.50:8080/trace";

    private final Resty resty = new Resty();
    private final AuthenticationManager manager;

    private String sessionId = "";


    public TRACEStore() {
        super("TRACEStore");
        this.manager = new AuthenticationManager(this);
    }


    public void submitLocation(String sessionId, String data){

        String url = BASE_URI+"/tracker/put/geo/"+sessionId;

        try {
            resty.text(url, content(new us.monoid.json.JSONObject(data)));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    private String getGeoJsonLocation(SerializableLocation location){
        Gson gson = new Gson();
        JsonObject geoJson = new JsonObject();
        geoJson.addProperty("latitude", location.getLatitude());
        geoJson.addProperty("longitude", location.getLongitude());
        geoJson.addProperty("timestamp", location.getTimestamp());
        return gson.toJson(geoJson);
    }

    private void performSubmitTrack(Intent intent) {

        String url = BASE_URI+"/tracker/put/geo/";
        Track t;
        String sessionId;

        if(!intent.hasExtra(Constants.TRACK_EXTRA)) return;

        t = intent.getParcelableExtra(Constants.TRACK_EXTRA);
        sessionId = t.getSessionId();
        url+=sessionId;

        Log.d(LOG_TAG, "performSubmitTrack with session "+sessionId);

        try {

            for(SerializableLocation location : t.getTracedTrack()){
                String data = getGeoJsonLocation(location);
                resty.text(url, content(new us.monoid.json.JSONObject(data)));
            }

            //Toast.makeText(this, sessionId+" session uploaded.", Toast.LENGTH_SHORT).show();

        } catch (JSONException | IOException e) {
            e.printStackTrace();
            //Toast.makeText(this, "Unable to upload "+sessionId+".", Toast.LENGTH_SHORT).show();

        }

    }


    private boolean login(String username, String password) throws InvalidAuthCredentialsException {

        String url;

        try {
            username = URLEncoder.encode(username,"UTF-8");
            password = URLEncoder.encode(password,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        url = BASE_URI+"/auth/login?username="+username+"&password="+password;

        String tmp;
        try {

            tmp = resty.text(url, form()).toString();

            if(tmp.equals(Constants.TRACEService.FAILED_LOGIN_KEY)) {
                throw new InvalidAuthCredentialsException();
            }else
                sessionId = tmp;

            TRACEStoreApiClient.setSessionId(sessionId);

            Log.i(LOG_TAG, "Starting session "+sessionId);
            return  true;

        } catch (IOException e) {
            //e.printStackTrace();
            //return false;
            //TODO: for testing purposes only!!! Must be remover after ports are unlocked
            Log.e("LOGIN", "Unable to login... Generating local session");
            sessionId = String.valueOf(Math.random());
            TRACEStoreApiClient.setSessionId(sessionId);
            return true;
        }

    }

    private void performLogin(Intent intent){

        Log.d(LOG_TAG, "performLogin");


        String username, password, error ="";
        boolean isFirst = manager.isFirstTime(), success = false;

        if(isFirst){
            username = intent.getStringExtra(Constants.USERNAME_KEY);
            password = intent.getStringExtra(Constants.PASSWORD_KEY);
        }else{
            username = manager.getUsername();
            password = manager.getPassword();
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


        if (sessionId != null && !sessionId.isEmpty() && isFirst)
            manager.storeCredentials(username, password);

    }

    public void performInitiateSession(Intent intent){

        Log.d(LOG_TAG, "Attempting session acquisition...");

        boolean isFirst = manager.isFirstTime();

        if(isFirst) {
            Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                    .putExtra(Constants.FIRST_TIME_BROADCAST, true);

            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }else
            login(manager.getUsername(), manager.getPassword());

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
