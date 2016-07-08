package org.trace.tracker.tracking.modules.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import org.trace.tracker.exceptions.GoogleApiClientDisconnectedException;
import org.trace.tracker.tracking.modules.ModuleInterface;

import java.util.LinkedList;
import java.util.Queue;

public class ActivityRecognitionModule implements ModuleInterface, ResultCallback<Status> {

    protected final static String LOG_TAG = "ActivityRecogModule";

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private BroadcastReceiver mActivityReceiver;
    private PendingIntent mActivityRecogIntent = null;

    private boolean isTracking = false;

    //Tracking Parameters
    private long interval = 1000;
    private int minimumConfidence = 75;

    private Queue<SimpleDetectedActivity> activities;

    public ActivityRecognitionModule(Context ctx, GoogleApiClient googleApiClient){

        if(!googleApiClient.isConnected())
            throw new GoogleApiClientDisconnectedException();

        this.mContext = ctx;
        this.mGoogleApiClient = googleApiClient;

        this.activities = new LinkedList<>();
    }

    @Override
    public void startTracking(){

        if(!mGoogleApiClient.isConnected()) {
            Log.e(LOG_TAG, "ERROR, the google api client is not connected.");
            return;
        }

        Intent i = new Intent(mContext, ActivityRecognitionHandler.class);
        mActivityRecogIntent = PendingIntent.getService(mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        ActivityRecognition.ActivityRecognitionApi
                .requestActivityUpdates(
                        mGoogleApiClient,
                        interval,
                        getActivityDetectionPendingIntent())
                .setResultCallback(this);

        isTracking = true;
    }

    @Override
    public void stopTracking(){

        if(!isTracking) return;

        ActivityRecognition.ActivityRecognitionApi
                .removeActivityUpdates(
                        mGoogleApiClient,
                        getActivityDetectionPendingIntent())
                .setResultCallback(this);

        isTracking = false;

    }

    @Override
    public boolean isTracking() {
        return isTracking;
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(mContext, ActivityRecognitionHandler.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public void onResult(@NonNull Status status) {
        /*
        if (status.isSuccess()) {
            Log.i(LOG_TAG, "Success adding or removing activity detection: " + status.getStatusMessage());
        } else {
            Log.e(LOG_TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }
        */
    }


    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setMinimumConfidence(int minimumConfidence) {
        this.minimumConfidence = minimumConfidence;
    }

    public int getMinimumConfidence() {
        return minimumConfidence;
    }

    /**
     * Returns a human readable String corresponding to a detected activity type.
     */
    public static String getActivityString(int detectedActivityType) {

        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return "Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "Cycling";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.UNKNOWN:
                return "Unknown";
            case DetectedActivity.WALKING:
                return "Walking";
            default:
                return "Unknown";
        }
    }
}
