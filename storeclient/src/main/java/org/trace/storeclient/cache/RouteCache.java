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

package org.trace.storeclient.cache;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;

import org.trace.storeclient.cache.exceptions.UnableToCreateRouteCopyException;
import org.trace.storeclient.data.Route;
import org.trace.storeclient.data.RouteSummary;
import org.trace.storeclient.data.RouteWaypoint;
import org.trace.storeclient.exceptions.AuthTokenIsExpiredException;
import org.trace.storeclient.exceptions.RemoteTraceException;
import org.trace.storeclient.remote.RouteHttpClient;
import org.trace.storeclient.storage.LocalRouteStorage;
import org.trace.storeclient.utils.ConnectivityUtils;

import java.util.List;


public class RouteCache {

    private static final String LOG_TAG = "RouteCache";
    private Context mContext;
    private RouteHttpClient mHttpClient;
    private LocalRouteStorage mLocalStorage;


    private RouteCache(Context context){
        mContext = context;
        mHttpClient = new RouteHttpClient();
        mLocalStorage = LocalRouteStorage.getStorageInstance(context);
    }


    private static RouteCache CACHE = null;

    public static RouteCache getCacheInstance(Context context){

        synchronized (RouteCache.class){
            if(CACHE == null)
                CACHE = new RouteCache(context);
        }

        return CACHE;
    }

    private boolean createLocalCopy(Route route){
        boolean success;


        success = mLocalStorage.storeCompleteRoute(route, true, false);
        mLocalStorage.dumpStorageLog();

        return success;
    }

    public boolean saveRoute(final String authToken, final Route route) throws UnableToCreateRouteCopyException {

        //Regardless of the scenario a local copy is always created
        boolean createdLocalCopy = createLocalCopy(route);

        if (!createdLocalCopy) {
            Log.w(LOG_TAG, "Failed to create a local copy of the route.");
            throw new UnableToCreateRouteCopyException();
        }

        // Scenario 1 - The app has a network connection (Wifi?)
        if(ConnectivityUtils.isConnected(mContext)) {
                //Handler postRoute = new Handler();
                //postRoute.post(new PostRouteRunnable(authToken, route));
                Thread t = new Thread(new PostRouteRunnable(authToken, route));
                t.run();
                return true;
        }

        return true;
    }

    public void loadRoute(String routeId){

    }

    /* Asyn Runnables
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private class PostRouteRunnable implements Runnable {

        private final String authToken;
        private final Route route;

        public PostRouteRunnable(String authToken, Route route){
            this.authToken = authToken;
            this.route = route;
        }

        @Override
        public void run() {

            String trackId = route.getSession();

            RouteSummary rs;
            String session = "";
            boolean wasPosted = true;

            // Step 2 - Then the route is uploaded to the TRACEStore server,
            //          this will provide an UID for the route.
            try {

                RouteSummary aux = route;
                aux.setSession(""); //TODO: find a nicer way to do this - the session must be empty for the server to accept it.
                String data = aux.toString();

                rs = mHttpClient.submitRouteSummary(authToken, data);
                session = rs.getSession();

                Log.d(LOG_TAG, "New session "+session);

                //TODO: for redundancy store the newly obtained route summary
                mLocalStorage.storeRouteSummary(rs, false, true);

            } catch (RemoteTraceException | AuthTokenIsExpiredException e) {
                e.printStackTrace();
                wasPosted = false;
            } finally {
                if (!wasPosted) {
                    Log.w(LOG_TAG, "Route was not posted");
                    return;
                }
            }


            //Step 3  - Upload the Route's trace (150 at each time)
            //Step 2a - Split the trace into batches of up to MAX_SIZE points
            int MAX_SIZE = 15;
            int size = 1, excess;
            JsonArray[] queue;
            JsonArray jTrace = route.getTraceAsJsonArray();

            if (jTrace.size() <= MAX_SIZE) {
                queue = new JsonArray[size];
                queue[0] = jTrace;
            } else {

                excess = jTrace.size() % MAX_SIZE;
                size = (jTrace.size() / MAX_SIZE) + (excess == 0 ? 0 : 1);

                queue = new JsonArray[size];

                for (int i = 0, left = 0, right = MAX_SIZE; i < size; i++) {

                    JsonArray batch = new JsonArray();

                    for (int j = left; j < right && j < jTrace.size(); j++) {
                        batch.add(jTrace.get(j));
                    }

                    left = right++;
                    right += MAX_SIZE;
                    queue[i] = batch;
                }
            }

            //Step 2b - Upload each of the batches and analyze the response
            // NOTE: Only if all batches are uploaded successfully with the track summary be stored.
            for (JsonArray batch : queue) {
                try {

                    boolean wasUploaded;

                    do {
                        wasUploaded = mHttpClient.uploadTraceBatch(authToken, session, batch);
                    } while (!wasUploaded);

                } catch (RemoteTraceException e) {
                    e.printStackTrace();
                } catch (AuthTokenIsExpiredException e) {
                    e.printStackTrace();
                }
            }

            //Step 3 - Request the map-matched values and then store the remote route
            try {
                RouteSummary aux = mHttpClient.downloadRouteSummary(authToken, session);
                List<RouteWaypoint> trace = mHttpClient.downloadRouteTrace(authToken, session);
                Log.d(LOG_TAG, aux.toString());

                mLocalStorage.updateRouteStateAndTrace(aux.getSession(), false, true, trace);
                mLocalStorage.dumpStorageLog();

            } catch (RemoteTraceException e) {
                e.printStackTrace();
            } catch (AuthTokenIsExpiredException e) {
                e.printStackTrace();
            }

            //Step 4 - Delete the local copy of the route
            boolean wasDeleted;
            do{
                wasDeleted = mLocalStorage.deleteRoute(trackId);
                mLocalStorage.dumpStorageLog();
            }while (!wasDeleted);

        }
    }
}
