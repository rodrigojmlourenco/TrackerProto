package org.trace.tracking;

import android.Manifest;

public final class Constants {

    public final static String OPERATION_KEY = "action";
    public final static String USERNAME_KEY = "username";
    public final static String PASSWORD_KEY = "password";


    //TRACEStore Extras
    public static final String TRACK_EXTRA = "org.trace.store.extras.TRACK";
    //Tracker
    public static final String COLLECT_ACTION = "org.trace.tracker.COLLECT";
    public static final String ACTIVITY_EXTRA = "org.trace.tracker.ACTIVITY_EXTRA";
    public static final String LOCATION_EXTRA = "org.trace.tracker.LOCATION_EXTRA";


    //IntentService and BroadcastReceiver
    public static final String BROADCAST_ACTION = "org.trace.intent.BROADCAST";
    public static final String FIRST_TIME_BROADCAST = "org.trace.intent.FIRST_TIME";

    public static final String BROADCAST_LOCATION_ACTION = "org.trace.intent.BROADCAST_LOCATION";
    public static final String BROADCAST_LOCATION_EXTRA = "org.trace.intent.BROADCAST_LOCATION_EXTRA";

    //Login Activity
    public static final String LOGIN_ACTION = "org.trace.intent.LOGIN";
    public static final String SUCCESS_LOGIN_KEY = "org.trace.intent.SUCCESS_LOGIN";
    public static final String LOGIN_ERROR_MSG_KEY = "org.trace.intent.LOGIN_ERROR";

    //Map Activity
    public static final String GPX_FILE_KEY = "org.trace.intent.GPX_FILE";
    public static final String TRACK_KEY = "org.trace.intent.TRACK";

    public interface TRACEService {
        String FAILED_LOGIN_KEY = "-1";
    }

    public interface permissions {
        int TRACKING         = 1;
        int EXTERNAL_STORAGE = 2;
        String[] TRACKING_PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        String[] EXTERNAL_STORAGE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }


    public static final String AUTH_TOKEN = "auth_token";

    public interface store {
        String  LATITUDE    = "latitude",
                LONGITUDE   = "longitude",
                TIMESTAMP   = "timestamp",
                ATTRIBUTES  = "attributes";

        interface attributes {
            String  ACTIVITY= "activity",
                    BEARING = "bearing",
                    ALTITUDE= "altitude",
                    SPEED   = "speed",
                    ACCURACY= "accuracy",
                    PROVIDER= "provider",
                    ELAPSED_NANOS = "elapsedNanos";
        }
    }

    public interface http {


        String CONTENT_TYPE = "Content-Type";
        String CONTENT_LENGTH = "Content-Length";
        String CONTENT_LANGUAGE = "Content-Language";
        String AUTHORIZATION = "Authorization";

    }
}
