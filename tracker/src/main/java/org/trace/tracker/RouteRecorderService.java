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

package org.trace.tracker;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.trace.tracker.exceptions.MissingLocationPermissionsException;
import org.trace.tracker.modules.activity.ActivityConstants;
import org.trace.tracker.settings.ConfigurationProfile;
import org.trace.tracker.settings.ConfigurationsManager;
import org.trace.tracker.storage.PersistentTrackStorage;
import org.trace.tracker.storage.data.Track;
import org.trace.tracker.storage.data.TrackSummary;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.EasyPermissions;


public class RouteRecorderService extends Service implements RouteRecorderInterface{

    private static final String LOG_TAG = "RouteRecorder";

    private IJsbergTracker mTracker;
    private PersistentTrackStorage mTrackStorage;
    private ConfigurationsManager mConfigManager;
    private final IBinder mBinder = new RouteRecorderBinder();


    /* Service Life Cycle
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    @Override
    public void onCreate() {
        super.onCreate();
        mTracker = IJsbergTracker.getTracker(this);
        mTrackStorage = new PersistentTrackStorage(this);
        mConfigManager = ConfigurationsManager.getInstance(this);

        mIdleTimer = mExecutorService.schedule(new IdleElapsed(), 20, TimeUnit.MINUTES); //TODO: should not be hardcoded
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    /* Service Binding
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {



        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public class RouteRecorderBinder extends Binder {
        RouteRecorderService getService(){
            return RouteRecorderService.this;
        }
    }

    /* Route Recorder Interface
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private String mCurrentSession;
    private RouteRecorderState mState;

    private final Object finishLock = new Object();

    private SimpleDateFormat mSDF = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String startTracking()
            throws MissingLocationPermissionsException{

        return startTracking(-1, false, false);
    }

    @Override
    public String startTracking(boolean isAutomatic, boolean isSilent)
            throws MissingLocationPermissionsException{

        return startTracking(-1, isAutomatic, isSilent);
    }

    @Override
    public String startTracking(int modality, boolean isAutomatic, boolean isSilent)
            throws MissingLocationPermissionsException {

        //Step 0 - Check for Location Permission, otherwise throw runtime exception
        if(!EasyPermissions.hasPermissions(this, TrackingConstants.permissions.TRACKING_PERMISSIONS))
            throw new MissingLocationPermissionsException();

        //Step 1 - Check if it is already tracking the user
        if(mState.isTracking){
            Log.w(LOG_TAG, LOG_TAG+" was already active.");
            return null;
        }

        //Step 2.a - Set the base values
        long startTime = System.currentTimeMillis();

        mState.setTracking(true);
        mState.setAutomaticTracking(isAutomatic);
        mState.setSilentStart(isSilent);
        mState.setElapsedDistance(0);
        mState.setCurrentTrackSummary(
                mTrackStorage.createNewTrackSummary(startTime, modality, isAutomatic ? 0 : 2));

        Log.i(LOG_TAG, "Starting a new route @"+mSDF.format(new Date(startTime)));

        //Step 2.b - Reset the idle timer
        resetIdleTimer();

        //Step 3 - Analyze question IJsberg
        if(isAutomatic){
            //TODO: do something i dont understand what or why
        }

        String session = mTrackStorage.getNextAvailableId(); //TODO: deprecate this, use mCurrentTrackSummary instead
        mCurrentSession= session;
        boolean isValid= false; //TODO: this should disapear completely


        mTracker.setSession(session, isValid);
        mTracker.updateSettings();
        mTracker.startLocationUpdates();
        mTracker.startActivityUpdates();

        //Step 4 - Register the receiver, which are responsible for handling new locations and activities.
        IntentFilter listenerFilter = new IntentFilter();
        listenerFilter.addAction(ActivityConstants.COLLECT_ACTION);
        listenerFilter.addAction(TrackingConstants.tracker.COLLECT_LOCATIONS_ACTION);

        LocalBroadcastManager.getInstance(this).registerReceiver(mTracker, listenerFilter);

        return mCurrentSession;
    }




    @Override
    public Track stopTracking() {
        return stopTracking(true);
    }

    @Override
    public Track stopTracking(boolean isManual) {

        synchronized (finishLock){
            if(mState.isFinishing()){
                Log.w(LOG_TAG, "Already finishing this route");
                return null;
            }else
                mState.setFinishing(true);
        }

        //TODO: maybe lock this one two
        if(!mState.isTracking()){
            Log.w(LOG_TAG, "No route was started.");
            return null;
        }else
            mState.setTracking(false);

        TrackSummary currentTrack = mState.currentTrackSummary;

        //Step 1 - Check if the route was automatic
        //          if so, delete tracks with less than 250m
        if(mState.isAutomaticTracking() && mState.getElapsedDistance() < 250) { //TODO: should not be hardcoded
            Log.w(LOG_TAG, "The track "+currentTrack.getSession()+" was ignored because of its size.");
            mTrackStorage.removeTrackSummaryAndTrace(mState.getCurrentTrackSummary());
            return null;
        }

        //Step 2 - Update the track summary sensing type in case of manual sensing.
        if(isManual){
            //currentTrack.setSensingType(1);
            mTrackStorage.updateTrackSummary(currentTrack);
        }

        //Step 3 - Stop location and activity updates and unregister the receiver
        mTracker.stopLocationUpdates();
        mTracker.stopActivityUpdates();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTracker);


        //Step 4 - ???
        // Basically, the check if there is last position (why was it not added?), and if there is
        // then the position if added to the TrackSummary and also to the sqlite storage
        if(mTracker.getCurrentLocation() != null){ //TODO: assure that is is the last acquired position and not the FusedLocation one

        }

        //Step 5 there is a lot of UI and network effort which we deemed inappropriate for the module!

        //Step 6 - The automatic tracking is re-enabled.

        //Step 7 - Stop the timers
        stopIdleTimer();


        deleteTrackIfIrrelevant(mCurrentSession);

        return mTrackStorage.getCompleteTrack(currentTrack);
    }

    private boolean deleteTrackIfIrrelevant(String session){
        Track t = mTrackStorage.getTrack(mCurrentSession);

        if(t == null || t.getTravelledDistance() <= 15 || t.getTracedTrack().size() <= 5) {
            mTrackStorage.deleteTrackById(session);

            Log.i(LOG_TAG, "This track was not stored because it is too short.");

            return true;
        }

        return false;
    }

    @Override
    public Location getLastLocation() {
        return mTracker.getCurrentLocation();
    }

    @Override
    public ConfigurationProfile getCurrentTrackingProfile() {
        return mConfigManager.getTrackingProfile();
    }

    @Override
    public void updateTrackingProfile(ConfigurationProfile profile) {
        mConfigManager.saveTrackingProfile(profile);
    }

    @Override
    public List<TrackSummary> getAllTracedTracks() {
        return mTrackStorage.getTracksSessions();
    }

    @Override
    public Track getTracedTrack(String trackId) {
        return mTrackStorage.getTrack(trackId);
    }

    @Override
    public void deleteTracedTrack(String trackId) {
        mTrackStorage.deleteTrackById(trackId);
    }

    /* State
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private class RouteRecorderState {
        protected boolean isTracking = false;
        protected boolean isAutomaticTracking = false;
        protected boolean isSilentStart = false;
        protected boolean isFinishing = false;

        @Deprecated //TODO: migrate to the tracker
        protected long elapsedDistance = 0;

        @Deprecated //TODO: migrate to the tracker
        private TrackSummary currentTrackSummary;

        protected RouteRecorderState(){}

        public boolean isTracking() {
            return isTracking;
        }

        public void setTracking(boolean tracking) {
            isTracking = tracking;
        }

        public boolean isAutomaticTracking() {
            return isAutomaticTracking;
        }

        public void setAutomaticTracking(boolean automaticTracking) {
            isAutomaticTracking = automaticTracking;
        }

        public boolean isSilentStart() {
            return isSilentStart;
        }

        public void setSilentStart(boolean silentStart) {
            isSilentStart = silentStart;
        }

        @Deprecated
        public long getElapsedDistance() {
            return elapsedDistance;
        }

        @Deprecated
        public void setElapsedDistance(long elapsedDistance) {
            this.elapsedDistance = elapsedDistance;
        }

        public boolean isFinishing() {
            return isFinishing;
        }

        public void setFinishing(boolean finishing) {
            isFinishing = finishing;
        }

        @Deprecated
        public TrackSummary getCurrentTrackSummary() {
            return currentTrackSummary;
        }

        @Deprecated
        public void setCurrentTrackSummary(TrackSummary currentTrackSummary) {
            this.currentTrackSummary = currentTrackSummary;
        }
    }

    /* Timers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    //TODO: migrate to IJsbergTracker
    private ScheduledFuture mIdleTimer;
    private ScheduledExecutorService mExecutorService = Executors.newScheduledThreadPool(1);


    protected void startIdleTimer(){
        mIdleTimer = mExecutorService.schedule(new IdleElapsed(), 20, TimeUnit.MINUTES);
    }
    protected void stopIdleTimer(){
        mIdleTimer.cancel(true);
    }

    protected void resetIdleTimer(){
        stopIdleTimer();
        startIdleTimer();
    }

    private class IdleElapsed implements Runnable {
        @Override
        public void run() {

            if(!mState.isTracking){
                Log.i(LOG_TAG, "IdleTime complete, however not tracking.");
                return;
            }

            stopTracking();
            stopIdleTimer();
        }
    }
}
