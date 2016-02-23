package org.trace.trackerproto;

/**
 * Created by Rodrigo Lourenço on 15/02/2016.
 */
public final class Constants {

    public final static int TRACE_LOC_PERMISSIONS = 01;

    public final static String OPERATION_KEY = "action";
    public final static String USERNAME_KEY = "username";
    public final static String PASSWORD_KEY = "password";


    public final static String LAST_LOCATION_KEY = "LAST_LOCATION";
    public final static String REQUEST_LOCATION_UPDATES = "IS_TRACKING";

    public final static String TRACKER_SERVICE_BOUND_KEY = "BOUND_TRACKER_SERVICE";
    public final static String TRACKER_SERVICE_TRACKING_KEY = "TRACKING_TRACKER_SERVICE";


    //IntentService and BroadcastReceiver
    public static final String BROADCAST_ACTION = "org.trace.intent.BROADCAST";
    public static final String FIRST_TIME_BROADCAST = "org.trace.intent.FIRST_TIME";

    //Login Activity
    public static final String LOGIN_ACTION = "org.trace.intent.LOGIN";
    public static final String SUCCESS_LOGIN_KEY = "org.trace.intent.SUCCESS_LOGIN";
    public static final String LOGIN_ERROR_MSG_KEY = "org.trace.intent.LOGIN_ERROR";

    //Map Activity
    public static final String GPX_FILE_KEY = "org.trace.intent.GPX_FILE";
    public static final String TRACK_KEY = "org.trace.intent.TRACK";

}
