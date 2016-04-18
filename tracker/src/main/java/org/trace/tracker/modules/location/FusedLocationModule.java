package org.trace.tracker.modules.location;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.trace.tracker.filter.HeuristicBasedFilter;
import org.trace.tracker.filter.OutlierFilteringLocationQueue;
import org.trace.tracker.modules.ModuleInterface;
import org.trace.tracker.storage.data.TraceLocation;


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
    private OutlierFilteringLocationQueue mLocationQueue;

    private HeuristicBasedFilter mOutlierDetector;
    private float mMinimumAccuracy  = 40f;
    private float mMaximumSpeed     = 55.56f;
    private float mMinimumSatellites= 4;

    public FusedLocationModule(Context ctx, GoogleApiClient client) {
        this.mContext = ctx;
        this.mGoogleApiClient = client;

        this.mLocationQueue = new OutlierFilteringLocationQueue(mContext);
        this.mLocationQueue.addHeuristicRule(new HeuristicBasedFilter.AccuracyBasedHeuristicRule(mMinimumAccuracy));
        this.mLocationQueue.addHeuristicRule(new HeuristicBasedFilter.SpeedBasedHeuristicRule(mMaximumSpeed));
        this.mLocationQueue.addHeuristicRule(new HeuristicBasedFilter.OverlappingLocationHeuristicRule());
        //mOutlierDetector = new HeuristicBasedFilter();
        //mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.AccuracyBasedHeuristicRule(mMinimumAccuracy));
        //mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.SatelliteBasedHeuristicRule(4));
        //mOutlierDetector.addNewHeuristic(new HeuristicBasedFilter.SpeedBasedHeuristicRule(mMaximumSpeed));
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
        //TODO: handle this
        //mOutlierDetector.updateHeuristic(new HeuristicBasedFilter.AccuracyBasedHeuristicRule(mMinimumAccuracy));
    }

    public void setMaximumSpeed(float mMaximumSpeed) {
        this.mMaximumSpeed = mMaximumSpeed;
        //TODO: handle this
        //mOutlierDetector.updateHeuristic(new HeuristicBasedFilter.SpeedBasedHeuristicRule(mMaximumSpeed));
    }

    public void setMinimumSatellites(float mMinimumSatellites) {
        this.mMinimumSatellites = mMinimumSatellites;
    }

    public void activateRemoveOutliers(boolean activate) {
        mLocationQueue.setIsEnabled(activate);
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

        mLocationQueue.addLocation(new TraceLocation(location));

    }

    /* Module Interface
    /* Module Interface
    /* Module Interface
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    @Override
    public void startTracking() {
        if (!isTracking) {

            mLocationQueue.clearQueue();

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    this.mGoogleApiClient,
                    createLocationRequest(),
                    this);

            isTracking = true;
        }
    }

    @Override
    public void stopTracking() {
        if(isTracking) {
            LocationServices.FusedLocationApi.removeLocationUpdates(this.mGoogleApiClient, this);
            //mLocationQueue.clearAndStoreQueue();
            isTracking = false;

        }
    }
}
