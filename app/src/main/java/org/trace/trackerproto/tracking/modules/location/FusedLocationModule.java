package org.trace.trackerproto.tracking.modules.location;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.tracking.filter.HeuristicBasedFilter;
import org.trace.trackerproto.tracking.modules.ModuleInterface;


public class FusedLocationModule implements LocationListener, ModuleInterface {

    protected final static String LOG_TAG = "FusedLocation";

    private final Context mContext;
    private final GoogleApiClient mGoogleApiClient;



    private boolean isTracking = false;

    // Tracking Parameters
    private long mInterval = 10000,
                 mFastInterval = 5000;

    private int mPriority = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private float mMinimumDisplacement = 2f; //meters

    // Outlier Detection Filters && Parameters
    private HeuristicBasedFilter mOutlierDetector;
    private float mMinimumAccuracy  = 40f;
    private float mMaximumSpeed     = 55.56f;
    private float mMinimumSatellites= 4;

    public FusedLocationModule(Context ctx, GoogleApiClient client) {
        this.mContext = ctx;
        this.mGoogleApiClient = client;

        mOutlierDetector = new HeuristicBasedFilter();
        mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.AccuracyBasedHeuristicRule(mMinimumAccuracy));
        //mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.SatelliteBasedHeuristicRule(4));
        mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.SpeedBasedHeuristicRule(mMaximumSpeed));
    }

    public long getInterval() {
        return mInterval;
    }

    public void setInterval(long mInterval) {
        this.mInterval = mInterval;
    }

    public long getFastInterval() {
        return mFastInterval;
    }

    public void setFastInterval(long mFastInterval) {
        this.mFastInterval = mFastInterval;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int mPriority) {
        this.mPriority = mPriority;
    }

    public float getMinimumDisplacement() {
        return mMinimumDisplacement;
    }

    public void setMinimumDisplacement(float mMinimumDisplacement) {
        this.mMinimumDisplacement = mMinimumDisplacement;
    }

    public void setMinimumAccuracy(float mMinimumAccuracy) {
        this.mMinimumAccuracy = mMinimumAccuracy;
    }

    public void setMaximumSpeed(float mMaximumSpeed) {
        this.mMaximumSpeed = mMaximumSpeed;
    }

    public void setMinimumSatellites(float mMinimumSatellites) {
        this.mMinimumSatellites = mMinimumSatellites;
    }

    public boolean isTracking() {
        return isTracking;
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(mInterval);
        mLocationRequest.setFastestInterval(mFastInterval);
        mLocationRequest.setPriority(mPriority);
        mLocationRequest.setSmallestDisplacement(mMinimumDisplacement);

        return mLocationRequest;
    }

    @Override
    public void onLocationChanged(Location location) {

        if(mOutlierDetector.isValidLocation(location)) {
            // Broadcast the location
            Intent localIntent = new Intent(Constants.COLLECT_ACTION);
            localIntent.putExtra(Constants.LOCATION_EXTRA, location);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(localIntent);
        }else
            Log.i(LOG_TAG, "Location discarded as it was identified as an outlier.");

    }

    public void startLocationUpdates() {

        if (!isTracking) {

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    this.mGoogleApiClient,
                    createLocationRequest(),
                    this);

            isTracking = true;
        }
    }

    public void stopLocationUpdates(){

        if(isTracking) {
            LocationServices.FusedLocationApi.removeLocationUpdates(this.mGoogleApiClient, this);
            isTracking = false;
        }
    }



    @Override
    public void startTracking() {
        startLocationUpdates();
    }

    @Override
    public void stopTracking() {
        stopLocationUpdates();
    }
}
