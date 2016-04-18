package org.trace.storeclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * <emph>Note:</emph> Soon to be deprecated. Avoid its use.
 */
public class TRACEStoreReceiver extends BroadcastReceiver {

    public TRACEStoreReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {


        if(intent.hasExtra(StoreClientConstants.FIRST_TIME_BROADCAST)
                && intent.getBooleanExtra(StoreClientConstants.FIRST_TIME_BROADCAST, true)){

            /**
             * TODO: send and action that means that a login activity should be presented.
            context.startActivity(
                    new Intent(context, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
             **/

            return;
        }

    }
}
