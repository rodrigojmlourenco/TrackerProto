package org.trace.storeclient.auth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.trace.storeclient.StoreClientConstants;


public class LoginBroadcastListener extends BroadcastReceiver {

    Class onSuccess, onFail;

    public LoginBroadcastListener(Class OnSuccessActivity, Class OnFailActivity){
        onSuccess = OnSuccessActivity;
        onFail = OnFailActivity;
    }

    public static IntentFilter getLoginIntentFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(StoreClientConstants.LOGIN_ACTION);
        return filter;
    }



    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.hasExtra(StoreClientConstants.SUCCESS_LOGIN_EXTRA)
                && intent.getBooleanExtra(StoreClientConstants.SUCCESS_LOGIN_EXTRA, false)) {

            Intent mainActivity = new Intent(context, onSuccess);
            context.startActivity(mainActivity);
        }else{
            Intent failedLogin = new Intent(context, onFail);
            failedLogin.putExtra(StoreClientConstants.LOGIN_ERROR_MSG_EXTRA, intent.getStringExtra(StoreClientConstants.LOGIN_ERROR_MSG_EXTRA));
            context.startActivity(failedLogin);
        }
    }
}
