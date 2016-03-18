package org.trace.tracking.store;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.trace.tracking.Constants;
import org.trace.tracking.store.exceptions.AuthTokenIsExpiredException;
import org.trace.tracking.store.exceptions.InvalidAuthCredentialsException;
import org.trace.tracking.store.exceptions.LoginFailedException;
import org.trace.tracking.store.exceptions.RemoteTraceException;
import org.trace.tracking.store.exceptions.UnableToCloseSessionTokenExpiredException;
import org.trace.tracking.store.exceptions.UnableToPerformLogin;
import org.trace.tracking.store.exceptions.UnableToRequestPostException;
import org.trace.tracking.store.exceptions.UnableToSubmitTrackTokenExpiredException;
import org.trace.tracking.storage.PersistentTrackStorage;
import org.trace.tracking.storage.data.TraceLocation;
import org.trace.tracking.storage.data.Track;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpClient {

    private final String LOG_TAG = HttpClient.class.getSimpleName();

    private JsonParser jsonParser;
    private final String BASE_URI = "http://146.193.41.50:8080/trace";

    private Context mContext;

    public HttpClient(Context context){
        this.mContext = context;
        jsonParser = new JsonParser();
    }

    private String extractAuthToken(String response) throws LoginFailedException {
        JsonObject jsonResponse = (JsonObject) jsonParser.parse(response);

        if(jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean())
            return jsonResponse.get("token").getAsString();
        else {

            int errorCode = jsonResponse.get("code").getAsInt();

            if(errorCode == 2)
                throw new InvalidAuthCredentialsException();

            String errorMessage = jsonResponse.get("error").getAsString();
            throw new LoginFailedException(errorMessage);
        }
    }

    private String validateAndExtractSession(String response) throws RemoteTraceException {
        JsonObject jsonResponse = (JsonObject) jsonParser.parse(response);

        if(jsonResponse.get("success").getAsBoolean()){
            return jsonResponse.get("session").getAsString();
        }else{
            throw new RemoteTraceException("GetSession", jsonResponse.get("error").getAsString());
        }
    }

    private String constructTraceTrack(List<TraceLocation> locations){
        JsonObject traceTrack = new JsonObject();
        JsonArray track = new JsonArray();

        for(TraceLocation location : locations)
            track.add(location.getSerializableLocationAsJson());

        traceTrack.add("track", track);
        return traceTrack.toString();
    }

    private String performPostRequest(String urlEndpoint, JsonObject requestProperties, String data) throws UnableToRequestPostException, AuthTokenIsExpiredException {
        URL url;
        HttpURLConnection connection = null;
        String dataUrl = BASE_URI+urlEndpoint;

        try {

            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            if(requestProperties != null)
                for(Map.Entry<String, JsonElement> entry : requestProperties.entrySet())
                    connection.setRequestProperty(entry.getKey(), entry.getValue().getAsString());

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            if(data != null && !data.isEmpty()) wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();

            // Get Response
            int responseCode = connection.getResponseCode();

            switch (responseCode){
                case 200:
                    break;
                case 401:
                    throw new AuthTokenIsExpiredException();
                default:
                    throw new UnableToRequestPostException(String.valueOf(responseCode));
            }

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }

            rd.close();


            return response.toString();

        } catch (IOException e) {

            e.printStackTrace();
            throw new UnableToRequestPostException(e.getMessage());

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

    }

    private void validateHttpResponse(String requestType, String response) throws RemoteTraceException {

        JsonObject jsonResponse = (JsonObject) jsonParser.parse(response);

        if(!jsonResponse.get("success").getAsBoolean()){
            throw new RemoteTraceException(requestType, jsonResponse.get("error").getAsString());
        }

    }


    public String login(String username, String password) throws UnableToPerformLogin, LoginFailedException {

        String urlEndpoint ="/auth/login";
        JsonObject requestProperties = new JsonObject();
        String dataUrlParameters = "username="+username+"&password="+password;

        requestProperties.addProperty(Constants.http.CONTENT_TYPE, "application/x-www-form-urlencoded");
        requestProperties.addProperty(Constants.http.CONTENT_LENGTH, Integer.toString(dataUrlParameters.getBytes().length));
        requestProperties.addProperty(Constants.http.CONTENT_LANGUAGE, "en-US,en,pt");

        try {
            String response = performPostRequest(urlEndpoint, requestProperties, dataUrlParameters);
            String authToken= extractAuthToken(response);
            Log.d(LOG_TAG, "Login successful with { authToken: '" + authToken + "'}");
            return authToken;
        } catch (UnableToRequestPostException | AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw new UnableToPerformLogin();
        }
    }

    public void logout(String authToken) throws RemoteTraceException, AuthTokenIsExpiredException {

        String urlEndpoint = "/auth/logout";
        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(Constants.http.AUTHORIZATION, "Bearer " + authToken);

        try {

            performPostRequest(urlEndpoint, requestProperties, null);
            Log.d(LOG_TAG, "Logout successful");

        } catch (UnableToRequestPostException |AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw new RemoteTraceException("Logout", e.getMessage());
        }

    }


    public String requestTrackingSession(String authToken) throws RemoteTraceException, AuthTokenIsExpiredException {

        String urlEndpoint = "/auth/session/open";
        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(Constants.http.AUTHORIZATION, "Bearer " + authToken);

        try {
            String response = performPostRequest(urlEndpoint, requestProperties, null);
            return validateAndExtractSession(response);
        } catch (UnableToRequestPostException e) {
            e.printStackTrace();
            throw new RemoteTraceException("GetSession", e.getMessage());
        }
    }


    public void closeTrackingSession(String authToken, String session) throws RemoteTraceException, AuthTokenIsExpiredException {

        String urlEndpoint = "/auth/close";
        String dataUrlParams = "session="+session;
        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(Constants.http.AUTHORIZATION, "Bearer "+authToken);

        try {
            String response = performPostRequest(urlEndpoint, requestProperties, dataUrlParams);
            validateHttpResponse("CloseSession", response);
        } catch (UnableToRequestPostException e) {
            e.printStackTrace();
            throw new RemoteTraceException("CloseSession", e.getMessage());
        }
    }

    private void uploadLocationSequence(String authToken, String session, List<TraceLocation> locations)
            throws RemoteTraceException, AuthTokenIsExpiredException {

        String data = constructTraceTrack(locations);
        String urlEndpoint = "/tracker/put/track/"+session;

        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(Constants.http.AUTHORIZATION, "Bearer "+authToken);
        requestProperties.addProperty(Constants.http.CONTENT_TYPE, "application/json; charset=UTF-8");

        try {
            String response = performPostRequest(urlEndpoint, requestProperties, data);
            validateHttpResponse("UploadTrack", response);
        } catch (UnableToRequestPostException e) {
            e.printStackTrace();
            throw new RemoteTraceException("UploadTrack", e.getMessage());
        }
    }

    public boolean submitTrackAndCloseSession(String authToken, Track track)
            throws RemoteTraceException, UnableToCloseSessionTokenExpiredException, UnableToSubmitTrackTokenExpiredException {



        String session, localSession;

        PersistentTrackStorage storage = new PersistentTrackStorage(mContext);


        if(!track.isValid()){
            Log.d(LOG_TAG, "Session is local, requesting a valid session before proceeding...");




            try {
                session = requestTrackingSession(authToken);

                localSession = track.getSessionId();

                if(!storage.updateTrackSession(localSession, session))
                    return false;


            } catch (AuthTokenIsExpiredException e) {
                throw new UnableToCloseSessionTokenExpiredException();
            }
        }else{
            session = track.getSessionId();
        }




        try {
            uploadLocationSequence(authToken, session, track.getTracedTrack());


            try {
                closeTrackingSession(authToken, session);
                storage.uploadTrack(session);
            } catch (AuthTokenIsExpiredException e1) {
                throw new UnableToCloseSessionTokenExpiredException();
            }


        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw new UnableToSubmitTrackTokenExpiredException();
        }

        return true;
    }
}
