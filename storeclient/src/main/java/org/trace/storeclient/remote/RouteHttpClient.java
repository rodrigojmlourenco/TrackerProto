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

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.trace.storeclient.data.RouteSummary;
import org.trace.storeclient.data.RouteWaypoint;
import org.trace.storeclient.exceptions.AuthTokenIsExpiredException;
import org.trace.storeclient.exceptions.RemoteTraceException;
import org.trace.storeclient.exceptions.UnableToRequestGetException;
import org.trace.storeclient.exceptions.UnableToRequestPostException;

import java.util.ArrayList;
import java.util.List;

/**
 * HttpClient specifically designed to ease route-based network operations.
 *
 * @see BaseHttpClient
 */
public class RouteHttpClient extends BaseHttpClient {

    public RouteHttpClient(){
        this.BASE_URI = BASE_URI + "/tracker";
    }

    /**
     * Submits the specified route summary, which is in JSON, to the TRACEStore server.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     * @param summary The route summary.
     *
     * @return The route summary with an updated and valid session identifier.
     *
     * @throws RemoteTraceException If a network error occurred.
     * @throws AuthTokenIsExpiredException If the authentication token is no longer valid.
     */
    public RouteSummary submitRouteSummary(String authToken, String summary)
            throws RemoteTraceException, AuthTokenIsExpiredException {

        String data = summary;
        String urlEndpoint = "/put/track";

        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(http.AUTHORIZATION, "Bearer "+authToken);
        requestProperties.addProperty(http.CONTENT_TYPE, "application/json; charset=UTF-8");

        try {
            String response = performPostRequest(urlEndpoint, requestProperties, data);
            validateHttpResponse("submitRouteSummary", response);

            JsonObject jResponse = (JsonObject) jsonParser.parse(response);

            return new RouteSummary((JsonObject) jsonParser.parse(jResponse.get("token").getAsString()));


        } catch (UnableToRequestPostException e) {
            e.printStackTrace();
            throw new RemoteTraceException("submitRouteSummary", e.getMessage());
        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Uploads the trace or part of the trace, i.e. sequence of RouteWaypoint to the TRACEStore server. This operation
     * should only be performed once the RouteSummary has also been uploaded.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     * @param session The route's session, which is assigned by the TRACEStore server.
     * @param traceBatch A batch or the complete trace.
     *
     * @return True if it was successfully stored in by the server.
     *
     * @throws RemoteTraceException If a network error occurred.
     * @throws AuthTokenIsExpiredException If the authentication token is no longer valid.
     */
    public boolean uploadTraceBatch(String authToken, String session, JsonArray traceBatch)
            throws RemoteTraceException, AuthTokenIsExpiredException {
        String data = traceBatch.toString();
        String urlEndpoint = "/put/track/trace?session="+session;

        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(http.AUTHORIZATION, "Bearer "+authToken);
        requestProperties.addProperty(http.CONTENT_TYPE, "application/json; charset=UTF-8");

        try {
            String response = performPostRequest(urlEndpoint, requestProperties, data);
            validateHttpResponse("uploadTraceBatch", response);
            return true;
        } catch (UnableToRequestPostException e) {
            e.printStackTrace();
            throw new RemoteTraceException("uploadTraceBatch", e.getMessage());
        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Downloads the RouteSummary kept by the TRACEStore server, and identified by the provided
     * session.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     * @param session The route's session, which is assigned by the TRACEStore server.
     *
     * @return The downloaded route summary.
     *
     * @throws RemoteTraceException If a network error occurred.
     * @throws AuthTokenIsExpiredException If the authentication token is no longer valid.
     */
    public RouteSummary downloadRouteSummary(String authToken, String session)
            throws RemoteTraceException, AuthTokenIsExpiredException {

        String urlEndpoint = "/get/track?session="+session;

        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(http.AUTHORIZATION, "Bearer "+authToken);
        requestProperties.addProperty(http.CONTENT_TYPE, "application/json; charset=UTF-8");

        try {
            String response = performGetRequest(urlEndpoint, requestProperties);
            validateHttpResponse("downloadRouteSummary", response);

            JsonObject jResponse = (JsonObject) jsonParser.parse(response);

            return new RouteSummary((JsonObject) jsonParser.parse(jResponse.get("token").getAsString()));

        } catch (UnableToRequestGetException e) {
            e.printStackTrace();
            throw new RemoteTraceException("downloadRouteSummary", e.getMessage());
        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Downloads the trace stored by the TRACEStore server, and identified by the provided session.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     * @param session The route's session, which is assigned by the TRACEStore server.
     *
     * @return The downloaded route trace.
     *
     * @throws RemoteTraceException If a network error occurred.
     * @throws AuthTokenIsExpiredException If the authentication token is no longer valid.
     */
    public List<RouteWaypoint> downloadRouteTrace(String authToken, String session)
            throws RemoteTraceException, AuthTokenIsExpiredException {

        List<RouteWaypoint> trace = new ArrayList<>();
        String urlEndpoint = "/get/track/trace?session="+session;

        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(http.AUTHORIZATION, "Bearer "+authToken);
        requestProperties.addProperty(http.CONTENT_TYPE, "application/json; charset=UTF-8");

        try {
            String response = performGetRequest(urlEndpoint, requestProperties);
            validateHttpResponse("downloadRouteTrace", response);

            JsonObject jResponse = (JsonObject) jsonParser.parse(response);
            JsonArray jTrace = (JsonArray) jsonParser.parse(jResponse.get("token").getAsString());

            for (int i=0; i < jTrace.size(); i++)
                trace.add(new RouteWaypoint(session, (JsonObject) jTrace.get(i)));


            Log.d(LOG_TAG, "Stuff");
            return trace;

        } catch (UnableToRequestGetException e) {
            e.printStackTrace();
            throw new RemoteTraceException("downloadRouteTrace", e.getMessage());
        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Queries the TRACEStore server for the list of all complete, i.e. routes with complete traces,
     * stored in the server, and belonging to the user.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     *
     * @return List of complete route sessions stored in the TRACEStore server.
     *
     * @throws RemoteTraceException If a network error occurred.
     * @throws AuthTokenIsExpiredException If the authentication token is no longer valid.
     */
    public List<String> queryExistingSessions(String authToken)
            throws RemoteTraceException, AuthTokenIsExpiredException{

        List<String> serverSessions = new ArrayList<>();
        String urlEndpoint = "/get/track/digest";

        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(http.AUTHORIZATION, "Bearer "+authToken);
        requestProperties.addProperty(http.CONTENT_TYPE, "application/json; charset=UTF-8");

        try {
            String response = performGetRequest(urlEndpoint, requestProperties);
            validateHttpResponse("queryExistingSessions", response);

            JsonObject jResponse = (JsonObject) jsonParser.parse(response);
            JsonArray jSessions = (JsonArray) jsonParser.parse(jResponse.get("token").getAsString());

            for(int i=0; i < jSessions.size(); i++)
                serverSessions.add(jSessions.get(i).getAsString());

            return serverSessions;

        } catch (UnableToRequestGetException e) {
            e.printStackTrace();
            throw new RemoteTraceException("queryExistingSessions", e.getMessage());
        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
