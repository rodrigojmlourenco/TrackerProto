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
import android.content.IntentFilter;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import org.trace.tracker.settings.ConfigurationProfile;
import org.trace.tracker.settings.ConfigurationsManager;
import org.trace.tracker.storage.PersistentTrackStorage;
import org.trace.tracker.storage.data.TraceLocation;
import org.trace.tracker.storage.data.Track;
import org.trace.tracker.storage.data.TrackSummary;

import java.util.List;

public class Tracker {

    private Context mContext;
    private Messenger mMessenger;
    private LocationBroadcastReceiver mLocationBroadcastReceiver;

    //Volatile Location Storage
    private TraceLocation mCurrentLocation;
    private final Object locationQueueLock = new Object();

    //Volatile Track Information
    private String mCurrentTrack = null;
    private final Object trackLock = new Object();

    //Persistent Track Storage
    private PersistentTrackStorage mTrackStorage;

    //Tracing Configuration Management
    private ConfigurationsManager mSettingsManager;

    private Tracker(Context context, Messenger messenger){

        mContext    = context;
        mMessenger  = messenger;

        mCurrentLocation= null;
        mTrackStorage   = new PersistentTrackStorage(mContext);
        mSettingsManager= ConfigurationsManager.getInstance(mContext);

        mLocationBroadcastReceiver = new LocationBroadcastReceiver();
    }

    private static Tracker TRACKER = null;

    public static Tracker getInstance(Context context, Messenger messenger){
        synchronized (Tracker.class){
            if(TRACKER == null)
                TRACKER = new Tracker(context,messenger);
        }

        return TRACKER;
    }
    private void sendRequest(Message msg){

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiates the location and activity tracking modules.
     * <br>
     * <br><b>Note:</b> It is important to assure in API version above 23, that the ACCESS_FINE_LOCATION
     * and ACCESS_COARSE_LOCATION have been granted, and otherwise, request them.
     *
     */
    public void startTracking(){

        String id = mTrackStorage.getNextAvailableId();
        synchronized (trackLock){
            mCurrentTrack = id;
        }

        Message msg = Message.obtain(null, TRACETrackerService.TRACETrackerOperations.TRACK_ACTION);
        sendRequest(msg);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mLocationBroadcastReceiver,
                mLocationBroadcastReceiver.getLocationBroadcastIntentFilter());
    }


    /**
     * Stops the location and activity tracking modules.
     */
    public String stopTracking(){

        Message msg = Message.obtain(null, TRACETrackerService.TRACETrackerOperations.UNTRACK_ACTION);
        sendRequest(msg);

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mLocationBroadcastReceiver);

        String id;

        synchronized (trackLock){
            id = mCurrentTrack;
            mCurrentTrack = null;
        }

        return id;
    }

    /**
     * Request the most current location.
     */
    public TraceLocation getLastLocation(){

        Message msg = Message.obtain(null, TRACETrackerService.TRACETrackerOperations.LAST_LOCATION_ACTION);
        sendRequest(msg);

        synchronized (locationQueueLock) {
            return mCurrentLocation;
        }
    }

    /**
     * TODO: limpa tudo...
     * This method should be invoked when the Tracker is no longer required for the forseeable
     * future, <i>e.g.</i> before the application closes. The method will terminate any pending
     * connections and open resources.
     */
    public void teardown(){

    }

    /* Tracking Configuration Management
    /* Tracking Configuration Management
    /* Tracking Configuration Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * Updates the tracking profile settings. These define the sampling rates used, how outliers
     * are identified, among other information.
     *
     * @param profile The tracking profile.
     *
     * @see ConfigurationProfile
     */
    public void updateTrackingProfile(Context context, ConfigurationProfile profile){
        mSettingsManager.saveTrackingProfile(profile);
    }

    /**
     * Fetches the current tracking profile.
     * @return The current ConfigurationProfile
     * @see ConfigurationProfile
     */
    public ConfigurationProfile getCurrentTrackingProfile(Context context){
        return mSettingsManager.getTrackingProfile();
    }

    /* Track Storage Management
    /* Track Storage Management
    /* Track Storage Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * Fetches all the stored tracks as a list of SimplifiedTracks
     * @return TrackSummary list
     * @see TrackSummary
     */
    @Deprecated
    public List<TrackSummary> getAllTracedTracks(Context context){
        //return mTrackStorage.getTracksSessions();
        throw new RuntimeException("getAllTracedTracks@Tracker is deprecated");
    }

    /**
     * Fetches a track identified by its session identifier as a complete track.
     * @param sessionId The track's identifier
     * @return The Track
     *
     * @see Track
     */
    public Track getTracedTrack(Context context, String sessionId){
        return mTrackStorage.getTrack(sessionId);
    }

    /**
     * Removes the track identified by its session identifier from memory.
     * @param sessionId The track's session identifier.
     */
    public void deleteTracedTrack(Context context, String sessionId){
        mTrackStorage.deleteTrackById(sessionId);
    }


    /* Location Broadcast Listener
    /* Location Broadcast Listener
    /* Location Broadcast Listener
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            TraceLocation location = null;

            if (intent.hasExtra(TrackingConstants.tracker.BROADCAST_LOCATION_EXTRA)) {
                location = intent.getParcelableExtra(TrackingConstants.tracker.BROADCAST_LOCATION_EXTRA);
            } else if (intent.hasExtra(TrackingConstants.tracker.LOCATION_EXTRA)){
                location = intent.getParcelableExtra(TrackingConstants.tracker.LOCATION_EXTRA);
            }

            if(location != null)
                synchronized (locationQueueLock){
                    mCurrentLocation = location;
                }
        }

        public IntentFilter getLocationBroadcastIntentFilter(){
            IntentFilter filter = new IntentFilter();
            filter.addAction(TrackingConstants.tracker.BROADCAST_LOCATION_ACTION);
            filter.addAction(TrackingConstants.tracker.COLLECT_LOCATIONS_ACTION);
            return filter;
        }
    }
}