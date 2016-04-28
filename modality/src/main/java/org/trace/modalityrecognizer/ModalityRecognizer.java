package org.trace.modalityrecognizer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import org.trace.modalityrecognizer.data.Modality;
import org.trace.modalityrecognizer.data.TraceModality;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * The ModalityRecognizer was designed to enable and ease the tracking of the user's modality.
 * <br><br>
 * The component was designed as a singleton, therefore, at any time only one instance of the
 * component may be running.
 */
public class ModalityRecognizer implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    public final static String TAG = "ModalityRecognizer";

    private Context mContext;
    protected GoogleApiClient mGoogleApiClient;
    private ActivityDetectionBroadcastReceiver mReceiver;

    private long interval;
    private int minConfidence;
    private final TraceModality mUnknownModality = new TraceModality(Modality.Unknown, 100, System.currentTimeMillis());

    private final Object modalityListLock = new Object();
    private LinkedList<TraceModality> mTrackedModalities;

    private ModalityRecognizer(Context context){
        this.mContext = context;
        buildGoogleApiClient(context);

        mReceiver = new ActivityDetectionBroadcastReceiver();


    }

    private static ModalityRecognizer TRACKER = null;

    /**
     * Allows access to the ModalityRecognizer instance.
     * @param context The application or main activity context.
     * @return The instance of the ModalityRecognizer
     */
    public static ModalityRecognizer getInstance(Context context){
        synchronized (ModalityRecognizer.class){
            if(TRACKER == null)
                TRACKER = new ModalityRecognizer(context);
        }

        return TRACKER;
    }


    /* Activity Tracking API
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    /**
     * Initiates modality tracking. The ModalityRecognizer will attempt to recognize the current
     * modality according to the specified interval, while discarding modalities with low confidence.
     *
     * @param interval The updating interval.
     * @param minConfidence The minimum accepted confidence.
     */
    public void startModalityTracking(long interval, int minConfidence){

        mGoogleApiClient.connect();

        this.interval = interval;
        this.minConfidence = minConfidence;

        synchronized (modalityListLock) {
            mTrackedModalities = new LinkedList<>();
        }

        LocalBroadcastManager
                .getInstance(mContext)
                .registerReceiver(mReceiver, new IntentFilter(ModalityTrackerService.BROADCAST_ACTION));
    }

    /**
     * Terminates the modality tracking.
     */
    public void stopModalityTracking(){

        mGoogleApiClient.disconnect();

        synchronized (modalityListLock) {
            mTrackedModalities.clear();
        }
        
        LocalBroadcastManager
                .getInstance(mContext)
                .unregisterReceiver(mReceiver);
    }

    /**
     * Fetches the most recent updated modality.
     * @return The most recent TraceModality
     *
     * @see TraceModality
     */
    public TraceModality getCurrentModality(){
        TraceModality current;

        synchronized (modalityListLock) {
            current = mTrackedModalities.getLast();
        }

        if(current == null)
            return mUnknownModality;
        else
            return current;
    }

    /* Support Methods
    /* Support Methods
    /* Support Methods
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private PendingIntent getActivityDetectionPendingIntent(){
        Intent intent = new Intent(mContext, ModalityTrackerService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * ActivityRecognition API.
     */
    protected synchronized void buildGoogleApiClient(Context context) {

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();
    }



    /* GoogleApiClient.ConnectionCallbacks & GoogleApiClient.OnConnectionFailedListener
    /* GoogleApiClient.ConnectionCallbacks & GoogleApiClient.OnConnectionFailedListener
    /* GoogleApiClient.ConnectionCallbacks & GoogleApiClient.OnConnectionFailedListener
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                interval,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection failed : "+connectionResult.getErrorMessage());

        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                mGoogleApiClient,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);

    }


    /* Activity Recognition ResultCallback
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    @Override
    public void onResult(Status status) {

        if (status.isSuccess()) {

        } else {
            Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }

    }

    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {

        private TraceModality mCurrentModality = null;

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> updatedActivities =
                    intent.getParcelableArrayListExtra(ModalityTrackerService.ACTIVITY_EXTRA);

            DetectedActivity activity = updatedActivities.get(0);
            TraceModality modality = new TraceModality(activity.getType(), activity.getConfidence(), System.currentTimeMillis());

            if(activity.getConfidence() >= minConfidence) {

                Log.i(TAG, modality.toString());

                //Update the current modality
                if(mCurrentModality == null || !mCurrentModality.equals(modality))
                    mCurrentModality = modality;


                synchronized (modalityListLock) {
                    mTrackedModalities.add(modality);
                }

            }else {
                Log.e(TAG, "Ignoring the new modality due to low confidence: " + modality.toString());
            }

        }
    }
}
