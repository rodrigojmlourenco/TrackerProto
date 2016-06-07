package org.trace.storeclient.auth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.trace.storeclient.R;
import org.trace.storeclient.StoreClientConstants;
import org.trace.storeclient.TraceAuthenticationManager;

public class AuthenticationRenewalListener extends BroadcastReceiver {

    private Context mContext;
    private TraceAuthenticationManager mAuthManager;

    public AuthenticationRenewalListener(Context context, TraceAuthenticationManager manager){
        mContext = context;
        mAuthManager = manager;
    }

    public static IntentFilter getAuthenticationRenewalFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(StoreClientConstants.TOKEN_EXPIRED_ACTION);
        return filter;
    }


    public static Intent getFailedRemoteOperationIntent(JsonObject operation){
        return new Intent(StoreClientConstants.TOKEN_EXPIRED_ACTION)
                .putExtra(StoreClientConstants.FAILED_OPERATION, operation.toString());
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        mAuthManager.login();
        JsonParser parser = new JsonParser();

        if(intent.hasExtra(StoreClientConstants.FAILED_OPERATION)){
            JsonObject failedOperation = (JsonObject)parser.parse(intent.getStringExtra(StoreClientConstants.FAILED_OPERATION));

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
