package org.trace.tracker;


public final class TrackingConstants {

    //IntentService and BroadcastReceiver
    public static final String BROADCAST_ACTION = "org.trace.intent.BROADCAST";
    public static final String FIRST_TIME_BROADCAST = "org.trace.intent.FIRST_TIME";


    public interface tracker {

        String SESSION_EXTRA = "org.trace.tracker.SESSION_ID";
        String VALID_SESSION_EXTRA = "org.trace.tracker.VALID_SESSION";

        //getLastLocation
        String COLLECT_LOCATIONS_ACTION = "org.trace.tracker.COLLECT";
        String BROADCAST_LOCATION_ACTION = "org.trace.tracking.BROADCAST_LOCATION";

        String BROADCAST_LOCATION_EXTRA = "org.trace.tracking.BROADCAST_LOCATION_EXTRA";
        String ACTIVITY_EXTRA = "org.trace.tracker.ACTIVITY_EXTRA";
        String LOCATION_EXTRA = "org.trace.tracker.LOCATION_EXTRA";
    }
}
