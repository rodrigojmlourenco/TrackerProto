package org.trace.tracking.store.auth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.trace.tracking.TrackingConstants;

public class LoginBroadcastListener extends BroadcastReceiver {

    Class onSuccess, onFail;

    public LoginBroadcastListener(Class OnSuccessActivity, Class OnFailActivity){
        onSuccess = OnSuccessActivity;
        onFail = OnFailActivity;
    }

    public static IntentFilter getLoginIntentFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(TrackingConstants.store.LOGIN_ACTION);
        return filter;
    }



    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.hasExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA)
                && intent.getBooleanExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA, false)) {

            Intent mainActivity = new Intent(context, onSuccess);
            context.startActivity(mainActivity);
        }else{
            Intent failedLogin = new Intent(context, onFail);
            failedLogin.putExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA, intent.getStringExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA));
            context.startActivity(failedLogin);
        }
    }
}
