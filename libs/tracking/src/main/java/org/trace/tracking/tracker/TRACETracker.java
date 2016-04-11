package org.trace.tracking.tracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.trace.tracking.TrackingConstants;
import org.trace.tracking.tracker.modules.activity.ActivityConstants;
import org.trace.tracking.tracker.settings.SettingsManager;
import org.trace.tracking.tracker.settings.TrackingProfile;
import org.trace.tracking.tracker.storage.GPXTrackWriter;
import org.trace.tracking.tracker.storage.PersistentTrackStorage;
import org.trace.tracking.tracker.storage.data.SimplifiedTrack;
import org.trace.tracking.tracker.storage.data.Track;

import java.util.List;

/**
 * The TRACETracker is a Service that is responsible for managing the tracking efforts. In particular
 * this service is responsible for tracking the user's location and activity mode.
 */
public class TRACETracker extends Service {

    private final String LOG_TAG = "TRACETracker";

    private Tracker mTracker;

    final Messenger mMessenger = new Messenger(new ClientHandler());

    @Override
    public void onCreate() {
        super.onCreate();
        mTracker = Tracker.getTracker(this);
    }

    /* Service
    /* Service
    /* Service
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Log.d(LOG_TAG, "onBind");

        IntentFilter listenerFilter = new IntentFilter();
        listenerFilter.addAction(ActivityConstants.COLLECT_ACTION);
        listenerFilter.addAction(TrackingConstants.tracker.COLLECT_LOCATIONS_ACTION);

        LocalBroadcastManager.getInstance(this).registerReceiver(mTracker, listenerFilter);
        registerReceiver(mTracker, new IntentFilter(ActivityConstants.COLLECT_ACTION)); //TODO: confirmar se este é necessário


        return mMessenger.getBinder();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTracker);

        unregisterReceiver(mTracker);

        return super.onUnbind(intent);
    }


    /* Service Requests Handling
    /* Service Requests Handling
    /* Service Requests Handling
    ************************************************************************************************
    ************************************************************************************************
    ************************************************************************************************
     */

    /**
     * Operation codes of the methods supported by the TRACETrackerService
     */
    protected interface TRACETrackerOperations {

        int TRACK_LOCATION_ACTION   = 2;
        int UNTRACK_LOCATION_ACTION = 3;

        int TRACK_ACTIVITY_ACTION   = 5;
        int UNTRACK_ACTIVITY_ACTION = 6;

        int TRACK_ACTION            = 7;
        int UNTRACK_ACTION          = 8;

        int LAST_LOCATION_ACTION    = 9;
    }

    private class ClientHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case TRACETrackerOperations.TRACK_ACTION:
                case TRACETrackerOperations.TRACK_LOCATION_ACTION:
                    startTracking(msg);
                    break;
                case TRACETrackerOperations.UNTRACK_ACTION:
                case TRACETrackerOperations.UNTRACK_LOCATION_ACTION:
                    stopTracking();
                    break;
                case TRACETrackerOperations.TRACK_ACTIVITY_ACTION:
                case TRACETrackerOperations.UNTRACK_ACTIVITY_ACTION:
                    break;
                case TRACETrackerOperations.LAST_LOCATION_ACTION:
                    broadcastCurrentLocation();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void startTracking(Message msg){

        Bundle data = msg.getData();
        String session = data.getString(TrackingConstants.tracker.SESSION_EXTRA);
        boolean isValid = data.getBoolean(TrackingConstants.tracker.VALID_SESSION_EXTRA);


        mTracker.setSession(session, isValid);
        mTracker.updateSettings();
        mTracker.startLocationUpdates();
        mTracker.startActivityUpdates();
    }

    private void stopTracking(){
        mTracker.stopLocationUpdates();
        mTracker.stopActivityUpdates();
    }

    private void broadcastCurrentLocation(){

        Location location = mTracker.getCurrentLocation();

        Intent locationBroadcast = new Intent();
        locationBroadcast.setAction(org.trace.tracking.TrackingConstants.tracker.BROADCAST_LOCATION_ACTION);
        locationBroadcast.putExtra(TrackingConstants.tracker.BROADCAST_LOCATION_EXTRA, location);

        LocalBroadcastManager.getInstance(this).sendBroadcast(locationBroadcast);
    }


    /* Trace Tracker Client Abstraction Layer
    /* Trace Tracker Client Abstraction Layer
    /* Trace Tracker Client Abstraction Layer
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * The TraceTrackerClient was designed to ease communication with the TraceTracker service. It
     * operates as an abstraction layer, that masks the communication between the activities and
     * the TraceTracker service.
     */
    public static class Client {

        private static void sendRequest(Messenger messenger, Message msg){
            try {
                messenger.send(msg);
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
         * @param messenger Messenger object for Activity Service communication
         * @param sessionId The current session identifier
         * @param isValidSession Boolean denoting if the session identifier is local of remote.
         */
        public static void startTracking(Messenger messenger, String sessionId, boolean isValidSession){

            Bundle data = new Bundle();
            data.putString(TrackingConstants.tracker.SESSION_EXTRA, sessionId);
            data.putBoolean(TrackingConstants.tracker.VALID_SESSION_EXTRA, isValidSession);

            Message msg = Message.obtain(null, TRACETrackerOperations.TRACK_ACTION);
            msg.setData(data);

            sendRequest(messenger, msg);
        }

        /**
         * Stops the location and activity tracking modules.
         */
        public static void stopTracking(Messenger messenger){
            Message msg = Message.obtain(null, TRACETrackerOperations.UNTRACK_ACTION);
            sendRequest(messenger, msg);
        }

        /**
         * Request the most current location. Because the communication is made with the service
         * this method does not actually returns the location. Instead, the location is broadcasted
         * through the LocalBroadcastManager with the TrackingConstants.BROADCAST_LOCATION_ACTION, and
         * under the TrackingConstants.BROADCAST_LOCATION_EXTRA extra.
         * <br>
         * <br><b>Note:</b> It is important to assure in API version above 23, that the ACCESS_FINE_LOCATION
         * and ACCESS_COARSE_LOCATION have been granted, and otherwise, request them.
         * <br>
         * <pre>
         *     {@code
         *     mLocationReceiver = new BroadcastReceiver(){
         *      {@literal @}Override
         *      public void onReceive(Context context, Intent intent) {
         *          Location mCurrentLocation = intent.getParcelableExtra(TrackingConstants.tracker.BROADCAST_LOCATION_EXTRA);
         *          //Do something with the location
         *          }
         *     };
         *
         *     IntentFilter locationFilter = new IntentFilter();
         *     locationFilter.addAction(TrackingConstants.tracker.BROADCAST_LOCATION_ACTION);
         *
         *     LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, locationFilter);
         *     }
         * </pre>
         */
        public static void getLastLocation(Messenger messenger){
            Message msg = Message.obtain(null, TRACETrackerOperations.LAST_LOCATION_ACTION);
            sendRequest(messenger, msg);
        }

        /**
         * Updates the tracking profile settings. These define the sampling rates used, how outliers
         * are identified, among other information.
         *
         * @param profile The tracking profile.
         *
         * @see TrackingProfile
         */
        public static void updateTrackingProfile(Context context, TrackingProfile profile){
            SettingsManager settingsManager = SettingsManager.getInstance(context);
            settingsManager.saveTrackingProfile(profile);
        }

        /**
         * Fetches the current tracking profile.
         * @return The current TrackingProfile
         * @see TrackingProfile
         */
        public static TrackingProfile getCurrentTrackingProfile(Context context){
            SettingsManager settingsManager = SettingsManager.getInstance(context);
            return settingsManager.getTrackingProfile();
        }


        /**
         * Fetches all the stored tracks as a list of SimplifiedTracks
         * @return SimplifiedTrack list
         * @see SimplifiedTrack
         */
        public static List<SimplifiedTrack> getAllStoredTracks(Context context){
            PersistentTrackStorage storage = new PersistentTrackStorage(context);
            return storage.getTracksSessions();
        }

        /**
         * Fetches a track identified by its session identifier as a complete track.
         * @param sessionId The track's identifier
         * @return The Track
         *
         * @see Track
         */
        public static Track getStoredTrack(Context context, String sessionId){
            PersistentTrackStorage storage = new PersistentTrackStorage(context);
            return storage.getTrack(sessionId);
        }


        /**
         * Fetches the number of tracks currently stored in memory.
         * @return The number of stored tracks.
         */
        public static int getStoredTracksCount(Context context){
            PersistentTrackStorage storage = new PersistentTrackStorage(context);
            return  storage.getTracksCount();
        }

        /**
         * Removes the track identified by its session identifier from memory.
         * @param sessionId The track's session identifier.
         */
        public static void deleteStoredTrack(Context context, String sessionId){
            PersistentTrackStorage storage = new PersistentTrackStorage(context);
            storage.deleteTrackById(sessionId);
        }



        /**
         * Exports the track identified by its session identifier to external storage. The track
         * is exported as a gpx file, which conforms to the GPS Exchange Format.
         *
         * <br><b>Note:</b> It is important to assure in API version above 23, that the READ and
         * WRITE permissions for external storage have been granted.
         *
         * @param track The track to be exported
         */
        public static String exportStoredTrackToExternalMemory(Context context, Track track){
            return GPXTrackWriter.exportAsGPX(context, track);
        }
    }
}
