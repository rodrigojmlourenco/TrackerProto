package org.trace.trackerproto.store;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.trace.trackerproto.store.exceptions.AuthTokenIsExpiredException;
import org.trace.trackerproto.store.exceptions.InvalidAuthCredentialsException;
import org.trace.trackerproto.store.exceptions.LoginFailedException;
import org.trace.trackerproto.store.exceptions.RemoteTraceException;
import org.trace.trackerproto.store.exceptions.UnableToPerformLogin;
import org.trace.trackerproto.tracking.data.SerializableLocation;
import org.trace.trackerproto.tracking.data.Track;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

//TODO: refactorizar... muito código repetido
public class HttpClient {

    private final String LOG_TAG = HttpClient.class.getSimpleName();

    private JsonParser jsonParser;
    private final String BASE_URI = "http://146.193.41.50:8080/trace";

    public HttpClient(){
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


    public String login(String username, String password) throws UnableToPerformLogin, LoginFailedException {

        URL url;
        HttpURLConnection connection = null;
        String dataUrl = BASE_URI+"/auth/login";
        String dataUrlParameters = "username="+username+"&password="+password;

        try {

            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(dataUrlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US,en,pt");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(dataUrlParameters);
            wr.flush();
            wr.close();

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }

            rd.close();
            return extractAuthToken(response.toString());

        } catch (IOException e) {

            e.printStackTrace();
            throw new UnableToPerformLogin();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void logout(String authToken) throws RemoteTraceException, AuthTokenIsExpiredException {

        URL url;
        HttpURLConnection connection = null;
        String dataUrl = BASE_URI+"/auth/logout";

        try {

            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.flush();
            wr.close();

            // Get Response
            int code = connection.getResponseCode();
            Log.d(LOG_TAG, "{ op: 'logout, code:" + code + "}");

            if(code == 401)
                throw new AuthTokenIsExpiredException();

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }

            rd.close();

        } catch (IOException e) {

            e.printStackTrace();
            throw new RemoteTraceException("Logout", e.getMessage());

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
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

    public String requestTrackingSession(String authToken) throws RemoteTraceException {

        URL url;
        HttpURLConnection connection = null;
        String dataUrl = BASE_URI+"/auth/session/open";

        try {

            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer "+authToken);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.flush();
            wr.close();

            // Get Response
            connection.getResponseCode();
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }

            rd.close();

            return validateAndExtractSession(response.toString());


        } catch (IOException e) {

            e.printStackTrace();
            throw new RemoteTraceException("GetSession", e.getMessage());

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


    public void closeTrackingSession(String authToken, String session) throws RemoteTraceException {

        URL url;
        HttpURLConnection connection = null;
        String dataUrl = BASE_URI+"/auth/close";
        String dataUrlParams = "session="+session;
        try {

            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer "+authToken);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(dataUrlParams);
            wr.flush();
            wr.close();

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }

            rd.close();

            validateHttpResponse("CloseSession", response.toString());


        } catch (IOException e) {

            e.printStackTrace();
            throw new RemoteTraceException("CloseSession", e.getMessage());

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void uploadGeoLocation(String authToken, String session, String geoLocation) throws RemoteTraceException {
        URL url;
        Gson gson = new Gson();
        HttpURLConnection connection = null;
        String dataUrl = BASE_URI+"/tracker/put/geo/"+session;
        try {

            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(geoLocation.getBytes("UTF-8"));
            wr.flush();
            wr.close();

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }

            rd.close();

            validateHttpResponse("UploadGeoLocation", response.toString());

        } catch (IOException e) {

            e.printStackTrace();
            throw new RemoteTraceException("UploadGeoLocation", e.getMessage());

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    private String constructTraceTrack(List<SerializableLocation> locations){
        JsonObject traceTrack = new JsonObject();
        JsonArray track = new JsonArray();

        for(SerializableLocation location : locations){
            JsonObject jsonLocation = new JsonObject();
            jsonLocation.addProperty("latitude", location.getLatitude());
            jsonLocation.addProperty("longitude", location.getLongitude());
            jsonLocation.addProperty("timestamp", location.getTimestamp());
            track.add(jsonLocation);
        }

        traceTrack.add("track", track);
        return traceTrack.toString();
    }

    private void uploadLocationSequence(String authToken, String session, List<SerializableLocation> locations) throws RemoteTraceException {

        URL url;
        Gson gson = new Gson();
        HttpURLConnection connection = null;
        String dataUrl = BASE_URI+"/tracker/put/track/"+session;

        String data = constructTraceTrack(locations);


        try {

            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }

            rd.close();

            validateHttpResponse("UploadTrack", response.toString());

        } catch (IOException e) {

            e.printStackTrace();
            throw new RemoteTraceException("UploadTrack", e.getMessage());

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
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

    public void submitTrackAndCloseSession(String authToken, Track track) throws RemoteTraceException {

        String session = track.getSessionId();

        uploadLocationSequence(authToken, session, track.getTracedTrack());
        //closeTrackingSession(authToken, session);


    }


}
