/*
 * Copyright (c) 2016 Rodrigo Lourenço, Miguel Costa, Paulo Ferreira, João Barreto @  INESC-ID.
 *
 * This file is part of TRACE.
 *
 * TRACE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TRACE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TRACE.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.trace.storeclient.remote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.trace.storeclient.exceptions.AuthTokenIsExpiredException;
import org.trace.storeclient.exceptions.RemoteTraceException;
import org.trace.storeclient.exceptions.UnableToRequestGetException;
import org.trace.storeclient.exceptions.UnableToRequestPostException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;


/**
 * Base HttpClient for communication with the TRACEStore server.
 */
public class BaseHttpClient {

    protected final String LOG_TAG = "Remote";
    protected final JsonParser jsonParser = new JsonParser();
    protected String BASE_URI = "http://146.193.41.50:8080/trace";

    final protected void validateHttpResponse(String requestType, String response) throws RemoteTraceException {

        JsonObject jsonResponse = (JsonObject) jsonParser.parse(response);

        if(!jsonResponse.get("success").getAsBoolean()){
            throw new RemoteTraceException(requestType, jsonResponse.get("error").getAsString());
        }

    }

    private JsonObject constructRemoteRequest(String method, String urlEndpoint, JsonObject requestProperties, String data){
        JsonObject operation = new JsonObject();
        operation.addProperty("method", method);
        operation.addProperty("endpoint", urlEndpoint);
        operation.add("properties", requestProperties);
        operation.addProperty("data", data);
        return operation;
    }

    /**
     * Performs a post request
     * @param urlEndpoint The url endpoint
     * @param requestProperties The post request properties
     * @param data The data being posted
     *
     * @return The server's response
     *
     * @throws UnableToRequestPostException If the server's HTTP response is any other than 200 - OK.
     * @throws AuthTokenIsExpiredException If the authentication token has expired.
     */
    final public String performPostRequest(String urlEndpoint, JsonObject requestProperties, String data)
            throws UnableToRequestPostException, AuthTokenIsExpiredException {

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

    /**
     * Performs a GET request
     * @param urlEndpoint The url endpoint
     * @param requestProperties The GET request properties
     *
     * @return The server's response
     *
     * @throws UnableToRequestPostException If the server's HTTP response is any other than 200 - OK.
     * @throws AuthTokenIsExpiredException If the authentication token has expired.
     */
    final public String performGetRequest(String urlEndpoint, JsonObject requestProperties)
            throws UnableToRequestGetException, AuthTokenIsExpiredException {

        URL url;
        HttpURLConnection connection = null;
        String dataUrl = BASE_URI+urlEndpoint;

        try {

            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if(requestProperties != null)
                for(Map.Entry<String, JsonElement> entry : requestProperties.entrySet())
                    connection.setRequestProperty(entry.getKey(), entry.getValue().getAsString());

            connection.setDoInput(true);
            connection.connect();

            // Get Response
            int responseCode = connection.getResponseCode();

            switch (responseCode){
                case 200:
                    break;
                case 401:
                    throw new AuthTokenIsExpiredException();
                default:
                    throw new UnableToRequestGetException(String.valueOf(responseCode));
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
            throw new UnableToRequestGetException(e.getMessage());

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

    }

    /**
     * Request properties
     */
    public interface http {
        String CONTENT_TYPE = "Content-Type";
        String CONTENT_LENGTH = "Content-Length";
        String CONTENT_LANGUAGE = "Content-Language";
        String AUTHORIZATION = "Authorization";
    }
}
