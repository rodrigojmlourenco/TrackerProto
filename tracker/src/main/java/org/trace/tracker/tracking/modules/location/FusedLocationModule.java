package org.trace.tracker.tracking.modules.location;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.trace.tracker.Constants;
import org.trace.tracker.tracking.modules.ModuleInterface;


public class FusedLocationModule implements LocationListener, ModuleInterface {

    protected final static String LOG_TAG = "FusedLocation";

    private final Context mContext;
    private final GoogleApiClient mGoogleApiClient;


    public FusedLocationModule(Context ctx, GoogleApiClient client) {
        this.mContext = ctx;
        this.mGoogleApiClient = client;
    }

    @Override
    public void onLocationChanged(Location location) {
        Intent localIntent = new Intent(Constants.tracker.COLLECT_LOCATIONS_ACTION);
        localIntent.putExtra(Constants.tracker.LOCATION_EXTRA, location);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(localIntent);
    }

    /* Module Interface
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private boolean isTracking = false;

    @Override
    public void startTracking() {
        if (!isTracking) {

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
            isTracking = false;

        }
    }

    @Override
    public boolean isTracking() {
        return isTracking;
    }

    /* Tracking Configuration Profile
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private long mInterval = 10000,
            mFastInterval = 5000;

    private int mPriority = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private float mMinimumDisplacement = 2f; //meters

    @Deprecated
    private float mMinimumAccuracy;

    @Deprecated
    private float mMaximumSpeed;

    public void setInterval(long mInterval) {
        this.mInterval = mInterval;
    }

    public void setFastInterval(long mFastInterval) {
        this.mFastInterval = mFastInterval;
    }

    public void setPriority(int mPriority) {
        this.mPriority = mPriority;
    }

    public void setMinimumDisplacement(float mMinimumDisplacement) {
        this.mMinimumDisplacement = mMinimumDisplacement;
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(mInterval);
        mLocationRequest.setFastestInterval(mFastInterval);
        mLocationRequest.setPriority(mPriority);
        mLocationRequest.setSmallestDisplacement(mMinimumDisplacement);

        return mLocationRequest;
    }

    @Deprecated
    public void setMinimumAccuracy(float minimumAccuracy) {
        this.mMinimumAccuracy = minimumAccuracy;
    }

    @Deprecated
    public void setMaximumSpeed(float mMaximumSpeed) {
        this.mMaximumSpeed = mMaximumSpeed;
    }

    @Deprecated
    public void activateRemoveOutliers(boolean activeOutlierRemoval) {
        return;
    }
}
