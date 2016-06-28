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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;

import org.trace.tracker.google.GoogleClientManager;
import org.trace.tracker.modules.activity.ActivityConstants;
import org.trace.tracker.modules.activity.ActivityRecognitionModule;
import org.trace.tracker.modules.location.FusedLocationModule;
import org.trace.tracker.settings.ConfigurationProfile;
import org.trace.tracker.settings.ConfigurationsManager;
import org.trace.tracker.storage.data.TraceLocation;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class IJsbergTracker extends BroadcastReceiver implements CollectorManager {

    private static final String LOG_TAG = "IJsbergTracker";

    private static IJsbergTracker TRACKER = null;

    private Context mContext;
    private GoogleClientManager mGoogleMan; //TODO: this is dangerous cause it hides the possibility of a disconnected googleclientapi


    private IJsbergTracker(Context context){
        mContext = context;

        mGoogleMan = new GoogleClientManager(mContext);
        mGoogleMan.connect();

        //Settings
        mSettingsManager = ConfigurationsManager.getInstance(context);
    }

    protected static IJsbergTracker getTracker(Context ctx){

        synchronized (IJsbergTracker.class) {
            if (TRACKER == null)
                TRACKER = new IJsbergTracker(ctx);
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

        if(intent.hasExtra(TrackingConstants.tracker.LOCATION_EXTRA)) {

            TraceLocation location = intent.getParcelableExtra(TrackingConstants.tracker.LOCATION_EXTRA);
            onHandleLocation(location);


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
     * IJsberg positionChanged@RouteRecorder.cs:357
     * @param location
     */
    private void onHandleLocation(TraceLocation location){

        if(location != null || !mLocationModule.isTracking()){
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
        if(mCurrentActivity != null)
            location.setActivityMode(mCurrentActivity);


        //Step 3 -


        if(isAcceptableAccuracy(location)){

            if(mLastKnownLocation == null) { //1st recorded position

                //Save the route summary in a database with the new point


                ((RouteRecorderService)mContext).resetIdleTimer();
                mLastKnownLocation = location;

            }else if(isInterestingNewPosition(location, (TraceLocation) mLastKnownLocation)){ //This is a new location

                //Update the route summary in a database with the new point

                ((RouteRecorderService)mContext).resetIdleTimer();
                mLastKnownLocation = location;
            }

        }


    }

    private boolean isAcceptableAccuracy(TraceLocation position){
        return position.getAccuracy() <= 50; //TODO: should not be hardcoded
    }

    private boolean isInterestingNewPosition(TraceLocation position, TraceLocation lastPosition) {
        if (lastPosition != null)
            return lastPosition.distanceTo(position) > 100 /* TODO: should not be hardcoded */
                || position.isCorner(lastPosition.getBearing()); /*TODO: may cause infinite interesting points */
        else
            return false;

    }

    public void startLocationUpdates(){
        if(mLocationModule ==null) init();

        //travelledDistance = 0;
        mLocationModule.startTracking();
    }

    public void stopLocationUpdates(){
        mLocationModule.stopTracking();
    }

    public void startActivityUpdates(){
        if(mActivityRecognitionModule ==null) init();
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
    public void storeLocation(Location location) {
        throw new UnsupportedOperationException();
    }

    /* Activity Tracking
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private DetectedActivity mCurrentActivity = null;
    private ActivityRecognitionModule mActivityRecognitionModule = null;

    public void stopActivityUpdates(){
        mActivityRecognitionModule.stopTracking();
    }

    private void onHandleDetectedActivity(ArrayList<DetectedActivity> detectedActivities){
        //TODO:
    }


    /* Session Management
     * TODO: deprecate all dependencies to sessions
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    @Deprecated
    private String mSessionId = null;
    @Deprecated
    private boolean isValidSession = false;

    @Deprecated
    public void setSession(String session, boolean isValid){
        teardownSession();
        mSessionId = session;
        isValidSession = isValid;
    }

    @Deprecated
    private void teardownSession(){
        mSessionId = null;
        isValidSession = false;
    }

    /* Configuration Profile Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private ConfigurationsManager mSettingsManager;

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

        //TODO: this should be in the ActivityRecognitionModule
        //mMinimumActivityConfidence = profile.getActivityMinimumConfidence();
    }
}
