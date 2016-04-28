package org.trace.modalityrecognizer;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class ModalityTrackerService extends IntentService {

    public static final String PACKAGE_NAME = "com.google.android.gms.location.activityrecognition";
    public static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";
    public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public ModalityTrackerService() {
        super(ModalityRecognizer.TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        Intent localIntent = new Intent(BROADCAST_ACTION);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        /*
        // Log each activity.
        Log.i(ModalityRecognizer.TAG, "activities detected");
        for (DetectedActivity da: detectedActivities) {
            Log.i(ModalityRecognizer.TAG, ModalityConstants.getActivityString(
                            getApplicationContext(),
                            da.getType()) + " " + da.getConfidence() + "%"
            );
        }
        */

        // Broadcast the list of detected activities.
        localIntent.putExtra(ACTIVITY_EXTRA, detectedActivities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}
