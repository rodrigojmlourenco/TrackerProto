package org.trace.trackerproto.tracking.modules.location;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.trace.trackerproto.tracking.CollectorManager;
import org.trace.trackerproto.tracking.exceptions.UnableToParseTraceException;
import org.trace.trackerproto.tracking.filter.HeuristicBasedFilter;
import org.trace.trackerproto.tracking.modules.ModuleInterface;
import org.trace.trackerproto.tracking.utils.LocationUtils;

import java.util.LinkedList;

import static org.trace.trackerproto.tracking.filter.HeuristicBasedFilter.*;


/**
 * Created by Rodrigo Lourenço on 12/02/2016.
 */
public class FusedLocationModule implements LocationListener, ModuleInterface{

    protected final static String LOG_TAG = "FusedLocation";

    private final Context context;
    private final GoogleApiClient mGoogleApiClient;
    private final LinkedList<Location> mLocations;

    private boolean isTracking = false;

    private long mInterval      = 10000,
                 mFastInterval  = 5000;

    private int mPriority = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private float mMinimumDisplacement = 5f; //meters

    private HeuristicBasedFilter outlierFilter;

    public FusedLocationModule(Context ctx, GoogleApiClient client){
        this.context = ctx;
        this.mGoogleApiClient = client;
        this.mLocations = new LinkedList<>();

        this.outlierFilter = new HeuristicBasedFilter();
        outlierFilter.addNewHeuristic(new HeuristicBasedFilter.AccuracyBasedHeuristicRule(30));
        outlierFilter.addNewHeuristic(new SatelliteBasedHeuristicRule(4));
        outlierFilter.addNewHeuristic(new SpeedBasedHeuristicRule(16.67f));
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

    public boolean isTracking() {
        return isTracking;
    }

    private LocationRequest createLocationRequest(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(mInterval);
        mLocationRequest.setFastestInterval(mFastInterval);
        mLocationRequest.setPriority(mPriority);
        mLocationRequest.setSmallestDisplacement(mMinimumDisplacement);

        return mLocationRequest;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("FL", location.toString());

        if(outlierFilter.isValidLocation(location)) {
            this.mLocations.add(location);
            ((CollectorManager) context).storeLocation(location);
        }else
            Log.e(LOG_TAG, "Location was discarded as an outlier.");
    }

    public void startLocationUpdates(){

        if(!isTracking) {

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

    public Location getLastLocation(){
        return this.mLocations.getLast();
    }

    @Override
    public void startTracking(long millis) {

        //TODO: update the interval values

        startLocationUpdates();


    }

    @Override
    public void stopTracking() {
        stopLocationUpdates();
        dump();
    }

    @Override
    public void dump() {
        try {
            Log.i(LOG_TAG, LocationUtils.generateGPXTrack(mLocations));
        } catch (UnableToParseTraceException e) {
            e.printStackTrace();
        }
    }
}
