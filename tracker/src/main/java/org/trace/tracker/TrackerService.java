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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.trace.tracker.exceptions.MissingLocationPermissionsException;
import org.trace.tracker.permissions.PermissionsConstants;
import org.trace.tracker.settings.ConfigurationProfile;
import org.trace.tracker.settings.ConfigurationsManager;
import org.trace.tracker.storage.PersistentTrackStorage;
import org.trace.tracker.storage.data.Track;
import org.trace.tracker.storage.data.TrackSummary;
import org.trace.tracker.tracking.IJsbergTrackingEngine;
import org.trace.tracker.tracking.TrackingConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * <b>Note: </b>To avoid incoherent RouteRecorderState's this service should first be started and then
 * bound.
 */
public class TrackerService extends Service implements Tracker {

    private static final String LOG_TAG = "Tracker";
    private static final boolean IS_TESTING = false; //TODO: DANGEROUS please remove before release

    private TrackingEngine mTracker;
    private PersistentTrackStorage mTrackStorage;
    private ConfigurationsManager mConfigManager;
    private final IBinder mBinder = new CustomBinder();


    /* Service Life Cycle
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    @Override
    public void onCreate() {
        super.onCreate();
        mTracker = IJsbergTrackingEngine.getTracker(this);
        mTrackStorage = new PersistentTrackStorage(this);
        mConfigManager = ConfigurationsManager.getInstance(this);
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
        mState.load();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mState.save();
        return super.onUnbind(intent);
    }


    @Override
    public void onDestroy() {
        Log.e("TEST", "onDestroy");
        mState.reset();
        super.onDestroy();
    }

    public class CustomBinder extends Binder {
        public TrackerService getService(){
            return TrackerService.this;
        }
    }

    /* Tracker Interface
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private String mCurrentSession;
    private RouteRecorderState mState = new RouteRecorderState();

    private final Object finishLock = new Object();
    private final Object trackingLock = new Object();

    private SimpleDateFormat mSDF = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String startTracking()
            throws MissingLocationPermissionsException{

        Log.i(LOG_TAG, "Started tracking");
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
        if (!EasyPermissions.hasPermissions(this, PermissionsConstants.TRACKING_PERMISSIONS))
            throw new MissingLocationPermissionsException();

        //Step 1 - Check if it is already tracking the user
        synchronized (trackingLock){
            if (mState.isTracking || mTracker.isTracking()) {
                Log.w(LOG_TAG, LOG_TAG + " was already active.");
                return null;
            }else {
                mState.setTracking(true);
                mState.save();
            }

        }

        //Step 2.a - Set the base values
        long startTime = System.currentTimeMillis();


        mState.setAutomaticTracking(isAutomatic);
        mState.setSilentStart(isSilent);
        mState.save();

        mTracker.setCurrentTrack(mTrackStorage.createNewTrackSummary(startTime, modality, isAutomatic ? 0 : 2));

        Log.i(LOG_TAG, "Starting a new route @"+mSDF.format(new Date(startTime)));

        //Step 2.b - Reset the idle timer
        mTracker.resetIdleTimer();

        //Step 3 - Analyze question IJsberg
        if(isAutomatic){
            //TODO: do something i dont understand what or why
        }

        String session = mTrackStorage.getNextAvailableId(); //TODO: deprecate this, use mCurrentTrackSummary instead
        mCurrentSession= session;

        mTracker.updateSettings();
        mTracker.startTracking();

        //Step 4 - Register the receiver, which are responsible for handling new locations and activities.
        IntentFilter trackingFilter = new IntentFilter();
        trackingFilter.addAction(TrackingConstants.ActivityRecognition.COLLECT_ACTION);
        trackingFilter.addAction(Constants.tracker.COLLECT_LOCATIONS_ACTION);

        LocalBroadcastManager.getInstance(this).registerReceiver((BroadcastReceiver) mTracker, trackingFilter);

        //Step 5 - Register the receiver, which is responsible for handling idle timeouts.
        IntentFilter timeoutFilter = new IntentFilter(IJsbergTrackingEngine.Extras.IDLE_TIMEOUT_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mTimeoutReceiver, timeoutFilter);

        return mCurrentSession;
    }


    @Override
    public Track stopTracking() {
        return stopTracking(true);
    }

    @Override
    public Track stopTracking(boolean isManual) {

        synchronized (trackingLock) {
            if (!mState.isTracking()) {
                Log.w(LOG_TAG, "No route was started.");
                return null;
            } else {
                mState.setTracking(false);
                mState.save();
            }
        }

        synchronized (finishLock){
            if(mState.isFinishing()){
                Log.w(LOG_TAG, "Already finishing this route");
                return null;
            }else
                mState.setFinishing(true);
        }

        mState.save();

        TrackSummary currentTrack = mTracker.getCurrentTrack();

        //Step 1 - Check if the route was automatic
        //          if so, delete tracks with less than 250m
        if(mState.isAutomaticTracking() && currentTrack.getElapsedDistance() < 250) { //TODO: should not be hardcoded
            Log.w(LOG_TAG, "The track "+currentTrack.getTrackId()+" was ignored because of its size.");
            mTrackStorage.deleteTrack(currentTrack.getTrackId());
            return null;
        }

        //Step 2 - Update the track summary sensing type in case of manual sensing.
        if(isManual){
            mTracker.getCurrentTrack().setSensingType(1);
            mTrackStorage.updateTrackSummaryDistanceAndTime(mTracker.getCurrentTrack());
        }

        //Step 4 - ???
        // Basically, the check if there is last position (why was it not added?), and if there is
        // then the position if added to the TrackSummary and also to the sqlite storage
        if(mTracker.getCurrentLocation() != null){ //TODO: assure that is is the last acquired position and not the FusedLocation one
            ;
        }

        //Step 5 there is a lot of UI and network effort which we deemed inappropriate for the module!

        //Step 6 - The automatic tracking is re-enabled.

        //Step 7 - Stop the timers


        //Step 3 - Stop location and activity updates and unregister the receiver
        boolean wasDeleted = deleteTrackIfIrrelevant(currentTrack.getTrackId());

        if(wasDeleted)
            mTracker.abortTracking();
        else
            mTracker.stopTracking();

        LocalBroadcastManager.getInstance(this).unregisterReceiver((BroadcastReceiver) mTracker);

        //TODO: remover (just for debugging)
        mTrackStorage.dumpTrackSummaryTable();

        synchronized (finishLock){
            mState.setFinishing(false);
            mState.save();
        }

        if(!wasDeleted)
            return mTrackStorage.getCompleteTrack(currentTrack);
        else
            return null;
    }

    private boolean deleteTrackIfIrrelevant(String session){

        if(IS_TESTING){
            Log.w("TESTING", "Skipping the deletion for testing purposes");
            return false;
        }

        Track t;
        try {
             t = mTrackStorage.getTrack(session);
        }catch(RuntimeException e){
            Log.e(LOG_TAG, "Nothing to delete, same as if it was deleted.");
            return true;
        }

        if(t == null || t.getElapsedDistance() <= 15 || t.getTracedTrack().size() <= 5) {
            mTrackStorage.deleteTrack(session);

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
        mTrackStorage.dumpTrackSummaryTable();
        return mTrackStorage.getAllTrackSummaries();
    }

    @Override
    public Track getTracedTrack(String trackId) {
        return mTrackStorage.getTrack(trackId);
    }

    @Override
    public void deleteTracedTrack(String trackId) {
        mTrackStorage.deleteTrack(trackId);
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

        public boolean isFinishing() {
            return isFinishing;
        }

        public void setFinishing(boolean finishing) {
            isFinishing = finishing;
        }

        public void save() {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("STATE", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("isTracking", isTracking);
            editor.putBoolean("isAutomaticTracking", isAutomaticTracking);
            editor.putBoolean("isSilentStart", isSilentStart);
            editor.putBoolean("isFinishing", isFinishing);

            editor.commit();
        }

        public void load(){
            SharedPreferences sharedPreferences =
                    getApplicationContext().getSharedPreferences("STATE", Context.MODE_PRIVATE);


            isTracking = sharedPreferences.getBoolean("isTracking", isTracking);
            isAutomaticTracking = sharedPreferences.getBoolean("isAutomaticTracking", isAutomaticTracking);
            isSilentStart = sharedPreferences.getBoolean("isSilentStart", isSilentStart);
            isFinishing = sharedPreferences.getBoolean("isFinishing", isFinishing);
        }

        public void reset() {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("STATE", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("isTracking", false);
            editor.putBoolean("isAutomaticTracking", false);
            editor.putBoolean("isSilentStart", false);
            editor.putBoolean("isFinishing", false);

            editor.commit();
        }
    }

    /* Timers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private BroadcastReceiver mTimeoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!mState.isTracking()){
                Log.i(LOG_TAG, "IdleTimer complete, however not tracking.");
                return;
            }

            stopTracking();
            mTracker.stopIdleTimer();
        }
    };


}
