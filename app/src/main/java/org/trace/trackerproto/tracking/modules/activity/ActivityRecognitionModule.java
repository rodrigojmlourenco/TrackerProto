package org.trace.trackerproto.tracking.modules.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;

import org.trace.trackerproto.tracking.exceptions.GoogleApiClientDisconnectedException;
import org.trace.trackerproto.tracking.modules.ModuleInterface;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Rodrigo Lourenço on 17/02/2016.
 */
public class ActivityRecognitionModule implements ModuleInterface, ResultCallback<Status> {

    protected final static String LOG_TAG = "ActivityRecogModule";

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private BroadcastReceiver mActivityReceiver;
    private PendingIntent mActivityRecogIntent = null;

    private boolean isTracking = false;

    private Queue<SimpleDetectedActivity> activities;

    public ActivityRecognitionModule(Context ctx, GoogleApiClient googleApiClient){

        if(!googleApiClient.isConnected())
            throw new GoogleApiClientDisconnectedException();

        this.mContext = ctx;
        this.mGoogleApiClient = googleApiClient;

        this.activities = new LinkedList<>();

        /*
        this.mActivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Add current time
                Calendar rightNow = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a");
                String strDate = sdf.format(rightNow.getTime());;

                String activity = intent.getStringExtra("activity");
                int confidence  = intent.getExtras().getInt("confidence");

                String v =  strDate + " " +
                        activity + " " +
                        "Confidence : " + confidence + "\n";

                Log.i(LOG_TAG, v);

                activities.add(new SimpleDetectedActivity(activity, confidence));
            }
        };

        /*
        IntentFilter filter = new IntentFilter();
        filter.addAction(ActivityConstants.BROADCAST_ACTION);
        mContext.registerReceiver(mActivityReceiver, filter);
        */
    }

    @Override
    public void startTracking(long interval){

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

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(mContext, ActivityRecognitionHandler.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            Log.i(LOG_TAG, "Success adding or removing activity detection: " + status.getStatusMessage());
        } else {
            Log.e(LOG_TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }


}
