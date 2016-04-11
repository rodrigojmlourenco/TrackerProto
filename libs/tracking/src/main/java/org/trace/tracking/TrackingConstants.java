package org.trace.tracking;

import android.Manifest;

//TODO: precisa de refactorização, algumas incoerências nos nomes.
public final class TrackingConstants {

    //IntentService and BroadcastReceiver
    //TODO: isto é mesmo necessário?
    public static final String BROADCAST_ACTION = "org.trace.intent.BROADCAST";
    public static final String FIRST_TIME_BROADCAST = "org.trace.intent.FIRST_TIME";


    public interface permissions {
        //TODO: move this to the app
        int TRACKING            = 1;
        int EXTERNAL_STORAGE    = 2;
        int DRAW_MAPS           = 3;
        int FOCUS_ON_MAP        = 4;

        String[] TRACKING_PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        String[] EXTERNAL_STORAGE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    public interface store {

        //Login
        String LOGIN_ACTION = "org.trace.intent.LOGIN";
        String SUCCESS_LOGIN_EXTRA = "org.trace.intent.SUCCESS_LOGIN";
        String LOGIN_ERROR_MSG_EXTRA = "org.trace.intent.LOGIN_ERROR";

        //AuthTokenExpired
        String TOKEN_EXPIRED_ACTION  = "org.trace.intent.EXPIRED_TOKEN";
        String FAILED_OPERATION_KEY = "org.trace.intent.FAILED_OPERATION";
        String FAILED_OPERATION_EXTRAS = "org.trace.intent.FAILED_OPERATION";


        String AUTH_TOKEN_EXTRA = "auth_token";

        String TRACK_EXTRA  = "org.trace.store.extras.TRACK";
        String SESSION_EXTRA= "org.trace.store.extras.SESSION";

        String OPERATION_KEY    = "action";
        String USERNAME_KEY     = "username";
        String PASSWORD_KEY     = "password";
        String ID_TOKEN_KEY     = "id_token";




    }

    public interface location {
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
