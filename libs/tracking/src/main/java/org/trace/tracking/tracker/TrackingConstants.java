package org.trace.tracking.tracker;


import com.google.android.gms.location.DetectedActivity;

public interface TrackingConstants {

    interface ActivityRecognition {

        String PACKAGE_NAME = "com.google.android.gms.location.activityrecognition";

        String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";

        String COLLECT_ACTION = PACKAGE_NAME + ".COLLECT";


    }
}
