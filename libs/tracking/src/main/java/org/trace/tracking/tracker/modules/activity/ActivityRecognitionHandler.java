package org.trace.tracking.tracker.modules.activity;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

/**
 * Created by Rodrigo Lourenço on 18/02/2016.
 *
 * @see {https://github.com/googlesamples/android-play-location/tree/master/ActivityRecognition}
 */
public class ActivityRecognitionHandler extends IntentService {

    protected static final String TAG = "ActivityRecogHandler";

    public ActivityRecognitionHandler() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);


        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        // Log each activity.
        /*Log.i(TAG, "activities detected");
        for (DetectedActivity da: detectedActivities) {

            Log.i(TAG, ActivityConstants.getActivityString(
                            getApplicationContext(),
                            da.getType()) + " " + da.getConfidence() + "%"
            );
        }*/

        // Broadcast the list of detected activities.
        Intent localIntent = new Intent(ActivityConstants.COLLECT_ACTION);
        localIntent.putExtra(ActivityConstants.ACTIVITY_EXTRA, detectedActivities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}