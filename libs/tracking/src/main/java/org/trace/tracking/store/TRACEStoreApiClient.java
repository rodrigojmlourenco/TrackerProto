package org.trace.tracking.store;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import org.trace.tracking.Constants;
import org.trace.tracking.store.api.TRACEStoreOperations;
import org.trace.tracking.store.auth.AuthenticationManager;
import org.trace.tracking.storage.data.Track;

public class TRACEStoreApiClient {

    private static final String LOG_TAG = "TRACEStore";

    private static String sessionId;
    private static boolean isValid;

    protected static void setSessionId(String session){
        sessionId = session;
        isValid = true;
    }

    protected static void setSessionId(String session, boolean valid){
        sessionId = session;
        isValid = valid;
    }


    public static  boolean isValidSession(){
        return isValid;
    }

    public static String getSessionId(){
        return sessionId;
    }

    public static void requestLogin(Context context, String username, String password){
        Intent mI = new Intent(context, TRACEStore.class);
        mI.putExtra(Constants.OPERATION_KEY, TRACEStoreOperations.login.toString());
        mI.putExtra(Constants.USERNAME_KEY, username);
        mI.putExtra(Constants.PASSWORD_KEY, password);
        context.startService(mI);
    }

    public static void requestLogout(Context context){
        Intent mI = new Intent(context, TRACEStore.class);
        mI.putExtra(Constants.OPERATION_KEY, TRACEStoreOperations.logout.toString());
        context.startService(mI);
    }

    public static void requestInitiateSession(Context context){
        Intent mI = new Intent(context, TRACEStore.class);
        mI.putExtra(Constants.OPERATION_KEY, TRACEStoreOperations.initiateSession.toString());
        context.startService(mI);
    }

    public static boolean isFirstTime(Context context){
        return AuthenticationManager.isFirstTime(context);
    }

    public static void uploadTrackingInfo(Location location, DetectedActivity activity){
        Log.d(LOG_TAG, "[TODO] uploading { location:"+location+", activity: "+activity+"}");
    }

    public static void uploadWholeTrack(Context context, Track track){
        Intent mI = new Intent(context, TRACEStore.class);
        mI.putExtra(Constants.OPERATION_KEY, TRACEStoreOperations.submitTrack.toString());
        mI.putExtra(Constants.TRACK_EXTRA, track);
        context.startService(mI);
    }
}
