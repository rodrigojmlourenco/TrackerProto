package org.trace.tracker.tracking.modules.activity;

/**
 * Created by Rodrigo Lourenço on 28/03/2016.
 */

import com.google.android.gms.location.DetectedActivity;

/**
 * Returns a human readable String corresponding to a detected activity type.
 */
public class ActivityConstants {
    public static int getActivityIndex(DetectedActivity activity){

        int activityId;

        switch (activity.getType()){
            case DetectedActivity.UNKNOWN:
                activityId = -1;
                break;
            case DetectedActivity.STILL:
                activityId = 0;
                break;
            case DetectedActivity.ON_FOOT: //TODO: this can also be an instance of running?
            case DetectedActivity.WALKING:
                activityId = 1;
                break;
            case DetectedActivity.RUNNING:
                activityId = 2;
                break;
            case DetectedActivity.ON_BICYCLE:
                activityId = 3;
                break;
            case DetectedActivity.IN_VEHICLE:
                activityId = 4;
                break;
            case DetectedActivity.TILTING: //TODO: for testing purposes -- please remove
                activityId = 6;
                break;
            default /*Other*/:
                activityId = 5;
        }

        return activityId;
    }

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
     *
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
