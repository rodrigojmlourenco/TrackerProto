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

package org.trace.tracker.tracking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;

import org.trace.tracker.*;
import org.trace.tracker.google.GoogleClientManager;
import org.trace.tracker.tracking.modules.activity.ActivityConstants;
import org.trace.tracker.tracking.modules.activity.ActivityRecognitionModule;
import org.trace.tracker.tracking.modules.location.FusedLocationModule;
import org.trace.tracker.settings.ConfigurationProfile;
import org.trace.tracker.settings.ConfigurationsManager;
import org.trace.tracker.storage.PersistentTrackStorage;
import org.trace.tracker.storage.data.TraceLocation;
import org.trace.tracker.storage.data.TrackSummary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class IJsbergTrackingEngine extends BroadcastReceiver implements TrackingEngine {

    private static final String LOG_TAG = "IJsbergTrackingEngine";
    private static final boolean IS_TESTING = false; //TODO: carefull with this

    private static IJsbergTrackingEngine TRACKER = null;
    private final PersistentTrackStorage mTrackStorage;

    private Context mContext;
    private GoogleClientManager mGoogleMan; //TODO: this is dangerous cause it hides the possibility of a disconnected googleclientapi

    //State
    private TrackSummary mCurrentTrack;

    private IJsbergTrackingEngine(Context context){
        mContext = context;

        mGoogleMan = new GoogleClientManager(mContext);
        mGoogleMan.connect();

        //Settings
        mSettingsManager = ConfigurationsManager.getInstance(context);

        mTrackStorage = new PersistentTrackStorage(context);

        mIdleTimer = mExecutorService.schedule(new IdleElapsed(), 20, TimeUnit.MINUTES); //TODO: should not be hardcoded
    }

    public static IJsbergTrackingEngine getTracker(Context ctx){

        synchronized (IJsbergTrackingEngine.class) {
            if (TRACKER == null)
                TRACKER = new IJsbergTrackingEngine(ctx);
        }

        return TRACKER;
    }


    private void init(){

        mLocationModule = new FusedLocationModule(
                mContext,
                mGoogleMan.getApiClient());

        mActivityRecognitionModule = new ActivityRecognitionModule(
                mContext,
                mGoogleMan.getApiClient());
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.hasExtra(Constants.tracker.LOCATION_EXTRA)) {

            Log.d(LOG_TAG, "new location");//TODO: remove

            Location location = intent.getParcelableExtra(Constants.tracker.LOCATION_EXTRA);
            onHandleLocation(new TraceLocation(location));


        }else if(intent.hasExtra(ActivityConstants.ACTIVITY_EXTRA)) {

            ArrayList<DetectedActivity> updatedActivities =
                    intent.getParcelableArrayListExtra(ActivityConstants.ACTIVITY_EXTRA);

            onHandleDetectedActivity(updatedActivities);
        }
    }

    /* Location Tracking
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private Location mLastKnownLocation = null;
    private final Object mLocationLock = new Object();
    private FusedLocationModule mLocationModule = null;

    /**
     * IJsberg positionChanged@Tracker.cs:357
     * @param location
     */
    private void onHandleLocation(final TraceLocation location){

        if(location == null || !mLocationModule.isTracking()){

            Log.w(LOG_TAG, location == null ? "Null position" : "NOT supposed to be tracking");
            return;
        }else
            Log.i(LOG_TAG, "New position "+location.getTime());

        // Step 1 - Correct the speed and bearing values for atypical new positions
        if(mLastKnownLocation != null && location.getSpeed() <= 0){

            float distance = mLastKnownLocation.distanceTo(location);
            long timeDiff  = location.getTime() - mLastKnownLocation.getTime();
            timeDiff = TimeUnit.MILLISECONDS.toSeconds(timeDiff);

            //Step 1a - Correct the speed to a calculated value (m/s)
            if(distance == 0 || timeDiff == 0){
                location.setSpeed(0);
            }else{
                location.setSpeed(distance / timeDiff);
            }

            //Step 1b - Correct the heading to a calculated value
            location.setBearing(mLastKnownLocation.bearingTo(location));

        }

        //Step 2 - Update the current activity
        synchronized (mActivityLock) {
            if (mCurrentActivity != null)
                location.setActivityMode(mCurrentActivity);
        }


        //Step 3 -
        if(isAcceptableAccuracy(location)){

            //1st recorded position
            if(mLastKnownLocation == null) {

                mCurrentTrack.setStartTimestamp(location.getTime());
                mCurrentTrack.setStoppedTimestamp(location.getTime());
                mCurrentTrack.setElapsedDistance(0);
                mCurrentTrack.setFromLocation(location);

                mTrackStorage.updateTrackSummary(mCurrentTrack);
                mTrackStorage.storeLocation(location, mCurrentTrack.getTrackId());

                //Get the semantic address of the start location
                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if(addresses != null && addresses.size() > 0){
                            Address address = addresses.get(0);
                            ArrayList<String> addressFragments = new ArrayList<String>();

                            // Fetch the address lines using getAddressLine,
                            // join them, and send them to the thread.
                            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                                addressFragments.add(address.getAddressLine(i));
                            }
                            String sLocation = TextUtils.join(", ", addressFragments);
                            Log.i(LOG_TAG,sLocation );

                            mCurrentTrack.setSemanticFromLocation(sLocation);
                            mTrackStorage.updateTrackSummary(mCurrentTrack);

                        }
                    }
                });

                resetIdleTimer();
                mLastKnownLocation = location;

            }else if(isInterestingNewPosition(location, (TraceLocation) mLastKnownLocation)){ //This is a new location


                //Update the route summary in a database with the new point
                double travelledDistance = mCurrentTrack.getElapsedDistance() + mLastKnownLocation.distanceTo(location);
                mCurrentTrack.setElapsedDistance(travelledDistance);
                mCurrentTrack.setStoppedTimestamp(location.getTime());

                mTrackStorage.updateTrackSummary(mCurrentTrack);
                mTrackStorage.storeLocation(location, mCurrentTrack.getTrackId());

                resetIdleTimer();
                mLastKnownLocation = location;
            }
        }
    }

    private boolean isAcceptableAccuracy(TraceLocation position){
        return position.getAccuracy() <= 50; //TODO: should not be hardcoded
    }

    private boolean isInterestingNewPosition(TraceLocation position, TraceLocation lastPosition) {

        if(IS_TESTING) {
            Log.w(LOG_TAG, "IS_TESTING: forcing location to interesting");
            return true;
        }

        if (lastPosition != null)
            return lastPosition.distanceTo(position) > 100 /* TODO: should not be hardcoded */
                    || position.isCorner(lastPosition.getBearing()); /*TODO: may cause infinite interesting points */
        else
            return false;

    }

    public void startLocationUpdates(){
        if(mLocationModule ==null) init();
        mLocationModule.startTracking();
    }

    public void stopLocationUpdates(){
        mLocationModule.stopTracking();
    }

    public void startActivityUpdates(){

        if(mActivityRecognitionModule ==null)
            init();

        mActivityRecognitionModule.startTracking();
    }

    private boolean isFreshLocation(Location location){
        long timeDiff = System.currentTimeMillis() - location.getTime();
        return timeDiff <= 30*1000; //30s
    }

    private Location getLastKnownLocation(){

        if(mGoogleMan.getApiClient().isConnected())
            return LocationServices.FusedLocationApi.getLastLocation(mGoogleMan.getApiClient());
        else
            return null;

    }


    /* Activity Tracking
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private final Object mActivityLock = new Object();
    private DetectedActivity mCurrentActivity = null;
    private ActivityRecognitionModule mActivityRecognitionModule = null;
    private ActivityState mActivityState = new ActivityState();


    public void stopActivityUpdates(){
        mActivityRecognitionModule.stopTracking();
    }

    private void onHandleDetectedActivity(ArrayList<DetectedActivity> detectedActivities){
        if(detectedActivities.isEmpty()) return;

        DetectedActivity aux = detectedActivities.get(0);

        for(DetectedActivity activity : detectedActivities)
            if (aux.getConfidence() > activity.getConfidence())
                aux = activity;

        if(aux.getConfidence() < mActivityRecognitionModule.getMinimumConfidence()) {
            String activityName = ActivityRecognitionModule.getActivityString(aux.getType());
            Log.d(LOG_TAG, "Confidence on the activity '"+activityName+"' is too low, keeping the previous...");
            return;
        }

        /*
         * TODO: ON_FOOT may either be running or walking, so in that case it should take into account the previous
         * TODO: compute the mode modality
         */
        mActivityState.updateState(aux);


        synchronized (mActivityLock) {
            mCurrentActivity = aux;
        }
    }

    public class ActivityState{

        private boolean wasUpdated;
        private int[] activitiesCounter = new int[7];

        public ActivityState(){
            Arrays.fill(activitiesCounter, 0);
            wasUpdated = false;
        }

        public void updateState(DetectedActivity activity){
            int index = ActivityConstants.getActivityIndex(activity);
            activitiesCounter[index]++;
            wasUpdated = true;
        }

        public int getModeActivity(){

            if(!wasUpdated) return -1;

            int largest = activitiesCounter[0];
            int largestIndex = 0;

            for(int i = 0; i < activitiesCounter.length; i++)
            {
                if(activitiesCounter[i] > largest) {
                    largest = activitiesCounter[i];
                    largestIndex = i;
                }
            }

            return largestIndex;
        }
    }

    /* Timers
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private ScheduledFuture mIdleTimer;
    private ScheduledExecutorService mExecutorService = Executors.newScheduledThreadPool(1);


    protected void startIdleTimer(){
        mIdleTimer = mExecutorService.schedule(new IdleElapsed(), 20, TimeUnit.MINUTES);
    }



    private class IdleElapsed implements Runnable {
        @Override
        public void run() {

            if(!mLocationModule.isTracking()){
                Log.i(LOG_TAG, "IdleTimer complete, however not tracking.");
                return;
            }

            stopIdleTimer();
            LocalBroadcastManager.getInstance(mContext)
                    .sendBroadcast(new Intent(Extras.IDLE_TIMEOUT_ACTION));

        }
    }

    /* Tracking Engine
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private ConfigurationsManager mSettingsManager;

    @Override
    public void setCurrentTrack(TrackSummary summary) {
        mCurrentTrack = summary;
    }



    @Override
    public TrackSummary getCurrentTrack(){
        return  mCurrentTrack;
    }

    @Override
    public Location getCurrentLocation() {

        //Scenario 1 - There is a current location and its fresh
        synchronized (mLocationLock) {
            if (mLastKnownLocation != null && isFreshLocation(mLastKnownLocation))
                return mLastKnownLocation;
        }

        //Scenario 2 - There is no current location or it's not fresh
        //              Using the LocationServices the last known location is retrieved.
        Location lastKnown = getLastKnownLocation();
        if(lastKnown != null && isFreshLocation(lastKnown)){
            synchronized (mLocationLock){

                if(mLastKnownLocation != null
                        && mLastKnownLocation.getTime() >= lastKnown.getTime()) {

                    mLastKnownLocation = lastKnown;

                }

                return lastKnown;
            }
        }

        return lastKnown;

        /* TODO: este cenário pode levar a race conditions
        // Scenario 3 - Both scenarios failed
        //              Turn on the FusedLocationModule and wait for mLastKnownLocation to be set
        boolean await = true;
        startLocationUpdates();

        do {
            synchronized (mLocationLock) {
                await = mLastKnownLocation != null && isFreshLocation(mLastKnownLocation);
            }
        }while (await);

        stopLocationUpdates();
        return mLastKnownLocation;
        */
    }

    @Override
    public void startTracking() {
        startActivityUpdates();
        startLocationUpdates();
    }

    @Override
    public void stopTracking() {
        abortTracking();
    }

    @Override
    public void abortTracking() {
        stopLocationUpdates();
        stopActivityUpdates();
        stopIdleTimer();
    }

    @Override
    public boolean isTracking() {
        if(mLocationModule != null){
            return mLocationModule.isTracking();
        }else
            return false;
    }

    @Override
    public void updateSettings() {

        ConfigurationProfile profile = mSettingsManager.getTrackingProfile();

        if(mLocationModule ==null) init();

        mLocationModule.setInterval(profile.getLocationInterval());
        mLocationModule.setFastInterval(profile.getLocationFastInterval());
        mLocationModule.setMinimumDisplacement(profile.getLocationDisplacementThreshold());
        mLocationModule.setMinimumAccuracy(profile.getLocationMinimumAccuracy());
        mLocationModule.setPriority(profile.getLocationTrackingPriority());
        mLocationModule.setMaximumSpeed(profile.getLocationMaximumSpeed());
        mLocationModule.activateRemoveOutliers(profile.isActiveOutlierRemoval());

        if(mActivityRecognitionModule ==null) init();
        mActivityRecognitionModule.setInterval(profile.getActivityInterval());
        mActivityRecognitionModule.setMinimumConfidence(profile.getActivityMinimumConfidence());
    }

    @Override
    public int getModeActivity() {


        return mActivityState.getModeActivity();
    }

    @Override
    public void resetIdleTimer(){
        stopIdleTimer();
        startIdleTimer();
    }

    @Override
    public void stopIdleTimer(){
        mIdleTimer.cancel(true);
    }

    /* Others
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    public interface Extras {
        String IDLE_TIMEOUT_ACTION = "IDLE_TIMEOUT_ACTION";
    }


}
