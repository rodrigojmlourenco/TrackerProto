package org.trace.tracking.store.auth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.trace.tracking.R;
import org.trace.tracking.TrackingConstants;
import org.trace.tracking.store.TRACEStore;
import org.trace.tracking.store.TraceAuthenticationManager;
import org.trace.tracking.store.exceptions.UserIsNotLoggedException;
import org.trace.tracking.store.remote.HttpClient;
import org.trace.tracking.tracker.storage.data.Track;

import java.util.LinkedList;
import java.util.Queue;


public class AuthenticationRenewalListener extends BroadcastReceiver {

    private Context mContext;
    private TraceAuthenticationManager mAuthManager;

    public AuthenticationRenewalListener(Context context, TraceAuthenticationManager manager){
        mContext = context;
        mAuthManager = manager;
    }

    public static IntentFilter getAuthenticationRenewalFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(TrackingConstants.store.TOKEN_EXPIRED_ACTION);
        return filter;
    }


    public static Intent getFailedRemoteOperationIntent(JsonObject operation){
        return new Intent(TrackingConstants.store.TOKEN_EXPIRED_ACTION)
                .putExtra(TrackingConstants.store.FAILED_OPERATION_KEY, operation.toString());
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        mAuthManager.login();
        JsonParser parser = new JsonParser();

        if(intent.hasExtra(TrackingConstants.store.FAILED_OPERATION_KEY)){
            JsonObject failedOperation = (JsonObject)parser.parse(intent.getStringExtra(TrackingConstants.store.FAILED_OPERATION_KEY));

            if(!avoidOperation(failedOperation.get("endpoint").getAsString())){
                Toast.makeText(mContext, mContext.getString(R.string.failed_remote_operation), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean avoidOperation(String urlEndpoint){

        return  urlEndpoint.equals("/auth/session/open");
    }
    private void parseOperation(String urlEndpoint){
        switch (urlEndpoint){
            case "/auth/session/open":
                break;
            default:
                return;
        }
    }
}
