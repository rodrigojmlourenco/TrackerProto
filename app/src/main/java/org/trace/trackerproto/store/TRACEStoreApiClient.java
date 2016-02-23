package org.trace.trackerproto.store;

import android.content.Context;
import android.content.Intent;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.store.api.TRACEStoreOperations;
import org.trace.trackerproto.store.auth.AuthenticationManager;

/**
 * Created by Rodrigo Lourenço on 19/02/2016.
 */
public class TRACEStoreApiClient {

    private static String sessionId;

    protected static void setSessionId(String session){
        sessionId = session;
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

    public static void requestInitiateSession(Context context){
        Intent mI = new Intent(context, TRACEStore.class);
        mI.putExtra(Constants.OPERATION_KEY, TRACEStoreOperations.initiateSession.toString());
        context.startService(mI);
    }

    public static boolean isFirstTime(Context context){
        return AuthenticationManager.isFirstTime(context);
    }
}
