package org.trace.trackerproto.tracking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.settings.SettingsManager;
import org.trace.trackerproto.settings.TrackingProfile;
import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.modules.activity.ActivityConstants;
import org.trace.trackerproto.tracking.modules.activity.ActivityRecognitionModule;
import org.trace.trackerproto.tracking.modules.location.FusedLocationModule;
import org.trace.trackerproto.tracking.storage.TrackInternalStorage;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToStoreTrackException;

import java.util.ArrayList;
import java.util.LinkedList;

public class Tracker extends BroadcastReceiver implements CollectorManager{

    private static final String LOG_TAG = "Tracker";

    private static Tracker TRACKER = null;

    private Context mContext;
    private GoogleClientManager mGoogleMan;

    private Track track = null;
    private Location mCurrentLocation = null;
    private final Object mLocationLock = new Object();

    //Async
    private final Object mLock = new Object();

    //Location Modules
    private LinkedList<Location> mLocationTrace;
    private FusedLocationModule mFusedLocationModule = null;


    //Activity Modules
    private DetectedActivity mCurrentActivity = null;
    private  LinkedList<DetectedActivity> mActivityTrace;
    private ActivityRecognitionModule mActivityRecognitionModule = null;

    //Outlier Filters Parameters
    private int mMinimumActivityConfidence;

    //Settings
    private SettingsManager mSettingsManager;

    private Tracker(Context context){
        mContext = context;

        mGoogleMan = new GoogleClientManager(mContext);
        mGoogleMan.connect();

        //Settings
        mSettingsManager = SettingsManager.getInstance(context);

        //Location
        mLocationTrace = new LinkedList<>();

        //Activity Recognition
        mActivityTrace = new LinkedList<>();
    }

    protected static Tracker getTracker(Context ctx){
        if(TRACKER == null)
            TRACKER = new Tracker(ctx);

        return TRACKER;
    }


    private void init(){

        mFusedLocationModule = new FusedLocationModule(
                mContext,
                mGoogleMan.getApiClient());

        mActivityRecognitionModule = new ActivityRecognitionModule(
                mContext,
                mGoogleMan.getApiClient());
    }


    public void updateSettings() {
        TrackingProfile profile = mSettingsManager.getTrackingProfile();


        if(mFusedLocationModule ==null) init();
        mFusedLocationModule.setInterval(profile.getLocationInterval());
        mFusedLocationModule.setFastInterval(profile.getLocationFastInterval());
        mFusedLocationModule.setMinimumDisplacement(profile.getLocationDisplacementThreshold());
        mFusedLocationModule.setMinimumAccuracy(profile.getLocationMinimumAccuracy());
        mFusedLocationModule.setPriority(profile.getLocationTrackingPriority());
        mFusedLocationModule.setMaximumSpeed(profile.getLocationMaximumSpeed());

        if(mActivityRecognitionModule ==null) init();
        mActivityRecognitionModule.setInterval(profile.getArInterval());
        mActivityRecognitionModule.setMinimumConfidence(profile.getActivityMinimumConfidence());

        //TODO: this should be in the ActivityRecognitionModule
        mMinimumActivityConfidence = profile.getActivityMinimumConfidence();
    }


    @Override
    public void storeLocation(Location location) {

        if(track == null)
            track = new Track(TRACEStoreApiClient.getSessionId(), location);
        else
            track.addTracedLocation(location, (mCurrentActivity == null ? "" : mCurrentActivity.toString()));


        synchronized (mLocationLock) {
            mCurrentLocation = location;
        }
    }


    public void startLocationUpdates(){
        if(mFusedLocationModule ==null) init();

        track = null;

        mFusedLocationModule.startTracking();

    }

    public void stopLocationUpdates(){

        mFusedLocationModule.stopTracking();

        if(track == null) return;

        track.setSessionId(TRACEStoreApiClient.getSessionId());

        try {
            TrackInternalStorage.storeTracedTrack(mContext, TRACEStoreApiClient.getSessionId(), track);
        } catch (UnableToStoreTrackException e) {
            e.printStackTrace();
        }
    }

    public void startActivityUpdates(){
        if(mActivityRecognitionModule ==null) init();

        mActivityRecognitionModule.startTracking();
    }

    public void stopActivityUpdates(){
        mActivityRecognitionModule.stopTracking();
    }



    private void onHandleLocation(Location location){
        Log.e("onHandle", "Location:" + location.toString());

        mLocationTrace.add(location);
        storeLocation(location);

        DetectedActivity activity = null;
        synchronized (mLock){
            if(mCurrentActivity != null)
                activity = mCurrentActivity;
        }

        TRACEStoreApiClient.uploadTrackingInfo(location, activity);
    }

    private void onHandleDetectedActivity(ArrayList<DetectedActivity> detectedActivities){
        Log.e("onHandle", detectedActivities.toString());

        if(detectedActivities.isEmpty()) return;

        DetectedActivity aux = detectedActivities.get(0);

        for(DetectedActivity activity : detectedActivities)
            if (aux.getConfidence() > activity.getConfidence())
                aux = activity;

        if(aux.getConfidence() < mMinimumActivityConfidence) {
            String activityName = ActivityConstants.getActivityString(mContext, aux.getType());
            Log.d(LOG_TAG, "Confidence on the activity '"+activityName+"' is too low, keeping the previous...");
            return;
        }

        synchronized (mLock) {
            mCurrentActivity = aux;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.hasExtra(Constants.LOCATION_EXTRA)) {

            Location location = intent.getParcelableExtra(Constants.LOCATION_EXTRA);
            onHandleLocation(location);

        }else if(intent.hasExtra(Constants.ACTIVITY_EXTRA)) {

            ArrayList<DetectedActivity> updatedActivities =
                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);

            onHandleDetectedActivity(updatedActivities);
        }


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
            if (mCurrentLocation != null && isFreshLocation(mCurrentLocation))
                return mCurrentLocation;
        }

        //Scenario 2 - There is no current location or it's not fresh
        //              Using the LocationServices the last known location is retrieved.
        Location lastKnown = getLastKnownLocation();
        if(lastKnown != null && isFreshLocation(lastKnown)){
            synchronized (mLocationLock){

                if(mCurrentLocation != null
                    && mCurrentLocation.getTime() >= lastKnown.getTime()) {

                    mCurrentLocation = lastKnown;

                }

                return lastKnown;
            }
        }


        // Scenario 3 - Both scenarios failed
        //              Turn on the FusedLocationModule and wait for mCurrentLocation to be set
        boolean await = true;
        startLocationUpdates();

        do {
            synchronized (mLocationLock) {
                await = mCurrentLocation != null && isFreshLocation(mCurrentLocation);
            }
        }while (await);

        stopLocationUpdates();

        return mCurrentLocation;
    }
}
