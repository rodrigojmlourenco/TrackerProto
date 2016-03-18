package org.trace.tracking.modules.activity;

import android.content.Context;
import android.content.res.Resources;

import com.google.android.gms.location.DetectedActivity;


/**
 * Created by Rodrigo Lourenço on 18/02/2016.
 *
 * @see {https://github.com/googlesamples/android-play-location/tree/master/ActivityRecognition}
 */
public class ActivityConstants {

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


    public static final String PACKAGE_NAME = "com.google.android.gms.location.activityrecognition";

    public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";

    public static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";

    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES";

    public static final String ACTIVITY_UPDATES_REQUESTED_KEY = PACKAGE_NAME +
            ".ACTIVITY_UPDATES_REQUESTED";

    public static final String DETECTED_ACTIVITIES = PACKAGE_NAME + ".DETECTED_ACTIVITIES";

    /**
     * The desired time between activity detections. Larger values result in fewer activity
     * detections while improving battery life. A value of 0 results in activity detections at the
     * fastest possible rate. Getting frequent updates negatively impact battery life and a real
     * app may prefer to request less frequent updates.
     */
    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 0;

    /**
     * List of DetectedActivity types that we monitor in this sample.
     */
    protected static final int[] MONITORED_ACTIVITIES = {
            DetectedActivity.STILL,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.TILTING,
            DetectedActivity.UNKNOWN
    };

    public final static String COLLECT_ACTION = "";
}
