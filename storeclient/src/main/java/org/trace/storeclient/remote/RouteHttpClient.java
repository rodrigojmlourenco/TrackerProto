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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.trace.storeclient.data.RouteSummary;
import org.trace.storeclient.exceptions.AuthTokenIsExpiredException;
import org.trace.storeclient.exceptions.RemoteTraceException;
import org.trace.storeclient.exceptions.UnableToRequestPostException;

/**
 * Created by Rodrigo Lourenço on 06/07/2016.
 */
public class RouteHttpClient extends BaseHttpClient {

    public RouteHttpClient(){
        this.BASE_URI = BASE_URI + "/tracker";
    }

    public RouteSummary submitRouteSummary(String authToken, String summary)
            throws RemoteTraceException, AuthTokenIsExpiredException {

        String data = summary;
        String urlEndpoint = "/put/track";

        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(http.AUTHORIZATION, "Bearer "+authToken);
        requestProperties.addProperty(http.CONTENT_TYPE, "application/json; charset=UTF-8");

        try {
            String response = performPostRequest(urlEndpoint, requestProperties, data);
            validateHttpResponse("UploadTrack", response);

            JsonObject jResponse = (JsonObject) jsonParser.parse(response);

            return new RouteSummary((JsonObject) jsonParser.parse(jResponse.get("token").getAsString()));


        } catch (UnableToRequestPostException e) {
            e.printStackTrace();
            throw new RemoteTraceException("UploadTrack", e.getMessage());
        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean uploadTraceBatch(String authToken, String session, JsonArray traceBatch) throws RemoteTraceException, AuthTokenIsExpiredException {
        String data = traceBatch.toString();
        String urlEndpoint = "/put/track/trace?session="+session;

        JsonObject requestProperties = new JsonObject();
        requestProperties.addProperty(http.AUTHORIZATION, "Bearer "+authToken);
        requestProperties.addProperty(http.CONTENT_TYPE, "application/json; charset=UTF-8");

        try {
            String response = performPostRequest(urlEndpoint, requestProperties, data);
            validateHttpResponse("UploadTrack", response);

            JsonObject jResponse = (JsonObject) jsonParser.parse(response);

            //return new RouteSummary((JsonObject) jsonParser.parse(jResponse.get("token").getAsString()));
            return true;

        } catch (UnableToRequestPostException e) {
            e.printStackTrace();
            throw new RemoteTraceException("UploadTrack", e.getMessage());
        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
