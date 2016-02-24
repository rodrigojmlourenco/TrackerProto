package org.trace.trackerproto.tracking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.store.TRACEStoreApiClient;
import org.trace.trackerproto.tracking.data.Track;
import org.trace.trackerproto.tracking.filter.HeuristicBasedFilter;
import org.trace.trackerproto.tracking.modules.activity.ActivityRecognitionModule;
import org.trace.trackerproto.tracking.modules.location.FusedLocationModule;
import org.trace.trackerproto.tracking.storage.TrackInternalStorage;
import org.trace.trackerproto.tracking.storage.exceptions.UnableToStoreTrackException;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Rodrigo Lourenço on 23/02/2016.
 */
public class TRACETracker extends BroadcastReceiver implements CollectorManager{

    private static final String LOG_TAG = "TRACETracker";

    private static TRACETracker TRACKER = null;

    private Context mContext;
    private GoogleClientManager mGoogleMan;

    private Track track = null;

    //Async
    private Object mLock = new Object();

    //Location Modules
    private LinkedList<Location> mLocationTrace;
    private FusedLocationModule fusedLocationModule = null;
    private HeuristicBasedFilter mOutlierDetector;

    //Activity Modules
    private DetectedActivity mCurrentActivity = null;
    private  LinkedList<DetectedActivity> mActivityTrace;
    private ActivityRecognitionModule activityRecognitionModule = null;

    private TRACETracker(Context context){
        mContext = context;

        mGoogleMan = new GoogleClientManager(mContext);
        mGoogleMan.connect();

        //Location
        mLocationTrace = new LinkedList<>();
        mOutlierDetector = new HeuristicBasedFilter();
        mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.AccuracyBasedHeuristicRule(30));
        mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.SatelliteBasedHeuristicRule(4));
        mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.SpeedBasedHeuristicRule(16.67f));

        //Activity Recognition
        mActivityTrace = new LinkedList<>();
    }

    protected static TRACETracker getTracker(Context ctx){
        if(TRACKER == null)
            TRACKER = new TRACETracker(ctx);

        return TRACKER;
    }


    private void init(){

        fusedLocationModule = new FusedLocationModule(
                mContext,
                mGoogleMan.getApiClient());

        activityRecognitionModule = new ActivityRecognitionModule(
                mContext,
                mGoogleMan.getApiClient());
    }


    @Override
    public void storeLocation(Location location) {

        if(track == null){
            track = new Track(TRACEStoreApiClient.getSessionId(), location);
        }else
            track.addTracedLocation(location);
    }


    public void startLocationUpdates(){
        if(fusedLocationModule==null) init();

        track = null;
        fusedLocationModule.startLocationUpdates();

    }

    public void stopLocationUpdates(){
        fusedLocationModule.stopTracking();

        if(track == null) return;

        track.setSessionId(TRACEStoreApiClient.getSessionId());

        try {
            TrackInternalStorage.storeTracedTrack(mContext, TRACEStoreApiClient.getSessionId(), track);
        } catch (UnableToStoreTrackException e) {
            e.printStackTrace();
        }
    }

    public void startActivityUpdates(){
        if(activityRecognitionModule==null) init();

        activityRecognitionModule.startTracking(1000);
    }

    public void stopActivityUpdates(){
        activityRecognitionModule.stopTracking();
    }

    private void onHandleLocation(Location location){
        Log.e("onHandle", "Location:"+location.toString());

        if(mOutlierDetector.isValidLocation(location)){
            mLocationTrace.add(location);
            storeLocation(location);

            DetectedActivity activity = null;
            synchronized (mLock){
                if(mCurrentActivity != null)
                    activity = mCurrentActivity;
            }

            TRACEStoreApiClient.uploadTrackingInfo(location, activity);
        }
    }

    private void onHandleDetectedActivity(ArrayList<DetectedActivity> detectedActivities){
        Log.e("onHandle", detectedActivities.toString());

        if(detectedActivities.isEmpty()) return;

        DetectedActivity aux = detectedActivities.get(0);

        for(DetectedActivity activity : detectedActivities)
            if (aux.getConfidence() > activity.getConfidence())
                aux = activity;

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
}
