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
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.JsonArray;

import org.trace.storeclient.cache.exceptions.RouteIsIncompleteException;
import org.trace.storeclient.cache.exceptions.RouteNotFoundException;
import org.trace.storeclient.cache.exceptions.RouteNotParsedException;
import org.trace.storeclient.cache.exceptions.UnableToCreateRouteCopyException;
import org.trace.storeclient.cache.storage.RouteStorage;
import org.trace.storeclient.data.Route;
import org.trace.storeclient.data.RouteSummary;
import org.trace.storeclient.data.RouteWaypoint;
import org.trace.storeclient.exceptions.AuthTokenIsExpiredException;
import org.trace.storeclient.exceptions.RemoteTraceException;
import org.trace.storeclient.remote.RouteHttpClient;
import org.trace.storeclient.utils.ConnectivityUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Cache for storage and loading of routes.
 * <br>
 * This cache operates as an abstraction layer, masquerading all network operation. For performance,
 * all stored and loaded routes are first obtained through the local cache, while in the background
 * the network operations are performed.
 * <br>
 * This cache works in tandem with the CacheConnectivityMonitor to perform postponed
 * connectivity-sensitive operations.
 * <br>
 * Finally, the cache was also designed to reduce memory consumption of the users' device. To do so,
 * everytime an instance of the cache is requested, all route's traces older than a week are deleted.
 * The route summaries and states, however, are never deleted.
 */
public class RouteCache {

    public static final boolean VERBOSE = true;
    private static final String LOG_TAG = "RouteCache";

    private Context mContext;
    private RouteStorage mStorage;
    private RouteHttpClient mHttpClient;
    private ExecutorService mAsyncWorkers;

    private RouteCache(Context context){
        mContext = context;
        mHttpClient = new RouteHttpClient();
        mStorage = RouteStorage.getStorageInstance(context);
        mAsyncWorkers = Executors.newFixedThreadPool(4);

        this.clearOldTraces();
    }

    /* Singleton
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private static RouteCache CACHE = null;

    public static RouteCache getCacheInstance(Context context){

        synchronized (RouteCache.class){
            if(CACHE == null)
                CACHE = new RouteCache(context);
        }

        return CACHE;
    }

    /* Storing Routes
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private boolean createLocalCopy(Route route){
        boolean success;

        success = mStorage.storeCompleteRoute(route, true, false);

        if(VERBOSE) mStorage.dumpStorageLog();

        return success;
    }

    /**
     * Saves a route, both in a local repository and in the TRACEStore server.
     * <br>
     * If there is no network connection, and for redundancy purposes, the route is always stored
     * first in the local repository. Additionally, it is also marked as pending submission.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     * @param route The route to be stored
     *
     * @return True if the route was successfully stored, false otherwise.
     *
     * @throws UnableToCreateRouteCopyException
     */
    public boolean saveRoute(final String authToken, final Route route) throws UnableToCreateRouteCopyException {

        //Regardless of the scenario a local copy is always created
        boolean hasCreatedLocalCopy = createLocalCopy(route);

        if (!hasCreatedLocalCopy) {
            Log.w(LOG_TAG, "Failed to create a local copy of the route.");
            throw new UnableToCreateRouteCopyException();
        }


        if(ConnectivityUtils.isConnected(mContext)) {
            //mAsyncWorkers.execute(new PostRouteRunnable(authToken, route));
            postPendingRoutes(authToken);
        }

        return true;
    }

    /**
     * Because the user may not be connected at the time he requests a route to be saved, routes
     * may be stored locally but not in the server. This method enables pending routes to be
     * uploaded to the server.
     * <br>
     * Once a local route has been uploaded, it is removed from the local repository.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     */
    public void postPendingRoutes(String authToken){

        synchronized (mPostPendingLock){
            if(isPostingPendingRoutes)
                return;
            else
                isPostingPendingRoutes = true;
        }

        if(!mStorage.hasPendingRoutes()){
            Log.i(LOG_TAG, "No pending routes to upload");
            return;
        }

        final List<String> pendingRoutes = mStorage.getLocalRoutesSessions();

        if(pendingRoutes.size() > 0){
            mAsyncWorkers.execute(new PostPendingRoutesRunnable(authToken, pendingRoutes));
        }
    }

    private boolean isPostingPendingRoutes;
    private final Object mPostPendingLock = new Object();

    /* Loading Routes
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * Fetches all the user's route summaries.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     *
     * @return The user's route summaries.
     */
    public List<RouteSummary> loadRoutes(String authToken){

        if(ConnectivityUtils.isConnected(mContext) && authToken!=null) //TODO: this should be avoided
            this.loadMissingRoutes(authToken);

        return mStorage.getAllRoutes();
    }


    /**
     * Fetches a route identified by its session.
     * <br>
     * Additionally, it is important to mention that is the route is remote but incomplete, this
     * method will trigger the fetching of the route's trace.
     * @param authToken Required for communication with the server.
     * @param session String that uniquely identifies the route.
     *
     * @return The route summary identified by the session.
     *
     * @throws RouteNotFoundException when no Route is found, this should never occur however.
     */
    public RouteSummary loadRoute(final String authToken, final String session) throws RouteNotFoundException {

        RouteSummary summary = mStorage.getRouteSummary(session);

        //If the route is on the server, and its not complete on the device
        //then (if there is connectivity) the trace will be downloaded.
        if(ConnectivityUtils.isConnected(mContext) && authToken != null &&
                !mStorage.isLocalRoute(session) && !mStorage.isCompleteRoute(session)){

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {

                    List<RouteWaypoint> trace;

                    try {
                        trace = mHttpClient.downloadRouteTrace(authToken, session);
                        mStorage.updateRouteStateAndTrace(session, false, true, trace);
                        if(VERBOSE) mStorage.dumpStorageLog();
                    } catch (RemoteTraceException | AuthTokenIsExpiredException e) {
                        e.printStackTrace();
                    }
                }
            });

            t.run();
        }

        return summary;
    }


    /**
     * Loads the route trace, i.e. the sequence of route waypoints.
     *
     * @param authToken The authentication token, required by the TRACEStore server for security.
     * @param session The session that identifies the route.
     *
     * @return The route's trace
     *
     * @see RouteWaypoint
     *
     * @throws RouteNotFoundException If the user does not have the route.
     * @throws RouteIsIncompleteException If the route is stored, but its points are not.
     * @throws RouteNotParsedException If the route has not been map-matched yet by the server.
     */
    public List<RouteWaypoint> loadRouteTrace(final String authToken, final String session)
            throws RouteNotFoundException, RouteIsIncompleteException, RouteNotParsedException {

        if(mStorage.isLocalRoute(session))
            throw new RouteNotParsedException(session);

        if (mStorage.isCompleteRoute(session)){
            return mStorage.getRouteTrace(session);
        }else {

            if(ConnectivityUtils.isConnected(mContext) && authToken != null
                    && !mStorage.isLocalRoute(session)){
                mAsyncWorkers.execute(new Runnable() {
                    @Override
                    public void run() {
                        List<RouteWaypoint> trace;

                        try {
                            trace = mHttpClient.downloadRouteTrace(authToken, session);
                            mStorage.updateRouteStateAndTrace(session, false, true, trace);
                            if(VERBOSE) mStorage.dumpStorageLog();

                            LocalBroadcastManager.getInstance(mContext)
                                    .sendBroadcast(new Intent(Broadcast.TRACE_AVAILABLE_BROADCAST));

                        } catch (RemoteTraceException | AuthTokenIsExpiredException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            throw new RouteIsIncompleteException(session);
        }
    }

    /**
     * Although slim, there is a chance that the server contains routes that the storage does not.
     * This method queries the server for all the sessions stored in it, and for each missing one
     * it requests the Route Summary.
     *
     * <b>This method required network connectivity.</b>
     */
    public void loadMissingRoutes(final String authToken){

        mAsyncWorkers.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    List<String> serverSessions = mHttpClient.queryExistingSessions(authToken);
                    List<String> localSessions  = mStorage.getRemoteRouteSessions();

                    serverSessions.removeAll(localSessions);

                    for(String missing : serverSessions) {
                        Route aux = new Route(mHttpClient.downloadRouteSummary(authToken, missing));
                        mStorage.storeCompleteRoute(aux, false, false);
                    }

                    Log.i(LOG_TAG, "All missing route sessions loaded");

                    if(VERBOSE) mStorage.dumpStorageLog();

                    if(serverSessions.size() > 0)
                        LocalBroadcastManager.getInstance(mContext)
                                .sendBroadcast(new Intent(Broadcast.ROUTES_AVAILABLE_BROADCAST));

                } catch (RemoteTraceException e) {
                    e.printStackTrace();
                } catch (AuthTokenIsExpiredException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /* Cache Memory Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * Removes all routes' traces older than a week. The route summary, on the other hand, will
     * remain on the local repository.
     */
    protected void clearOldTraces(){
        mAsyncWorkers.execute(new Runnable() {
            @Override
            public void run() {
                mStorage.deleteOldRouteTraces();
            }
        });
    }





    /* Async Runnable
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private final Object mUploadLock = new Object();

    private class PostRouteRunnable implements Runnable {

        public static final int MAX_SIZE = 150;

        private final String authToken;
        private final Route route;

        public PostRouteRunnable(String authToken, Route route){
            this.authToken = authToken;
            this.route = route;
        }

        private JsonArray[] splitTraceIntoBatches(JsonArray trace){

            JsonArray[] queue;
            int size = 1, excess;

            if (trace.size() <= MAX_SIZE) {
                queue = new JsonArray[size];
                queue[0] = trace;
            } else {

                excess = trace.size() % MAX_SIZE;
                size = (trace.size() / MAX_SIZE) + (excess == 0 ? 0 : 1);

                queue = new JsonArray[size];

                for (int i = 0, left = 0, right = MAX_SIZE; i < size; i++) {

                    JsonArray batch = new JsonArray();

                    for (int j = left; j < right && j < trace.size(); j++) {
                        batch.add(trace.get(j));
                    }

                    left = right++;
                    right += MAX_SIZE;
                    queue[i] = batch;
                }
            }

            return queue;
        }

        @Override
        public void run() {

            String trackId = route.getSession();

            RouteSummary rs;
            String session = "";
            boolean wasPosted = true;

            synchronized (mUploadLock) {

                // Step 1 - The route is uploaded to the TRACEStore server,
                //          this will provide an UID for the route.
                try {

                    RouteSummary aux = route;
                    aux.setSession(""); //TODO: find a nicer way to do this - the session must be empty for the server to accept it.
                    String data = aux.toString();

                    rs = mHttpClient.submitRouteSummary(authToken, data);
                    session = rs.getSession();

                    mStorage.storeRouteSummary(rs, false, true);

                } catch (RemoteTraceException | AuthTokenIsExpiredException e) {
                    e.printStackTrace();
                    wasPosted = false;
                } finally {
                    if (!wasPosted) {
                        Log.w(LOG_TAG, "Route was not posted");
                        return;
                    }
                }

                //Step 2  - Upload the Route's trace (150 at each time)
                //Step 2a - Split the trace into batches of up to MAX_SIZE points
                JsonArray[] queue = splitTraceIntoBatches(route.getTraceAsJsonArray());

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

                    mStorage.updateRouteStateAndTrace(aux.getSession(), false, true, trace);

                    if(VERBOSE) mStorage.dumpStorageLog();

                } catch (RemoteTraceException | AuthTokenIsExpiredException e) {
                    e.printStackTrace();
                }

                //Step 4 - Delete the local copy of the route
                boolean wasDeleted;
                do {
                    wasDeleted = mStorage.deleteRoute(trackId);
                    if(VERBOSE) mStorage.dumpStorageLog();
                } while (!wasDeleted);
            }
        }
    }

    public class PostPendingRoutesRunnable implements Runnable {

        private final String authToken;
        private List<String> pendingSessions;

        public PostPendingRoutesRunnable(String authToken, List<String> sessions){
            this.authToken = authToken;
            pendingSessions = sessions;
        }

        @Override
        public void run() {

            Log.i(LOG_TAG, "Uploading "+pendingSessions.size()+" pending tracks");

            for(String session : pendingSessions){
                if(ConnectivityUtils.isConnected(mContext)) {
                    try {

                        // 1 - Load the local route
                        Route route = new Route(mStorage.getRouteSummary(session));
                        route.setTrace(mStorage.getRouteTrace(session));

                        // 2 - Upload the local route
                        PostRouteRunnable post = new PostRouteRunnable(authToken, route);
                        post.run();

                        Log.i(LOG_TAG, "Pending track successfully posted.");

                    } catch (RouteNotFoundException e) {
                        e.printStackTrace();
                    }
                }else{
                    break;
                }
            }

            synchronized (mPostPendingLock){
                isPostingPendingRoutes = false;
            }
        }
    }

    /* Broadcasts
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    public interface Broadcast {
        String TRACE_AVAILABLE_BROADCAST = "org.trace.storeclient.cache.broadcast.TRACE_AVAILABLE";
        String ROUTES_AVAILABLE_BROADCAST = "org.trace.storeclient.cache.broadcast.ROUTES_AVAILABLE";
    }

    /* Testing - Please remove before release
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public void dumpInfo(){
        mStorage.dumpStorageLog();
    }

}
