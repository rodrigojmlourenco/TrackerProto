package org.trace.tracking.tracker.modules.activity;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.trace.tracking.tracker.TrackingConstants;

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

        // Broadcast the list of detected activities.
        Intent localIntent = new Intent(TrackingConstants.ActivityRecognition.COLLECT_ACTION);
        localIntent.putExtra(TrackingConstants.ActivityRecognition.ACTIVITY_EXTRA, detectedActivities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}
