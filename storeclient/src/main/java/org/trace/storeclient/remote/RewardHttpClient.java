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
import com.google.gson.JsonParser;

import org.trace.storeclient.exceptions.AuthTokenIsExpiredException;
import org.trace.storeclient.exceptions.RemoteTraceException;
import org.trace.storeclient.exceptions.UnableToRequestGetException;


public class RewardHttpClient extends BaseHttpClient {

    public JsonArray fetchShopsWithRewards(double latitude, double longitude, double radius)
            throws RemoteTraceException, UnableToRequestGetException {

        JsonParser parser = new JsonParser();

        String data = String.format("lat=%f&lon=%f&radius=%f", latitude, longitude, radius);
        String urlEndpoint = "/reward/rewards?"+data;

        try {
            String response = performGetRequest(urlEndpoint, null, null);

            Log.d(LOG_TAG, response);

            JsonObject jsonResponse = (JsonObject) parser.parse(response);

            if(jsonResponse.get("success").getAsBoolean()) {
                return (JsonArray) parser.parse(jsonResponse.get("payload").getAsString());
            } else {
                int errorCode = jsonResponse.get("code").getAsInt();
                String errorMessage = jsonResponse.get("error").getAsString();
                throw new RemoteTraceException(String.valueOf(errorCode), errorMessage);
            }


        } catch (AuthTokenIsExpiredException e) {
            e.printStackTrace();
        }

        return null;
    }
}
